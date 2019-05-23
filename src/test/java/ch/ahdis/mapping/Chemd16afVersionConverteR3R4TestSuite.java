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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ch.ahdis.mapping.chmed16af.Chmed16afVersionConverterR3R4;

@RunWith(Parameterized.class)
public class Chemd16afVersionConverteR3R4TestSuite {

  private FhirContext contextStu3;
  private FhirContext contextR4;
  private String resource;
  private VersionConvertor_30_40 versionConvertor_30_40 = null;


  private static List<String> getResourceFiles(String path) throws IOException {
    List<String> filenames = new ArrayList<>();

    try (InputStream in = getResourceAsStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
      String resource;

      while ((resource = br.readLine()) != null) {
        filenames.add(resource);
      }
    }

    return filenames;
  }

  private static InputStream getResourceAsStream(String resource) {
    final InputStream in = getContextClassLoader().getResourceAsStream(resource);

    return in == null ? new Marker().getClass().getResourceAsStream(resource) : in;
  }

  private static ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @Parameters(name = "{index}: file {0}")
  public static Iterable<Object[]> data() throws ParserConfigurationException, IOException, FHIRFormatError {

    List<String> filesR3 = getResourceFiles("/mapping/chmed16af/stu3");
    List<Object[]> objects = new ArrayList<Object[]>(filesR3.size());

    for (String fn : filesR3) {
      objects.add(new Object[] { fn });
    }
    return objects;
  }

  public Chemd16afVersionConverteR3R4TestSuite(String resource) {
    super();
    this.resource = resource;
    this.contextStu3 = FhirVersionEnum.DSTU3.newContext();
    this.contextR4 = FhirVersionEnum.R4.newContext();
    this.versionConvertor_30_40 = new VersionConvertor_30_40();
    this.versionConvertor_30_40.addImplemenationGuideVersionConverter(new Chmed16afVersionConverterR3R4());
  }

  public org.hl7.fhir.dstu3.model.Resource convertResource(org.hl7.fhir.r4.model.Resource src) throws FHIRException {
    return versionConvertor_30_40.convertResource(src, true);
  }

  public org.hl7.fhir.r4.model.Resource convertResource(org.hl7.fhir.dstu3.model.Resource src) throws FHIRException {
    return versionConvertor_30_40.convertResource(src, true);
  }
  
  @Test
  public void test() throws FHIRException {
    String resourceStu3 = "/mapping/chmed16af/stu3/"+this.resource;
    String resourceR4 = "/mapping/chmed16af/r4/"+this.resource;
    
    org.hl7.fhir.dstu3.model.Resource stu3 = (org.hl7.fhir.dstu3.model.Resource) contextStu3.newXmlParser()
        .parseResource(getClass().getResourceAsStream(resourceStu3));
    org.hl7.fhir.r4.model.Resource r4goal = (org.hl7.fhir.r4.model.Resource) contextR4.newXmlParser()
        .parseResource(getClass().getResourceAsStream(resourceR4));
    org.hl7.fhir.r4.model.Resource r4 = convertResource(stu3);

    assertEquals(contextR4.newXmlParser().encodeResourceToString(r4goal),
        contextR4.newXmlParser().encodeResourceToString(r4));

    org.hl7.fhir.dstu3.model.Resource stu3back = convertResource(r4);
    assertEquals(contextStu3.newXmlParser().encodeResourceToString(stu3),
        contextStu3.newXmlParser().encodeResourceToString(stu3back));
  }

}
