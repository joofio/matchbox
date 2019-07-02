package ch.ahdis.mapping;

import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ch.ahdis.mapping.fml.MappingLanguageTransfomer_30_40;

public class MappingLanguageTansformer_30_40_Test extends Version_30_40Test {

	private FhirContext contextR3;

	public MappingLanguageTansformer_30_40_Test() {
	  this.contextR3 = FhirVersionEnum.DSTU3.newContext();
	}

	private MappingLanguageTransfomer_30_40 versionConvertor_30_40 = new MappingLanguageTransfomer_30_40();

	public org.hl7.fhir.r4.model.Resource convertResource(org.hl7.fhir.dstu3.model.Resource src) throws FHIRException {
		String r3 = contextR3.newXmlParser().encodeResourceToString(src);
		return versionConvertor_30_40.convertResource3To4(r3);
	}
  public org.hl7.fhir.dstu3.model.Resource convertResource(org.hl7.fhir.r4.model.Resource src) throws FHIRException {
  	return (Resource) contextR3.newJsonParser().parseResource(versionConvertor_30_40.convertResource4To3AsJson(src));
  }

  
}
