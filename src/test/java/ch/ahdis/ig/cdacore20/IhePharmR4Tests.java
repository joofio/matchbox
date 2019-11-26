package ch.ahdis.ig.cdacore20;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.elementmodel.Element;
import org.hl7.fhir.r4.elementmodel.Manager;
import org.hl7.fhir.r4.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.test.utils.TestingUtilities;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.hl7.fhir.utilities.cache.PackageCacheManager;
import org.hl7.fhir.utilities.cache.ToolsVersion;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class IhePharmR4Tests {

	private SimpleWorkerContext context;
	private FHIRPathEngine fp;

	@Before
	public void setUp() throws Exception {
		context = new SimpleWorkerContext();
		PackageCacheManager pcm = new PackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
		context.loadFromPackage(pcm.loadPackage("hl7.fhir.core", "4.0.0"), null, "StructureDefinition");
		context.loadFromPackage(pcm.loadPackage("hl7.fhir.cda", "dev"), null, "StructureDefinition");
		fp = new FHIRPathEngine(context);
	}

	@Test
	public void testManufacturedMaterial()
			throws FHIRFormatError, DefinitionException, FileNotFoundException, IOException, FHIRException {
		try {

			String fileSource = TestingUtilities.resourceNameToFile("cda", "ihe-pharm-manufacturedmaterial.xml");
			String roundTrip = TestingUtilities.resourceNameToFile("cda", "ihe-pharm-manufacturedmaterial.out.xml");

			Element e = Manager.parse(context, new FileInputStream(fileSource), FhirFormat.XML);

			Manager.compose(context, e, new FileOutputStream(roundTrip), FhirFormat.XML, OutputStyle.PRETTY, null);

			
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
			
//		<lotNumberText>10</lotNumberText>
//		<pharm:expirationTime value="20210101" /> <!-- Container information -->
//		<pharm:asContent classCode="CONT">
//			<pharm:containerPackagedMedicine
//				classCode="CONT" determinerCode="INSTANCE"> <!-- Medicinal product code (package-level) -->
//				<pharm:code code="12" displayName="13" codeSystem="14"
//					codeSystemName="15" /> <!-- Brand name (package) -->
//				<pharm:name>16</pharm:name>
//				<pharm:formCode code="17" displayName="18"
//					codeSystem="19" codeSystemName="20" />
//				<pharm:capacityQuantity value="21" unit="22" />
//				<pharm:asSuperContent>
//					<pharm:containerPackagedMedicine
//						classCode='CONT' determinerCode='INSTANCE'>
//						<pharm:capacityQuantity value="41" unit='42' />
//					</pharm:containerPackagedMedicine>
//				</pharm:asSuperContent>
//			</pharm:containerPackagedMedicine>
//		</pharm:asContent>
//		<!-- These are optional generic equivalents -->
//		<pharm:asSpecializedKind classCode="GRIC">
//			<pharm:generalizedMedicineClass
//				classCode="MMAT">
//				<pharm:code code="23" displayName="Generic Equivalent"
//					codeSystem="24" codeSystemName="25" />
//				<pharm:name>26</pharm:name>
//			</pharm:generalizedMedicineClass>
//		</pharm:asSpecializedKind>
//		<!-- This is the list of active ingredients -->
//		<pharm:ingredient classCode="ACTI">
//			<!-- strength of ingredient -->
//			<pharm:quantity>
//				<numerator xsi:type="PQ" value="27" unit="28" />
			assertEquals("27", fp.evaluateToString(e, "ingredient[0].quantity.numerator.value"));
			List<Element> ingredients = e.getChildrenByName("ingredient");			
			assertEquals(2,ingredients.size());
			Element numerator = ingredients.get(0).getChildrenByName("quantity").get(0).getChildrenByName("numerator").get(0);
			assertEquals("PQ", numerator.getExplicitType());
//				<denominator xsi:type="PQ" value="29" unit="30" />
//			</pharm:quantity>
//			<pharm:ingredient classCode="MMAT"
//				determinerCode="KIND">
//				<pharm:code code="31" displayName="Active Ingredient 1"
//					codeSystem="32" codeSystemName="33" />
//				<pharm:name>Active Ingredient 1</pharm:name>
//			</pharm:ingredient>
//		</pharm:ingredient>
//		<pharm:ingredient classCode="ACTI">
//			<!-- strength of ingredient -->
//			<pharm:quantity>
//				<numerator xsi:type="PQ" value="34" unit="35" />
//				<denominator xsi:type="PQ" value="36" unit="37" />
//			</pharm:quantity>
//			<pharm:ingredient classCode="MMAT"
//				determinerCode="KIND">
//				<pharm:code code="38" displayName="Active Ingredient 2"
//					codeSystem="39" codeSystemName="40" />
//				<pharm:name>Active Ingredient 2</pharm:name>
//			</pharm:ingredient>
//		</pharm:ingredient>
//	</Material>

// old

//			//    <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
//			assertEquals("POCD_HD000040", fp.evaluateToString(e, "typeId.extension"));
//			assertEquals("2.16.840.1.113883.1.3", fp.evaluateToString(e, "typeId.root"));
////    <templateId root="2.16.840.1.113883.3.27.1776"/>
//			assertEquals("2.16.840.1.113883.3.27.1776", fp.evaluateToString(e, "templateId.root"));
////    <id extension="c266" root="2.16.840.1.113883.19.4"/>
//			assertEquals("2.16.840.1.113883.19.4", fp.evaluateToString(e, "id.root"));
//			assertEquals("c266", fp.evaluateToString(e, "id.extension"));
//
////    <title>Good Health Clinic Consultation Note</title>
//			assertEquals("Good Health Clinic Consultation Note", fp.evaluateToString(e, "title.dataString"));
////    <effectiveTime value="20000407"/>
//			assertEquals("2000-04-07", fp.evaluateToString(e, "effectiveTime.value"));
////    <confidentialityCode code="N" codeSystem="2.16.840.1.113883.5.25"/>
//			assertEquals("N", fp.evaluateToString(e, "confidentialityCode.code"));
//			assertEquals("2.16.840.1.113883.5.25", fp.evaluateToString(e, "confidentialityCode.codeSystem"));
////    <languageCode code="en-US"/>
//			assertEquals("en-US", fp.evaluateToString(e, "languageCode.code"));
////    <setId extension="BB35" root="2.16.840.1.113883.19.7"/>
//			assertEquals("BB35", fp.evaluateToString(e, "setId.extension"));
//			assertEquals("2.16.840.1.113883.19.7", fp.evaluateToString(e, "setId.root"));
////    <versionNumber value="2"/>
//			assertEquals("2", fp.evaluateToString(e, "versionNumber.value"));
////    <recordTarget>
////      <patientRole>
////        <id extension="12345" root="2.16.840.1.113883.19.5"/>
//			assertEquals("12345", fp.evaluateToString(e, "recordTarget.patientRole.id.extension"));
//			assertEquals("2.16.840.1.113883.19.5", fp.evaluateToString(e, "recordTarget.patientRole.id.root"));
////        <patient>
////          <name>
////            <given>Henry</given>
//			assertEquals("Henry", fp.evaluateToString(e, "recordTarget.patientRole.patient.name.given.dataString"));
////            <family>Levin</family>
//			assertEquals("Levin", fp.evaluateToString(e, "recordTarget.patientRole.patient.name.family.dataString"));
//
//
//			assertEquals("Skin Exam", fp.evaluateToString(e,
//					"component.structuredBody.component.section.component.section.where(code.code='8709-8' and code.codeSystem='2.16.840.1.113883.6.1').title.dataString"));
//
//			// <div>Erythematous rash, palmar surface, left index finger.
//			// <img src="MM1"/></div>
//			String text = fp.evaluateToString(e,
//					"component.structuredBody.component.section.component.section.where(code.code='8709-8' and code.codeSystem='2.16.840.1.113883.6.1').text");
//			assertTrue(text.contains("<img src=\"MM1\"/>"));

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	@Ignore
	public void testDCI() throws FHIRFormatError, DefinitionException, FileNotFoundException, IOException, FHIRException {
		try {
			Element e = Manager.parse(context,
					new FileInputStream("C:\\work\\org.hl7.fhir.us\\ccda-to-fhir-maps\\cda\\IAT2-Discharge_Summary-DCI.xml"),
					FhirFormat.XML);

			Manager.compose(context, e, new FileOutputStream("C:\\temp\\ccda.xml"), FhirFormat.XML, OutputStyle.PRETTY, null);
//    Manager.compose(context, e, new FileOutputStream("C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-Discharge_Summary-DCI.out.json"), FhirFormat.JSON, OutputStyle.PRETTY, null);
//    Manager.compose(context, e, new FileOutputStream("C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-Discharge_Summary-DCI.out.ttl"), FhirFormat.TURTLE, OutputStyle.PRETTY, null);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	@Ignore
	public void testEpic()
			throws FHIRFormatError, DefinitionException, FileNotFoundException, IOException, FHIRException {
		Element e = Manager.parse(context,
				new FileInputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-Discharge-Homework-Epic.xml"),
				FhirFormat.XML);
		Manager.compose(context, e,
				new FileOutputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-Discharge-Homework-Epic.out.xml"),
				FhirFormat.XML, OutputStyle.PRETTY, null);
		Manager.compose(context, e,
				new FileOutputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-Discharge-Homework-Epic.out.json"),
				FhirFormat.JSON, OutputStyle.PRETTY, null);
		Manager.compose(context, e,
				new FileOutputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-Discharge-Homework-Epic.out.ttl"),
				FhirFormat.TURTLE, OutputStyle.PRETTY, null);
	}

	@Ignore
	public void testDHIT()
			throws FHIRFormatError, DefinitionException, FileNotFoundException, IOException, FHIRException {
		Element e = Manager.parse(context,
				new FileInputStream("C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-DS-Homework-DHIT.xml"),
				FhirFormat.XML);
		Manager.compose(context, e,
				new FileOutputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-DS-Homework-DHIT.out.xml"),
				FhirFormat.XML, OutputStyle.PRETTY, null);
		Manager.compose(context, e,
				new FileOutputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-DS-Homework-DHIT.out.json"),
				FhirFormat.JSON, OutputStyle.PRETTY, null);
		Manager.compose(context, e,
				new FileOutputStream(
						"C:\\work\\org.hl7.fhir.test\\ccda-to-fhir-maps\\testdocuments\\IAT2-DS-Homework-DHIT.out.ttl"),
				FhirFormat.TURTLE, OutputStyle.PRETTY, null);
	}
}