package ch.ahdis.mapping;

import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ch.ahdis.mapping.fml.MappingLanguageTransfomerByIg;
import ch.ahdis.mapping.fml.MappingLanguageTransfomer_30_40;

@RunWith(Parameterized.class)
public class MappingLanguageTransfomerChemd16afTestSuite extends Chemd16afTestSuite {

	static private MappingLanguageTransfomer_30_40 versionConvertor_30_40 = new MappingLanguageTransfomer_30_40();
	static private MappingLanguageTransfomerByIg chmed16af = new MappingLanguageTransfomerByIg(
			"/Users/oliveregger/Documents/github/chmed16af/output");
	private FhirContext contextR3;

	public MappingLanguageTransfomerChemd16afTestSuite(String resource) {
		super(resource);
		this.contextR3 = FhirVersionEnum.DSTU3.newContext();
	}

	public org.hl7.fhir.r4.model.Resource convertResource(org.hl7.fhir.dstu3.model.Resource src) throws FHIRException {
		String r3 = contextR3.newXmlParser().encodeResourceToString(src);
		org.hl7.fhir.r4.model.Resource res = versionConvertor_30_40.convertResource3To4(r3);
		return chmed16af.convertResource(res, "0.1.0", "0.2.0");
	}

	public org.hl7.fhir.dstu3.model.Resource convertResource(org.hl7.fhir.r4.model.Resource src) throws FHIRException {
		src = chmed16af.convertResource(src, "0.2.0", "0.1.0");
		return (Resource) contextR3.newJsonParser().parseResource(versionConvertor_30_40.convertResource4To3AsJson(src));
	}

}
