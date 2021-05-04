package ch.ahdis.matchbox.questionnaire;

import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.terminologies.ConceptMapEngine;
import org.hl7.fhir.r5.utils.structuremap.ITransformerServices;


public class TransformSupportServices implements ITransformerServices {

    private List<Base> outputs;
    private SimpleWorkerContext fhirContext;

        
        public TransformSupportServices(SimpleWorkerContext fhirContext, List<Base> outputs) {
        	this.fhirContext = fhirContext;
            this.outputs = outputs;
        }

        @Override
        public Base createType(Object appInfo, String name) throws FHIRException {
          StructureDefinition sd = fhirContext.fetchResource(StructureDefinition.class, name);
          return Manager.build(fhirContext, sd); 
        }

        @Override
        public Base createResource(Object appInfo, Base res, boolean atRootofTransform) {
          if (atRootofTransform)
            outputs.add(res);
          return res;
        }

        @Override
        public Coding translate(Object appInfo, Coding source, String conceptMapUrl) throws FHIRException {
          ConceptMapEngine cme = new ConceptMapEngine(fhirContext);
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
          //StructureMapTransformProvider.log.debug(message);
        }

    }