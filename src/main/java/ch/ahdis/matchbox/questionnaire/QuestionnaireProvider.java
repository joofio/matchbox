package ch.ahdis.matchbox.questionnaire;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureMap.StructureMapStructureComponent;
import org.hl7.fhir.r5.utils.structuremap.StructureMapUtilities;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ch.ahdis.matchbox.provider.SimpleWorkerContextProvider;

public class QuestionnaireProvider extends SimpleWorkerContextProvider<Questionnaire> implements IResourceProvider {

	public final static String LAUNCH_CONTEXT = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-launchContext";
	public final static String SOURCE_QUERIES = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-sourceQueries";
	public final static String SOURCE_STRUCTURE_MAP = "http://hl7.org/fhir/StructureDefinition/questionnaire-sourceStructureMap";
	
	
		
	public QuestionnaireProvider(SimpleWorkerContext fhirContext) {
		super(fhirContext, Questionnaire.class);
	}
	
	@Operation(name = "$populate", manualResponse = true,idempotent = true)
	public void extract( 
			@OperationParam(name = "questionnaire", min = 1, max = 1) Questionnaire questionnaire,
			@OperationParam(name = "subject", min = 1, max = 1, type=Reference.class) Reference ref,
			
			@OperationParam(name = "identifier", min=0, max=1) Identifier identifier,
			@OperationParam(name = "canonical", min=0, max=1) String canonical,
			@OperationParam(name = "subject", min=0, max=1) Reference subject,
			@OperationParam(name = "content", min=0 ) List<Reference> content,
			@OperationParam(name = "local", min=0, max=1 ) BooleanType local,
			
			HttpServletRequest  theServletRequest, HttpServletResponse theServletResponse)
	      throws IOException {
		System.out.println(" subj="+ref);
		System.out.println(" quest="+questionnaire);
		System.out.println(" questID="+questionnaire.getId());
		System.out.println(" content="+content);
		System.out.println(" content size="+content.size());
		
		StructureMapUtilities utils = new StructureMapUtilities(fhirContext, new TransformSupportServices(fhirContext, new ArrayList<Base>()));
		 String contentType = theServletRequest.getContentType();

		    Set<String> highestRankedAcceptValues = RestfulServerUtils
		        .parseAcceptHeaderAndReturnHighestRankedOptions(theServletRequest);

		    String responseContentType = Constants.CT_FHIR_XML_NEW;
		    if (highestRankedAcceptValues.contains(Constants.CT_FHIR_JSON_NEW)) {
		      responseContentType = Constants.CT_FHIR_JSON_NEW;
		    }
		   		    	
		    
		    String questionnaireStr = FhirContext.forR4().newJsonParser().encodeResourceToString(questionnaire);
		      org.hl7.fhir.r5.elementmodel.Element src = Manager.parse(fhirContext, new ByteArrayInputStream(questionnaireStr.getBytes()), FhirFormat.JSON);
		      
		      org.hl7.fhir.r5.elementmodel.Element launchContext = src.getExtension(LAUNCH_CONTEXT);
		      if (launchContext == null) throw new UnprocessableEntityException("No sdc-questionnaire-launchContext extension found in resource");
		       		      
		      Base sourceQueriesValue = src.getExtensionValue(SOURCE_QUERIES);
		      if (sourceQueriesValue == null) throw new UnprocessableEntityException("No sdc-questionnaire-sourceQueries extension found in resource");
		      String sourceQueriesUrl = sourceQueriesValue.primitiveValue();
		      
		      
		      Base mapUrlValue = src.getExtensionValue(SOURCE_STRUCTURE_MAP);
		      if (mapUrlValue == null) throw new UnprocessableEntityException("No sdc-questionnaire-sourceStructureMap extension found in resource");
		      String mapUrl = mapUrlValue.primitiveValue();
		      
		      
		      org.hl7.fhir.r5.model.StructureMap map = fhirContext.getTransform(mapUrl);
		      if (map == null) {
		          throw new UnprocessableEntityException("Map not available with canonical url "+mapUrl);
		      }

		      org.hl7.fhir.r5.elementmodel.Element r = getTargetResourceFromStructureMap(map);
		      if (r == null) {
		        throw new UnprocessableEntityException("Target Structure can not be resolved from map, is the corresponding implmentation guide provided?");
		      }
		      
		      utils.transform(null, src, map, r);
		      /*if (r.isResource() && "Bundle".contentEquals(r.getType())) {
		        Property bundleType = r.getChildByName("type");
		        if (bundleType!=null && bundleType.getValues()!=null && "document".equals(bundleType.getValues().get(0).primitiveValue())) {
		          removeBundleEntryIds(r);
		        }
		      }*/
		      theServletResponse.setContentType(responseContentType);
		      theServletResponse.setCharacterEncoding("UTF-8");
		      ServletOutputStream output = theServletResponse.getOutputStream();

		      if (output != null) {
		        if (output != null && responseContentType.equals(Constants.CT_FHIR_JSON_NEW))
		          new org.hl7.fhir.r5.elementmodel.JsonParser(fhirContext).compose(r, output, OutputStyle.PRETTY, null);
		        else
		          new org.hl7.fhir.r5.elementmodel.XmlParser(fhirContext).compose(r, output, OutputStyle.PRETTY, null);
		      }
		      theServletResponse.getOutputStream().close();
		    
		
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

	
}
