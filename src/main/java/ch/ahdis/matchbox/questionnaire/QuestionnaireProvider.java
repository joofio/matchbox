package ch.ahdis.matchbox.questionnaire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureMap.StructureMapStructureComponent;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.r5.utils.structuremap.StructureMapUtilities;
import org.springframework.beans.factory.annotation.Value;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ch.ahdis.matchbox.provider.SimpleWorkerContextProvider;

public class QuestionnaireProvider extends SimpleWorkerContextProvider<Questionnaire> implements IResourceProvider {

	public final static String LAUNCH_CONTEXT = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-launchContext";
	public final static String SOURCE_QUERIES = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-sourceQueries";
	public final static String SOURCE_STRUCTURE_MAP = "http://hl7.org/fhir/StructureDefinition/questionnaire-sourceStructureMap";
	
	private String baseUrl;
		
	public QuestionnaireProvider(String baseUrl, SimpleWorkerContext fhirContext) {		
		super(fhirContext, Questionnaire.class);
		this.baseUrl = baseUrl;
	}
	
	@Operation(name = "$populate", idempotent = true)
	public QuestionnaireResponse extract( 
			@OperationParam(name = "questionnaire", min = 1, max = 1) Questionnaire questionnaire,
			@OperationParam(name = "subject", min = 1, max = 1, type=Reference.class) Reference ref,
			
			@OperationParam(name = "identifier", min=0, max=1) Identifier identifier,
			@OperationParam(name = "canonical", min=0, max=1) String canonical,
			@OperationParam(name = "subject", min=0, max=1) Reference subject,
			@OperationParam(name = "content", min=0 ) List<Reference> content,
			@OperationParam(name = "local", min=0, max=1 ) BooleanType local)
	      throws IOException {		
			
		  StructureMapUtilities utils = new StructureMapUtilities(fhirContext, new TransformSupportServices(fhirContext, new ArrayList<Base>()));
			   	
		  // convert questionaire to element model		      		    
	      org.hl7.fhir.r5.elementmodel.Element src = convertToElementModel(questionnaire);
	      
	      // get launch context from questionnaire
	      org.hl7.fhir.r5.elementmodel.Element launchContext = src.getExtension(LAUNCH_CONTEXT);
	      if (launchContext == null) throw new UnprocessableEntityException("No sdc-questionnaire-launchContext extension found in resource");
	       		      	         
	      // get source queries from questionnaire
	      Extension sourceQueriesExt = questionnaire.getExtensionByUrl(SOURCE_QUERIES);
	      if (sourceQueriesExt == null) throw new UnprocessableEntityException("No sdc-questionnaire-sourceQueries extension found in resource");
		  Type t = sourceQueriesExt.getValue();
		  if (! (t instanceof Reference)) throw new UnprocessableEntityException("sdc-questionnaire-sourceQueries must have reference");
		  Resource sourceQueriesBundleResource = resolveResource(questionnaire, (Reference) t);
		  if (sourceQueriesBundleResource == null) throw new UnprocessableEntityException("sdc-questionnaire-sourceQueries not resolved");
		  if (! (sourceQueriesBundleResource instanceof Bundle)) throw new UnprocessableEntityException("sdc-questionnaire-sourceQueries is not a bundle");
		  Bundle sourceQueriesBundle = (Bundle) sourceQueriesBundleResource;
	      
		  // get structure map from questionnaire
	      Base mapUrlValue = src.getExtensionValue(SOURCE_STRUCTURE_MAP);
	      if (mapUrlValue == null) throw new UnprocessableEntityException("No sdc-questionnaire-sourceStructureMap extension found in resource");
	      String mapUrl = mapUrlValue.primitiveValue();
	      org.hl7.fhir.r5.model.StructureMap map = fhirContext.getTransform(mapUrl);
	      if (map == null) {
	          throw new UnprocessableEntityException("Map not available with canonical url "+mapUrl);
	      }
	      	      	   
	      
	      // build input bundle for structure map
	      Bundle processBundle = new Bundle();
	      for (Bundle.BundleEntryComponent entry : sourceQueriesBundle.getEntry()) {
	    	  if (entry.hasResource()) {
	    		  if (entry.getResource() instanceof Bundle) {
	    			processBundle.addEntry().setResource(entry.getResource()).setFullUrl(entry.getFullUrl());  
	    		  } else {
	    		    processBundle.addEntry().setResource(wrapIntoBundle(entry.getResource())).setFullUrl(entry.getFullUrl());
	    		  }
	    	  } else if (entry.hasRequest()) {
	    		  if (entry.getRequest().getMethod()!=Bundle.HTTPVerb.GET) throw new UnprocessableEntityException("Bundle request method must be GET");
	    		  String url = entry.getRequest().getUrl();
	    		  url = evaluateFhirPath(subject, url);
	    		  Bundle result = resolveBundleFromUri(url);
	    		  processBundle.addEntry().setResource(result).setFullUrl(entry.getFullUrl());
	    	  }
	      }
	      
	      // convert input bundle to element model
	      StructureDefinition bundleStructure = fhirContext.getStructure("Bundle");		      		      		      
	      org.hl7.fhir.r5.elementmodel.Element bundle = Manager.build(fhirContext, bundleStructure);	      
	      bundle = convertToElementModel(processBundle);	      
	      
	      // build output QuestionnaireResponse using element model
	      org.hl7.fhir.r5.elementmodel.Element r = getTargetResourceFromStructureMap(map);
	      if (r == null) {
	        throw new UnprocessableEntityException("Target Structure can not be resolved from map, is the corresponding implmentation guide provided?");
	      }	      
	      utils.transform(null, bundle, map, r);
	      
	      // convert output to R4 resource
	      IBaseResource result = convertToR4(r);	      
	      if (!(result instanceof QuestionnaireResponse)) throw new UnprocessableEntityException("Structure Map does not produce correct output resource type.");	      
	      QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) result;
	      
