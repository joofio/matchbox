package ch.ahdis.mapping.chmed16af;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import ch.ahdis.mapping.IgVersionConverterR4;

public class Chmed16afVersionConverterR3R4 implements IgVersionConverterR4 {

	@Override
	public Resource upgrade(Resource resource) {
		if (resource instanceof Patient) {
			Patient rsc = (Patient) resource;
			if (rsc.hasExtension()) {
				upgradePatientWithExtensions(rsc);
			}
		}
		return resource;
	}

	/**
	 * PrivateFields changed, in R3 
	 * 
	 * <extension url="http://chmed16af.emediplan.ch/fhir/StructureDefinition/PrivateFieldNameSample">
	 *   <valueString value="PrivateFieldValueSample"/> 
	 * </extension>
	 * 
	 * in R4 it is defined as complex extension to be able validate
	 * 
	 * <extension url="http://chmed16af.emediplan.ch/fhir/StructureDefinition/chmed16af-privatefield">
	 *   <extension url="name"> 
	 *     <valueString value="PrivateFieldNameSample" />
	 *   </extension> 
	 *   <extension url="value">
	 *      <valueString value="PrivateFieldValueSample" /> 
	 *   </extension> 
	 * </extension>
	 * 
	 * @param patient
	 */
	private void upgradePatientWithExtensions(Patient patient) {
		if (patient.hasExtension()) {
			List<Extension> extensions = new ArrayList<Extension>();
			extensions.addAll(patient.getExtension());
			for (Extension ext : extensions) {
				String privateFieldNameExt = "http://chmed16af.emediplan.ch/fhir/StructureDefinition/";
				if (ext.getUrl().startsWith(privateFieldNameExt)) {
					Extension r4 = new Extension();
					r4.setUrl("http://chmed16af.emediplan.ch/fhir/StructureDefinition/chmed16af-privatefield");
					r4.addExtension("name", new StringType(ext.getUrl().substring(privateFieldNameExt.length())));
					r4.addExtension("value", ext.getValue());
					patient.addExtension(r4);
					patient.getExtension().remove(ext);
				}
			}
		}
	}

	private void downgradePatientWithExtensions(Patient patient) {
		if (patient.hasExtension()) {
			List<Extension> extensions = new ArrayList<Extension>();
			extensions = patient
					.getExtensionsByUrl("http://chmed16af.emediplan.ch/fhir/StructureDefinition/chmed16af-privatefield");
			for (Extension ext : extensions) {
				Extension r3 = new Extension();
				Extension name = ext.getExtensionByUrl("name");
				Extension value = ext.getExtensionByUrl("value");
				if (name != null && value != null) {
					r3.setUrl("http://chmed16af.emediplan.ch/fhir/StructureDefinition/" + name.getValue());
					r3.setValue(value.getValue());
					patient.addExtension(r3);
					patient.getExtension().remove(ext);
				}
			}
		}
	}

	@Override
	public Resource downgrade(Resource resource) {
		if (resource instanceof Patient) {
			Patient rsc = (Patient) resource;
			if (rsc.hasExtension()) {
				downgradePatientWithExtensions(rsc);
			}
		}
		return resource;
	}

}
