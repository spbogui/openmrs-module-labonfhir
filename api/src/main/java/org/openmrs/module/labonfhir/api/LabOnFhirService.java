package org.openmrs.module.labonfhir.api;


import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.api.OpenmrsService;

import java.util.List;

public interface LabOnFhirService extends OpenmrsService {
    List<Encounter> getAllEncounterNotPushed(EncounterType encounterType, Boolean includeFalse);
}
