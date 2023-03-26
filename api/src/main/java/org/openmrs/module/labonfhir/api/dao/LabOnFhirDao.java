package org.openmrs.module.labonfhir.api.dao;

import org.hibernate.criterion.Restrictions;
import org.hl7.fhir.r4.model.Task;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.fhir2.api.FhirTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository("labonfhir.LabOnFhirDao")
public class LabOnFhirDao {

    @Autowired
    DbSessionFactory sessionFactory;

    @Autowired
    FhirTaskService taskService;

    private DbSession getSession() {
        return sessionFactory.getCurrentSession();
    }

    @SuppressWarnings("unchecked")
    public List<Encounter> getAllEncounterNotPushed(EncounterType encounterType, Boolean includeFalse) {
        List<Encounter> encounters = getSession().createCriteria(Encounter.class)
                .add(Restrictions.eq("encounterType", encounterType))
                .add(Restrictions.isNotEmpty("orders"))
                .add(Restrictions.eq("voided", includeFalse)).list();

        List<Encounter> encounterList = new ArrayList<>();

        if (encounters != null) {
            for (Encounter e : encounters) {
                Task task = taskService.get(e.getUuid());
                if (task == null) {
                    encounterList.add(e);
                }
            }
        }

        return encounterList;
    }
}
