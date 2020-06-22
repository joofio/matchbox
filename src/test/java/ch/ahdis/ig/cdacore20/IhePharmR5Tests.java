package ch.ahdis.ig.cdacore20;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.test.utils.TestingUtilities;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.utilities.cache.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.cache.ToolsVersion;
import org.junit.Before;
import org.junit.Test;

public class IhePharmR5Tests {

	private SimpleWorkerContext context;
	private FHIRPathEngine fp;

	@Before
	public void setUp() throws Exception {
		context = new SimpleWorkerContext();
	    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
	    context = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"));
	    fp = new FHIRPathEngine(context);
		context.loadFromPackage(pcm.loadPackage("hl7.fhir.cda", "dev"), null, "StructureDefinition");
		fp = new FHIRPathEngine(context);
	}

	@Test
	public void testManufacturedMaterial()
			throws FHIRFormatError, DefinitionException, FileNotFoundException, IOException, FHIRException {
		try {

		  InputStream fileSource = TestingUtilities.loadTestResourceStream("cda", "ihe-pharm-manufacturedmaterial.xml");

	        ByteArrayOutputStream baosXml = new ByteArrayOutputStream();

			Element e = Manager.parse(context, fileSource, FhirFormat.XML);

			Manager.compose(context, e, baosXml, FhirFormat.XML, OutputStyle.PRETTY, null);
			
			System.out.println(baosXml);

			
//		<Material xmlns:pharm="urn:ihe:pharm" xmlns="urn:hl7-org:v3"
//		xmlns:voc="urn:hl7-org:v3/voc"
//		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
//		xsi:schemaLocation="urn:hl7-org:v3 CDA.xsd" classCode="MMAT"
//		determinerCode="KIND">
//		<templateId root="1.3.6.1.4.1.19376.1.9.1.3.1" />
			assertEquals("1.3.6.1.4.1.19376.1.9.1.3.1", fp.evaluateToString(e, "templateId.root"));

//		<!-- National medicinal product code (brand-level) -->
//		<code code="1" displayName="2" codeSystem="3" codeSystemName="4" /> <!-- Brand name -->
//		<name>5</name>
//		<!-- Pharmaceutical dose form -->
//		<pharm:formCode code="6" displayName="7" codeSystem="8"
//			codeSystemName="9" />
			assertEquals("6", fp.evaluateToString(e, "formCode.code"));
			List<Element> elements = e.getChildrenByName("formCode");
			
			assertEquals(1,elements.size());
			Element elementFormCode = elements.get(0);
			assertEquals("27", fp.evaluateToString(e, "ingredient[0].quantity.numerator.value"));
			List<Element> ingredients = e.getChildrenByName("ingredient");			
			assertEquals(2,ingredients.size());
			Element numerator = ingredients.get(0).getChildrenByName("quantity").get(0).getChildrenByName("numerator").get(0);
//			assertEquals("PQ", numerator.getExplicitType());


		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

}