	      // add subject and identifier to response if given	      
	      if (subject != null) questionnaireResponse.setSubject(subject);
	      if (identifier != null) questionnaireResponse.setIdentifier(identifier);
	      
	      return questionnaireResponse;
	      		     		
	}
	  
    /**
     * create target resource from structure map
     * @param map
     * @return
     */
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
	    for (StructureDefinition sd : fhirContext.getStructures()) {
	      if (sd.getUrl().equalsIgnoreCase(targetTypeUrl)) {
	        structureDefinition = sd;
	        break;
	      }
	    }
	    if (structureDefinition == null)
	      throw new FHIRException("Unable to determine StructureDefinition for target type");
	
	    return Manager.build(fhirContext, structureDefinition);
	  }
	
	/**
	 * convert R4 resources to element model
	 * @param inputResource
	 * @return
	 */
	private org.hl7.fhir.r5.elementmodel.Element convertToElementModel(Resource inputResource) {
		 String inStr = FhirContext.forR4().newJsonParser().encodeResourceToString(inputResource);
		 try {
	       return Manager.parse(fhirContext, new ByteArrayInputStream(inStr.getBytes()), FhirFormat.JSON);
		 } catch (IOException e) {
			 throw new UnprocessableEntityException("Cannot convert resource to element model");
		 }	 
	}
	
	/**
	 * convert from element model to R4 resource
	 * @param input
	 * @return
	 */
	private IBaseResource convertToR4(org.hl7.fhir.r5.elementmodel.Element input) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
		  new org.hl7.fhir.r5.elementmodel.JsonParser(fhirContext).compose(input, output, OutputStyle.NORMAL, null);
		  return FhirContext.forR4().newJsonParser().parseResource(new String(output.toByteArray()));
		} catch (IOException e) {
			throw new UnprocessableEntityException("Cannot convert to R4");
		}
	}
	
	/**
	 * locally resolve reference
	 * @param container
	 * @param reference
	 * @return
	 */
	private Resource resolveResource(DomainResource container, Reference reference) {
		if (reference.hasReference()) {
			String targetRef = reference.getReference();		
			List<Resource> resources = container.getContained();		
			for (Resource resource : resources) {			
				if (targetRef.equals(resource.getId())) {
					return resource;
				}
			}
		}
		return null;
	}
	
	/**
	 * wrap single resource into bundle
	 * @param resource
	 * @return
	 */
	private Bundle wrapIntoBundle(Resource resource) {
		Bundle result = new Bundle();
		result.setType(BundleType.BATCHRESPONSE);
		result.addEntry().setResource(resource);
		return result;
	}
	
	/**
	 * resolve bundle from uri (search)
	 * @param uri
	 * @return
	 */
	private Bundle resolveBundleFromUri(String uri) {
		if (baseUrl==null) throw new UnprocessableEntityException("missing baseUrl");
		System.out.println("fetch from external: "+uri);
		IGenericClient client = FhirContext.forR4().newRestfulGenericClient(baseUrl);
		Bundle result = client.search()
			.byUrl(uri)
			.returnBundle(Bundle.class)
			.execute();
		System.out.println("retrieved entries: "+result.getEntry().size());
		return result;
	}
	
	/**
	 * replace FHIR path expressions in uri
	 * @param uri
	 * @return
	 */
	private String evaluateFhirPath(Reference subject, String uri) {
		
		while (uri.indexOf("{{") >= 0) {
			int p = uri.indexOf("{{");
			int c = uri.indexOf("}}");
			String expression = uri.substring(p+2,c);
		
			String r = null;
			if (expression.equals("%LaunchPatient.id")) {
				r = subject.getReference();
			} else {
				// TODO what is the rool element?
				System.out.println("IN:"+expression);
				FHIRPathEngine fp = new FHIRPathEngine(fhirContext);			
				r = fp.evaluateToString(null, expression);
				System.out.println("OUT:"+r);
			}
			
			uri = uri.substring(0,p)+r+uri.substring(c+2);
		}
				
		return uri;
	}
	
	//FhirPathR5 fhirPath = new FhirPathR5(FhirContext.forR4());
    //fhirPath.evaluate(theInput, thePath, theReturnType)
}
