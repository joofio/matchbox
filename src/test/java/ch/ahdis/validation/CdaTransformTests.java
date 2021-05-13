package ch.ahdis.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
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
import com.google.common.collect.MapDifference.ValueDifference;
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

  private String targetServer = "http://localhost:8080/r4";
  
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
    
    String cda = genericClient.convert(bundle, EncodingEnum.JSON,"http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCda", "text/xml");
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

    Bundle bundleReceived = convertBundleToCdaAndBack(bundle, "http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCda", "http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaToBundle");
    
    compare(bundle, bundleReceived, false);
  }

  private Bundle convertBundleToCdaAndBack(Bundle bundle, String mapToCda, String mapToBundle) {
    String cda = genericClient.convert(bundle, EncodingEnum.JSON,mapToCda, "text/xml");
    assertNotNull(cda);
    Bundle bundleReceived = (Bundle) genericClient.convert(cda, EncodingEnum.XML, mapToBundle, "application/json+fhir");
    assertNotNull(bundleReceived);
    return bundleReceived;
  }  


  @Test
  public void convertChSetId() throws IOException {
    InputStream inputStream =   getClass().getResourceAsStream("/transform/ch-emed/ch-setId.xml");
    
    Bundle bundle = (Bundle) contextR4.newXmlParser().parseResource(inputStream);
    assertNotNull(bundle);
  
    Bundle bundleReceived = convertBundleToCdaAndBack(bundle, "http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCdaCh", "http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaChToBundle");
  
    compare(bundle, bundleReceived, true);
  }

//  @Test
//  public void convertChEMedMedicationCard() throws IOException {
//    InputStream inputStream =   getClass().getResourceAsStream("/transform/ch-emed/2-7-MedicationCard.json");
//    
//    Bundle bundle = (Bundle) contextR4.newJsonParser().parseResource(inputStream);
//    assertNotNull(bundle);
//
//    Bundle bundleReceived = convertBundleToCdaAndBack(bundle, "http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCdaChEmedMedicationCardDocument", "http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaChEmedMedicationCardDocumentToBundle");
//
//    compare(bundle, bundleReceived);
//  }

  
  
  public Map<String, String> harmonizeBunldeIds(Bundle bundle) {
    Map<String, String>  ids = new  HashMap<String, String>();
    int index = 0;
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getFullUrl()!=null) {
        String resourceId = "res:"+index++;
        ids.put(entry.getFullUrl(), resourceId);
        if (entry.getFullUrl().startsWith("http")) {
          int slashResource = entry.getFullUrl().lastIndexOf("/");
          if (slashResource > 0) {
            int relResource = entry.getFullUrl().lastIndexOf("/", slashResource-1);
            String relUrl = entry.getFullUrl().substring(relResource+1);
            ids.put(relUrl, resourceId);
          }
        }
      }
    }
    return ids;
  }
  
  public String fshy(String text, Map<String, Object> map) {
    String result = text + "\n";
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      result += entry.getKey() + " = " + entry.getValue() + "\n";
    }
    result += "\n";
    return result;
  }
  
  public String fshyDifferrence(String text, Map<String, ValueDifference<Object>> map) {
    String result = text + "\n";
    for (Map.Entry<String, ValueDifference<Object>> entry : map.entrySet()) {
      result += entry.getKey() + " = " + entry.getValue() + "\n";
    }
    result += "\n";
    return result;
  }

  
  public void compare(IBaseResource left, IBaseResource right, boolean onlyDiffering) {
    
    
    Map<String, String> bundleLeftIds = null;
    Map<String, String> bundleRightIds = null;
    
    if (left.fhirType().equals("Bundle") && right.fhirType().equals("Bundle")) {
      bundleLeftIds = harmonizeBunldeIds((Bundle) left);
      bundleRightIds = harmonizeBunldeIds((Bundle) right);
    }
    
    String jsonLeft = contextR4.newJsonParser().encodeResourceToString(left);
    String jsonRight = contextR4.newJsonParser().encodeResourceToString(right);
    Gson g = new Gson();
    
    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
    
    Map<String, Object> leftMap = g.fromJson(jsonLeft, mapType);
    Map<String, Object> rightMap = g.fromJson(jsonRight, mapType);
    
    Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap, bundleLeftIds);
    Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap, bundleRightIds);
    
    
    log.debug(fshy("resulting transform",rightFlatMap));
    
    MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
    if (!difference.areEqual()) {
      log.error(difference.toString());
      log.error(fshy("entries only on left",difference.entriesOnlyOnLeft()));
      log.error(fshy("entries only on right",difference.entriesOnlyOnRight()));
      log.error(fshyDifferrence("entries differing",difference.entriesDiffering()));
    }

    assertTrue(onlyDiffering ? difference.entriesDiffering().isEmpty() : difference.areEqual());

  }

}
