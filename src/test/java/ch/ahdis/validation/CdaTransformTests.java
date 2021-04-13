package ch.ahdis.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ch.ahdis.matchbox.MatchboxApplication;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = { MatchboxApplication.class })
public class CdaTransformTests {
  
  private FhirContext contextR4 = FhirVersionEnum.R4.newContext();
  private GenericFhirClient genericClient = new GenericFhirClient(contextR4, this.targetServer);


//  @ClassRule
//  public static final SpringClassRule scr = new SpringClassRule();

//  @Rule
//  public final SpringMethodRule smr = new SpringMethodRule();

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdaTransformTests.class);

  private String targetServer = "http://test.ahdis.ch/r4";
  
  @Before 
  public void setup() {
    contextR4 = FhirVersionEnum.R4.newContext();
    genericClient = new GenericFhirClient(contextR4, this.targetServer);
  }

  @Test
  public void convertCdaToBundle() {
    String cda = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\"><typeId root=\"2.16.840.1.113883.1.3\" extension=\"POCD_HD000040\"/>\n" + 
        "    <templateId root=\"2.16.756.5.30.1.127.1.4\"/></ClinicalDocument>";
    IBaseResource bundle = genericClient.convert(cda, EncodingEnum.XML,"http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaToBundle", "application/json+fhir");
    assertEquals("Bundle", bundle.fhirType());
  }
  
  @Test
  public void convertBundleToCda() {
    String uuid = "urn:uuid:"+UUID.randomUUID().toString();
    Bundle bundle = new Bundle();
    bundle.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue(uuid));
    
    String cda = genericClient.convert(bundle, EncodingEnum.XML,"http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCda", "text/xml");
    assertNotNull(cda);
    assertTrue(cda.indexOf("ClinicalDocument")>0);
  }
  
  @Test
  public void convertBundleToCdaToBundle() {
    String uuid = "urn:uuid:"+UUID.randomUUID().toString();
    String entryUuid = "urn:uuid:"+UUID.randomUUID().toString();
    String entry2Uuid = "urn:uuid:"+UUID.randomUUID().toString();
    Bundle bundle = new Bundle();
    bundle.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue(uuid));
    bundle.setType(BundleType.DOCUMENT);
    Composition composition = new Composition();
    composition.setIdentifier(new Identifier().setSystem("urn:ietf:rfc:3986").setValue(uuid));
    composition.setStatus(CompositionStatus.FINAL);
    composition.setSubject(new Reference().setReference(entry2Uuid).setType("Patient"));
    bundle.addEntry().setFullUrl(entryUuid).setResource(composition);
    Patient patient = new Patient();
    patient.addName().addGiven("Given").setFamily("Family");
    bundle.addEntry().setFullUrl(entry2Uuid).setResource(patient);

    String cda = genericClient.convert(bundle, EncodingEnum.XML,"http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCda", "text/xml");
    assertNotNull(cda);
    Bundle bundleReceived = (Bundle) genericClient.convert(cda, EncodingEnum.XML,"http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaToBundle", "application/json+fhir");
    assertNotNull(bundleReceived);
    
    compare(bundle, bundleReceived);
  }  
  
  public String[] getUuids(Bundle bundle) {
    ArrayList<String> uuids = new ArrayList<String>();
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getFullUrl()!=null && entry.getFullUrl().startsWith("urn:uuid:")) {
        uuids.add(entry.getFullUrl());
      }
    }
    return uuids.toArray(new String[0]);
  }
  
  public String replaceUuids(String json, String uuids[]) {
    if (uuids==null || uuids.length==0) {
      return json;
    }
    for (int i=0; i<uuids.length; ++i) {
      json = json.replaceAll(uuids[i], "res:"+i);
    }
    return json;
  }
  
  public void compare(IBaseResource left, IBaseResource right) {
    
    String jsonLeft = contextR4.newJsonParser().encodeResourceToString(left);
    if (left.fhirType().equals("Bundle")) {
      jsonLeft = replaceUuids(jsonLeft, getUuids((Bundle) left));      
    }
    String jsonRight = contextR4.newJsonParser().encodeResourceToString(right);
    if (right.fhirType().equals("Bundle")) {
      jsonRight = replaceUuids(jsonRight, getUuids((Bundle) right));
    }
    Gson g = new Gson();
    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
    Map<String, Object> leftMap = g.fromJson(jsonLeft, mapType);
    Map<String, Object> rightMap = g.fromJson(jsonRight, mapType);
    
    Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
    Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);
    
    MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
    if (!difference.areEqual()) {
      log.error(difference.toString());
      log.error("entries only on left : "+ difference.entriesOnlyOnLeft().toString());
      log.error("entries only on right: "+ difference.entriesOnlyOnRight().toString());
      log.error("entries differing    : "+ difference.entriesDiffering().toString());
      assertEquals("", difference.toString());
    }
  }

}
