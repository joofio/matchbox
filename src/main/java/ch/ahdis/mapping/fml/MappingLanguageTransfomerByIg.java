package ch.ahdis.mapping.fml;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.elementmodel.Manager;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceFactory;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.terminologies.ConceptMapEngine;
import org.hl7.fhir.r4.utils.StructureMapUtilities;
import org.hl7.fhir.r4.utils.StructureMapUtilities.ITransformerServices;

public class MappingLanguageTransfomerByIg extends MappingLanguageTransfomer implements ITransformerServices {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MappingLanguageTransfomerByIg.class);

	public String ig = "";
	public String canonical = "";

	private SimpleWorkerContext contextR4;

	public MappingLanguageTransfomerByIg(String ig) {
		this.ig = ig;
		try {
			this.checkLoad();
		} catch (FHIRException e) {
			log.error("failed to load ig", e);
		}
	}

	public Resource convertResource(Resource r4, String versionFrom, String versionTo) throws FHIRException {
		StructureMapUtilities smu4 = new StructureMapUtilities(contextR4, this);
		String tn = r4.fhirType();
		if (r4 != null) {
			String map = canonical + "/StructureMap/" + tn + versionFrom + "to" + versionTo;
			StructureMap sm = contextR4.getTransform(map);
			if (sm != null) {
				tn = smu4.getTargetType(sm).getType();
				Resource target = ResourceFactory.createResource(tn);
				smu4.transform(contextR4, r4, sm, target);
				return target;
			} else {
				log.debug(
						"no map found for " + tn + " from version " + versionFrom + " to" + versionTo + " was looking for " + map);
			}
		}
		return r4;
	}

	private void checkLoad() throws FHIRException {
		if (contextR4 != null)
			return;
		try {
			log.debug("loading R4");
			contextR4 = new SimpleWorkerContext();
			contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.core", "4.0.0"));
			contextR4.setCanRunWithoutTerminology(true);
		} catch (FileNotFoundException e) {
			throw new FHIRException(e);
		} catch (IOException e) {
			throw new FHIRException(e);
		} finally {

		}

		log.debug("loading Maps");
		loadMapsForIg(ig);

		log.debug("loaded");
	}

	public void loadMapsForIg(String src) throws FHIRException {
		Map<String, byte[]> source = null;
		try {
			source = loadIgSource(src);
		} catch (Exception e1) {
			log.error("failed loading ig " + src, e1);
			throw new FHIRException(e1);
		}
		for (Entry<String, byte[]> t : source.entrySet()) {
			org.hl7.fhir.r4.model.Resource res = null;
			String fn = t.getKey();
			try {
				if (fn.endsWith(".json") && !fn.endsWith("template.json"))
					res = new org.hl7.fhir.r4.formats.JsonParser().parse(new ByteArrayInputStream(t.getValue()));
			} catch (Exception e) {
				throw new FHIRException("Error parsing " + fn + ": " + e.getMessage(), e);
			}
			if (res != null) {
				contextR4.cacheResource(res);
				if (res instanceof MetadataResource) {
					for (Resource r : ((MetadataResource) res).getContained()) {
						if (r instanceof MetadataResource) {
							MetadataResource mr = (MetadataResource) r.copy();
							mr.setUrl(((MetadataResource) res).getUrl() + "#" + r.getId());
							contextR4.cacheResource(mr);
						}
					}
				}
				if (res instanceof ImplementationGuide) {
					this.canonical = ((ImplementationGuide) res).getUrl().substring(0,
							((ImplementationGuide) res).getUrl().lastIndexOf("ImplementationGuide") - 1);
					log.debug("canonical " + this.canonical);
				}
			}
		}
	}

	@Override
	public void log(String message) {
		log.debug(message);
	}


	@Override
	public Base createType(Object appInfo, String name) throws FHIRException {
		StructureDefinition sd = contextR4.fetchResource(StructureDefinition.class, name);
		if (sd != null && sd.getKind() == StructureDefinitionKind.LOGICAL) {
			return Manager.build(contextR4, sd);
		} else {
			if (name.startsWith("http://hl7.org/fhir/StructureDefinition/"))
				name = name.substring("http://hl7.org/fhir/StructureDefinition/".length());
			return ResourceFactory.createResourceOrType(name);
		}
	}

	@Override
	public Base createResource(Object appInfo, Base res, boolean atRootofTransform) {
		return res;
	}

	@Override
	public Coding translate(Object appInfo, Coding source, String conceptMapUrl) throws FHIRException {
		ConceptMapEngine cme = new ConceptMapEngine(contextR4);
		return cme.translate(source, conceptMapUrl);
	}

	@Override
	public Base resolveReference(Object appContext, String url) throws FHIRException {
		return null;
	}

	@Override
	public List<Base> performSearch(Object appContext, String url) throws FHIRException {
		return null;
	}

}
