package ch.ahdis.mapping.R3R4;

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

import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Test;

public class VersionConverter_30_40Test {
  
  public VersionConverter_30_40Test() {
    super();
  }

  public org.hl7.fhir.dstu3.model.Resource convertResource(org.hl7.fhir.r4.model.Resource src) throws FHIRException {
    return VersionConvertor_30_40.convertResource(src, true);
  }

  public org.hl7.fhir.r4.model.Resource convertResource(org.hl7.fhir.dstu3.model.Resource src) throws FHIRException {
    return VersionConvertor_30_40.convertResource(src, true);
  }

  @Test
  public void testConvertDateTime() throws FHIRException {
    String date = "2019-01-01";
    DateTimeType stu3 = new DateTimeType(date);
    org.hl7.fhir.r4.model.DateTimeType r4 = VersionConvertor_30_40.convertDateTime(stu3);
    assertEquals(date, r4.getValueAsString());
    stu3 = VersionConvertor_30_40.convertDateTime(r4);
    assertEquals(date, stu3.getValueAsString());    
  }
  
  @Test
  public void testConvertPeriod() throws FHIRException {
    String date = "2019-01-01";
    DateTimeType dateTimeStu3 = new DateTimeType(date);
    Period periodStu3 = new Period(); 
    periodStu3.setStartElement(dateTimeStu3);
    org.hl7.fhir.r4.model.Period periodR4 = VersionConvertor_30_40.convertPeriod(periodStu3);
    assertEquals(date, periodR4.getStartElement().getValueAsString());
    periodStu3 = VersionConvertor_30_40.convertPeriod(periodR4);
    assertEquals(date, periodStu3.getStartElement().getValueAsString());
  }
  
  
  @Test
  public void testStructureDefintionVersions() throws FHIRException {
    StructureDefinition stu3 = new org.hl7.fhir.dstu3.model.StructureDefinition();
    stu3.setFhirVersion("3.0.1");
    org.hl7.fhir.r4.model.StructureDefinition r4 = (org.hl7.fhir.r4.model.StructureDefinition) convertResource(stu3);
    assertEquals("4.0.0", r4.getFhirVersion().toCode());
    stu3 = (StructureDefinition) convertResource(r4);
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
