package ch.ahdis.mapping;

/*
 * #%L
 * Matchbox Server
 * %%
 * Copyright (C) 2018 - 2019 ahdis
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Test;

abstract public class Version_30_40Test {
  
  public Version_30_40Test() {
    super();
  }

  abstract public org.hl7.fhir.dstu3.model.Resource convertResource(org.hl7.fhir.r4.model.Resource src) throws FHIRException;

  abstract public org.hl7.fhir.r4.model.Resource convertResource(org.hl7.fhir.dstu3.model.Resource src) throws FHIRException;

  
  @Test
  public void testStructureDefintionVersions() throws FHIRException {
    StructureDefinition stu3 = new org.hl7.fhir.dstu3.model.StructureDefinition();
    stu3.setFhirVersion("3.0.1");
    stu3.setTitle("test");
    stu3.setDescription("description");
    org.hl7.fhir.r4.model.StructureDefinition r4 = (org.hl7.fhir.r4.model.StructureDefinition) convertResource(stu3);
    assertNotNull(r4);
    assertNotNull(r4.getFhirVersion());
    assertEquals("4.0.0", r4.getFhirVersion().toCode());
    stu3 = (StructureDefinition) convertResource(r4);
    assertNotNull(stu3);
    assertNotNull(stu3.getFhirVersion());
    assertEquals("3.0.1", stu3.getFhirVersion());
  }
  
  @Test
  public void testVitalSignObservation() throws FHIRException {
    Observation stu3 = new org.hl7.fhir.dstu3.model.Observation();
    
    stu3.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category");

    org.hl7.fhir.r4.model.Observation r4 = (org.hl7.fhir.r4.model.Observation) convertResource(stu3);
    assertEquals("http://terminology.hl7.org/CodeSystem/observation-category", r4.getCategoryFirstRep().getCodingFirstRep().getSystem());

    stu3 = (Observation) convertResource(r4);
    assertEquals("http://hl7.org/fhir/observation-category", stu3.getCategoryFirstRep().getCodingFirstRep().getSystem());
  }
  
  
}
