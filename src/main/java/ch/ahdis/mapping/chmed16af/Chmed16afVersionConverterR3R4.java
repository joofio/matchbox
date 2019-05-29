package ch.ahdis.mapping.chmed16af;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import ch.ahdis.mapping.IgVersionConverterR4;

public class Chmed16afVersionConverterR3R4 implements IgVersionConverterR4 {

	@Override
	public Resource upgrade(Resource resource) {
		if (resource instanceof DomainResource) {
			DomainResource rsc = (DomainResource) resource;
			if (rsc.hasExtension()) {
				upgradeResourceWithExtensions(rsc);
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
	 * @param resource
	 */
	private void upgradeResourceWithExtensions(DomainResource resource) {
		if (resource.hasExtension()) {
			List<Extension> extensions = resource.getExtension();
			for (int i=0; i<extensions.size(); ++i) {
				Extension ext= extensions.get(i);
				String privateFieldNameExt = "http://chmed16af.emediplan.ch/fhir/StructureDefinition/";
				if (ext.getUrl().startsWith(privateFieldNameExt) && !(ext.getUrl().equals("http://chmed16af.emediplan.ch/fhir/StructureDefinition/chmed16af-receiver"))) {
					Extension r4 = new Extension();
					r4.setUrl("http://chmed16af.emediplan.ch/fhir/StructureDefinition/chmed16af-privatefield");
					r4.addExtension("name", new StringType(ext.getUrl().substring(privateFieldNameExt.length())));
					r4.addExtension("value", ext.getValue());
					resource.getExtension().set(i, r4);
				}
			}
		}
	}

	private void downgradeResourceWithExtensions(DomainResource resource) {
		if (resource.hasExtension()) {
			List<Extension> extensions = new ArrayList<Extension>();
			extensions.addAll(resource.getExtension());
			for (int i=0; i<extensions.size(); ++i) {
				Extension ext= extensions.get(i);
				if (ext.getUrl().equals("http://chmed16af.emediplan.ch/fhir/StructureDefinition/chmed16af-privatefield")) {
					Extension r3 = new Extension();
					Extension name = ext.getExtensionByUrl("name");
					Extension value = ext.getExtensionByUrl("value");
					if (name != null && value != null) {
						r3.setUrl("http://chmed16af.emediplan.ch/fhir/StructureDefinition/" + name.getValue());
						r3.setValue(value.getValue());
						resource.getExtension().set(i, r3);
					}
				}
			}
		}
	}

	@Override
	public Resource downgrade(Resource resource) {
		if (resource instanceof DomainResource ) {
			DomainResource rsc = (DomainResource) resource;
			if (rsc.hasExtension()) {
				downgradeResourceWithExtensions(rsc);
			}
		}
		return resource;
	}

}
