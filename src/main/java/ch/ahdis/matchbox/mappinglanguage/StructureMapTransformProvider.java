package ch.ahdis.matchbox.mappinglanguage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
/*
 * #%L
 * Matchbox Server
 * %%
 * Copyright (C) 2018 - 2019 ahdis
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.convertors.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureMap.StructureMapStructureComponent;
import org.hl7.fhir.r5.terminologies.ConceptMapEngine;
import org.hl7.fhir.r5.utils.StructureMapUtilities;
import org.hl7.fhir.r5.utils.StructureMapUtilities.ITransformerServices;
import org.hl7.fhir.utilities.cache.PackageCacheManager;
import org.hl7.fhir.utilities.cache.ToolsVersion;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;

public class StructureMapTransformProvider extends ca.uhn.fhir.jpa.rp.r4.StructureMapResourceProvider {
  
  public class TransformSupportServices implements ITransformerServices {

    private List<Base> outputs;

    public TransformSupportServices(List<Base> outputs) {
      this.outputs = outputs;
    }

    @Override
    public Base createType(Object appInfo, String name) throws FHIRException {
      StructureDefinition sd = workerContext.fetchResource(StructureDefinition.class, name);
      return Manager.build(workerContext, sd); 
    }

    @Override
    public Base createResource(Object appInfo, Base res, boolean atRootofTransform) {
      if (atRootofTransform)
        outputs.add(res);
      return res;
    }

    @Override
    public Coding translate(Object appInfo, Coding source, String conceptMapUrl) throws FHIRException {
      ConceptMapEngine cme = new ConceptMapEngine(workerContext);
      return cme.translate(source, conceptMapUrl);
    }

    @Override
    public Base resolveReference(Object appContext, String url) throws FHIRException {
      throw new FHIRException("resolveReference is not supported yet");
    }

    @Override
    public List<Base> performSearch(Object appContext, String url) throws FHIRException {
      throw new FHIRException("performSearch is not supported yet");
    }


    @Override
    public void log(String message) {
      StructureMapTransformProvider.log.debug(message);
    }

  }

  @Override
  public MethodOutcome create(HttpServletRequest theRequest, StructureMap theResource, String theConditional,
      RequestDetails theRequestDetails) {
    MethodOutcome outcome = super.create(theRequest, theResource, theConditional, theRequestDetails);
    if (outcome.getCreated()) {
      log.debug("created structuredmap, caching");
      init();
      updateWorkerContext(theResource);
    }
    return outcome;
  }

  public void updateWorkerContext(StructureMap theResource) {
    org.hl7.fhir.r5.model.StructureMap cached = workerContext.fetchResource(org.hl7.fhir.r5.model.StructureMap.class, theResource.getUrl());
    if (cached != null) {
      workerContext.dropResource(cached);
    }    
    workerContext.cacheResource(VersionConvertor_40_50.convertResource(theResource));
  }

  @Override
  public MethodOutcome delete(HttpServletRequest theRequest, IdType theResource, String theConditional,
      RequestDetails theRequestDetails) {
    MethodOutcome outcome = super.delete(theRequest, theResource, theConditional, theRequestDetails);
    init();
    workerContext.dropResource(theResource.getResourceType(), theResource.getId());
    return outcome;
  }

  @Override
  public MethodOutcome update(HttpServletRequest theRequest, StructureMap theResource, IdType theId,
      String theConditional, RequestDetails theRequestDetails) {
    init();
    updateWorkerContext(theResource);
    return super.update(theRequest, theResource, theId, theConditional, theRequestDetails);
  }

  static private SimpleWorkerContext workerContext = null;
  private StructureMapUtilities utils = null;

  protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureMapTransformProvider.class);

  private void init() {
    if (workerContext == null) {
      try {
        PackageCacheManager pcm = new PackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
        log.debug("loading hl7.fhir.r4.core");
        workerContext = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"));
        log.debug("loading hl7.fhir.cda");
        workerContext.loadFromPackage(pcm.loadPackage("hl7.fhir.cda", "dev"), null);
        workerContext.setCanRunWithoutTerminology(true);
        log.debug("loading done");
      } catch (FHIRException | IOException e) {
      }
      List<Base> outputs = new ArrayList<Base>();
      utils = new StructureMapUtilities(workerContext, new TransformSupportServices(outputs));
    }
  }

  @Override
  public StructureMap read(HttpServletRequest theRequest, IIdType theId, RequestDetails theRequestDetails) {
    StructureMap structureMap = super.read(theRequest, theId, theRequestDetails);
    return structureMap;
  }

  @Operation(name = "$transform", manualResponse = true, manualRequest = true)
  public void manualInputAndOutput(HttpServletRequest theServletRequest, HttpServletResponse theServletResponse)
      throws IOException {
    String contentType = theServletRequest.getContentType();

    Set<String> highestRankedAcceptValues = RestfulServerUtils
        .parseAcceptHeaderAndReturnHighestRankedOptions(theServletRequest);

    String responseContentType = Constants.CT_FHIR_XML_NEW;
    if (highestRankedAcceptValues.contains(Constants.CT_FHIR_JSON_NEW)) {
      responseContentType = Constants.CT_FHIR_JSON_NEW;
    }

    Map<String, String[]> requestParams = theServletRequest.getParameterMap();
    String[] source = requestParams.get("source");
    if (source != null && source.length > 0) {
      org.hl7.fhir.r5.elementmodel.Element src = Manager.parse(workerContext, theServletRequest.getInputStream(),
          contentType.contains("xml") ? FhirFormat.XML : FhirFormat.JSON);
      
      
      org.hl7.fhir.r5.model.StructureMap map = workerContext.getTransform(source[0]);

      org.hl7.fhir.r5.elementmodel.Element r = getTargetResourceFromStructureMap(map);
      utils.transform(null, src, map, r);

      ServletOutputStream output = theServletResponse.getOutputStream();

      theServletResponse.setContentType(contentType);
      if (output != null) {
        if (output != null && responseContentType.equals(Constants.CT_FHIR_JSON_NEW))
          new org.hl7.fhir.r5.elementmodel.JsonParser(workerContext).compose(r, output, OutputStyle.PRETTY, null);
        else
          new org.hl7.fhir.r5.elementmodel.XmlParser(workerContext).compose(r, output, OutputStyle.PRETTY, null);
      }
      theServletResponse.getOutputStream().close();
    }

  }

  private org.hl7.fhir.r5.elementmodel.Element getTargetResourceFromStructureMap(org.hl7.fhir.r5.model.StructureMap map) {
    String targetTypeUrl = null;
    for (StructureMapStructureComponent component : map.getStructure()) {
      if (component.getMode() == org.hl7.fhir.r5.model.StructureMap.StructureMapModelMode.TARGET) {
        targetTypeUrl = component.getUrl();
        break;
      }
    }

    if (targetTypeUrl == null)
      throw new FHIRException("Unable to determine resource URL for target type");

    StructureDefinition structureDefinition = null;
    for (StructureDefinition sd : workerContext.getStructures()) {
      if (sd.getUrl().equalsIgnoreCase(targetTypeUrl)) {
        structureDefinition = sd;
        break;
      }
    }
    if (structureDefinition == null)
      throw new FHIRException("Unable to determine StructureDefinition for target type");

    return Manager.build(workerContext, structureDefinition);
  }

  public org.hl7.fhir.r5.model.StructureMap getMapByUrl(String url) {
    return workerContext.fetchResource(org.hl7.fhir.r5.model.StructureMap.class, url);
  }

}
