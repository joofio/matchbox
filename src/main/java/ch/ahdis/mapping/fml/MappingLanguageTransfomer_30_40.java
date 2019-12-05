package ch.ahdis.mapping.fml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hl7.fhir.dstu3.elementmodel.ObjectConverter;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.context.BaseWorkerContext;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.elementmodel.Element;
import org.hl7.fhir.r4.elementmodel.Manager;
import org.hl7.fhir.r4.formats.IParser.OutputStyle;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceFactory;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.utils.StructureMapUtilities;
import org.hl7.fhir.r4.utils.StructureMapUtilities.ITransformerServices;

import ca.uhn.fhir.rest.api.EncodingEnum;

public class MappingLanguageTransfomer_30_40 extends MappingLanguageTransfomer implements ITransformerServices {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MappingLanguageTransfomer_30_40.class);

  static private SimpleWorkerContext contextR3;
  static private SimpleWorkerContext contextR4;
  private String igR3R4 = "fhir.versions.r3r4#dev";

  public MappingLanguageTransfomer_30_40() {
    try {
      this.checkLoad();
    } catch (FHIRException e) {
      log.error("could not initialize", e);
    }
  }

  public StructureMap parseMap(String content) throws FHIRException {
    org.hl7.fhir.r4.elementmodel.Element r4 = null;
    StructureMapUtilities smu4 = new StructureMapUtilities(contextR4, this);
    StructureMap map = smu4.parse(content, "map");
    return map;
  }

  public Resource convertResource3To4(String content) throws FHIRException {
    org.hl7.fhir.r4.elementmodel.Element r3 = null;
    try {
      r3 = new org.hl7.fhir.r4.elementmodel.XmlParser(contextR3).parse(new ByteArrayInputStream(content.getBytes()));
    } catch (IOException e) {
      log.error("parsing failed", e);
      throw new FHIRException(e);
    }
    return convertResource3To4(r3);
  }

  public Resource convertResource3To4(org.hl7.fhir.r4.elementmodel.Element r3) throws FHIRException {
    this.checkLoad();
    StructureMapUtilities smu4 = new StructureMapUtilities(contextR4, this);
    String tn = r3.fhirType();
    StructureMap sm = contextR4.getTransform("http://hl7.org/fhir/StructureMap/" + tn + "3to4");
    if (sm != null) {
      tn = smu4.getTargetType(sm).getType();
      // convert from r3 to r4
      Resource r4 = ResourceFactory.createResource(tn);
      smu4.transform(contextR4, r3, sm, r4);
      return r4;
    }
    return null;
  }

  public org.hl7.fhir.r4.elementmodel.Element convertResource4To3(Resource r4) throws FHIRException {
    this.checkLoad();
    StructureMapUtilities smu3 = new StructureMapUtilities(contextR3, this);
    String tn = r4.fhirType();
    StructureMap sm = contextR4.getTransform("http://hl7.org/fhir/StructureMap/" + tn + "4to3");
    if (sm != null) {
      StructureDefinition sd = smu3.getTargetType(sm);
      org.hl7.fhir.r4.elementmodel.Element ro3 = Manager.build(contextR3, sd);
      smu3.transform(contextR3, r4, sm, ro3);
      return ro3;
    }
    return null;
  }

  public String convertResource4To3AsJson(Resource r4) throws FHIRException {
    this.checkLoad();
    Element r3 = convertResource4To3(r4);
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    try {
      new org.hl7.fhir.r4.elementmodel.JsonParser(contextR3).compose(r3, bs, OutputStyle.PRETTY, null);
    } catch (IOException e) {
      log.error("parsing failed", e);
      throw new FHIRException(e);
    }
    return bs.toString();
  }

  /*
   * See R3R4ConversionTsts.java, adapted to Supporting multiple versions at once
   * is a little tricky. We're going to have 2 contexts: - an R3 context which is
   * used to read/write R3 instances - an R4 context which is used to perform the
   * transforms
   * 
   * R3 structure definitions are cloned into R3 context with a modified URL (as
   * 3.0/)
   * 
   */
  private void checkLoad() throws FHIRException {
    if (contextR4 != null)
      return;

    R3ToR4Loader ldr = new R3ToR4Loader().setPatchUrls(true).setKillPrimitives(true);

    log.debug("loading R3");

    try {
      contextR3 = new SimpleWorkerContext();
      contextR3.setAllowLoadingDuplicates(true);
      contextR3.setOverrideVersionNs("http://hl7.org/fhir/3.0/StructureDefinition");
      contextR3.loadFromPackage(pcm.loadPackage("hl7.fhir.r3.core", "3.0.2"), ldr, new String[] {});

      log.debug("loading R4");
      contextR4 = new SimpleWorkerContext();

      contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"));
      contextR4.setCanRunWithoutTerminology(true);
//      contextR4.setAllowLoadingDuplicates(true);
    } catch (FileNotFoundException e) {
      throw new FHIRException(e);
    } catch (IOException e) {
      throw new FHIRException(e);
    } finally {

    }

    log.debug("caching Resources");
    for (StructureDefinition sd : contextR3.allStructures()) {
      StructureDefinition sdn = sd.copy();
      sdn.getExtension().clear();
      contextR4.cacheResource(sdn);
    }

    for (StructureDefinition sd : contextR4.allStructures()) {
      if (sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE) {
        contextR3.cacheResource(sd);
        StructureDefinition sdn = sd.copy();
        sdn.setUrl(sdn.getUrl().replace("http://hl7.org/fhir/", "http://hl7.org/fhir/3.0/"));
        sdn.addExtension().setUrl("http://hl7.org/fhir/StructureDefinition/elementdefinition-namespace")
            .setValue(new UriType("http://hl7.org/fhir"));
        contextR3.cacheResource(sdn);
        contextR4.cacheResource(sdn);
      }
    }

    contextR3.setExpansionProfile(new org.hl7.fhir.r4.model.Parameters());
    contextR4.setExpansionProfile(new org.hl7.fhir.r4.model.Parameters());
    contextR3.setName("R3");
    contextR4.setName("R4");

    log.debug("loading Maps");

    loadMapsForIg(igR3R4);

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
      if (res instanceof MetadataResource) {
        if (res != null && !contextR4.hasResource(res.getClass(), ((MetadataResource) res).getUrl())) {
          contextR3.cacheResource(res);
          contextR4.cacheResource(res);
          log.debug("adding " + ((MetadataResource) res).getUrl());
          for (Resource r : ((MetadataResource) res).getContained()) {
            MetadataResource mr = (MetadataResource) r.copy();
            mr.setUrl(((MetadataResource) res).getUrl() + "#" + r.getId());
            contextR3.cacheResource(mr);
            contextR4.cacheResource(mr);
          }
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
    BaseWorkerContext context = (BaseWorkerContext) appInfo;
    if (context == contextR3) {
      StructureDefinition sd = context.fetchResource(StructureDefinition.class,
          "http://hl7.org/fhir/3.0/StructureDefinition/" + name);
      if (sd == null)
        throw new FHIRException("Type not found: '" + name + "'");
      return Manager.build(context, sd);
    } else
      return ResourceFactory.createResourceOrType(name);
  }

  @Override
  public Base createResource(Object appInfo, Base res, boolean atRootofTransform) {
    return res;
  }

  @Override
  public Coding translate(Object appInfo, Coding source, String conceptMapUrl) throws FHIRException {
    throw new Error("translate not done yet");
  }

  @Override
  public Base resolveReference(Object appContext, String url) {
    return null;
  }

  @Override
  public List<Base> performSearch(Object appContext, String url) throws FHIRException {
    return null;
  }

}
