package org.openmrs.module.labonfhir.api.scheduler;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.TaskStatus;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.FhirTaskService;
import org.openmrs.module.labonfhir.LabOnFhirConfig;
import org.openmrs.module.labonfhir.api.LabOnFhirService;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@Component
@Setter(AccessLevel.PACKAGE)
public class FetchHandleTask extends AbstractTask implements ApplicationContextAware {

	private static Log log = LogFactory.getLog(FetchHandleTask.class);

	private static ApplicationContext applicationContext;

	@Autowired
	private LabOnFhirConfig config;

	@Autowired
	private IGenericClient client;

	@Autowired
	private FhirTaskService taskService;

	@Autowired
	private EncounterService encounterService;

	@Autowired
	private LabOnFhirService labOnFhirService;

	@Autowired
	@Qualifier("sessionFactory")
	SessionFactory sessionFactory;

	@Autowired
	@Qualifier("fhirR4")
	private FhirContext ctx;

	@Override
	public void execute() {

		try {
			applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
		} catch (Exception e) {
			// return;
		}

		if (!config.isOpenElisEnabled()) {
			return;
		}

		try {
			EncounterType encounterType = encounterService.getEncounterTypeByUuid("DEMANDEEXAMENEEEEEEEEEEEEEEEEEEEEEEEEE"); // add encounter type

			if (encounterType != null) {
				List<Encounter> encounters = labOnFhirService.getAllEncounterNotPushed(encounterType, false);
				if (encounters != null) {
					for (Encounter encounter : encounters) {
						List<IBaseResource> baseResources = taskService.searchForTasks(
								null,
								null,
								null,
								new TokenAndListParam().addAnd(new TokenParam(encounter.getUuid())),
								null,
								null,
								null).getAllResources();

						for (IBaseResource baseResource : baseResources) {
							Task task = (Task) baseResource;

							if (task != null) {
								if (config.getActivateFhirPush()) {
									Bundle labBundle = createLabBundle(task);
									client.transaction().withBundle(labBundle).execute();
									log.debug(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(labBundle));
								}
							}
						}

					}
				}
			}



			// Get List of Tasks that belong to this instance and update them
		} catch (Exception e) {
			log.error("ERROR executing FetchTaskUpdates : " + e.toString() + getStackTrace(e));
		}

		super.startExecuting();
	}

	@Override
	public void shutdown() {
		log.debug("shutting down FetchTaskUpdates Task");

		this.stopExecuting();
	}

	private Bundle createLabBundle(Task task) {
		return getBundle(task, taskService);
	}

	public static Bundle getBundle(Task task, FhirTaskService taskService) {
		TokenAndListParam uuid = new TokenAndListParam().addAnd(new TokenParam(task.getIdElement().getIdPart()));
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("Task:patient"));
		includes.add(new Include("Task:owner"));
		includes.add(new Include("Task:encounter"));
		includes.add(new Include("Task:input"));
		includes.add(new Include("Task:based-on"));

		IBundleProvider labBundle = taskService.searchForTasks(null, null, null, uuid, null, null, includes);
		labBundle.getAllResources();

		Bundle transactionBundle = new Bundle();
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		List<IBaseResource> labResources = labBundle.getAllResources();
		for (IBaseResource r : labResources) {
			Resource resource = (Resource) r;
			Bundle.BundleEntryComponent component = transactionBundle.addEntry();
			component.setResource(resource);
			component.getRequest().setUrl(resource.fhirType() + "/" + resource.getIdElement().getIdPart())
					.setMethod(Bundle.HTTPVerb.PUT);
		}
		return transactionBundle;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
