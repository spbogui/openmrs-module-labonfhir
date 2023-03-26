package org.openmrs.module.labonfhir.api.impl;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.labonfhir.api.LabOnFhirService;
import org.openmrs.module.labonfhir.api.dao.LabOnFhirDao;

import java.util.List;

public class LabOnFhirServiceImpl  extends BaseOpenmrsService implements LabOnFhirService {

    private LabOnFhirDao dao;

    public void setDao(LabOnFhirDao dao) {
        this.dao = dao;
    }

    @Override
    public List<Encounter> getAllEncounterNotPushed(EncounterType encounterType, Boolean includeFalse) {
        return dao.getAllEncounterNotPushed(encounterType, includeFalse);
    }
}
