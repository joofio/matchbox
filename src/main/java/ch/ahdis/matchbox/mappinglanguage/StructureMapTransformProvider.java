package ch.ahdis.matchbox.mappinglanguage;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceFactory;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.utils.StructureMapUtilities;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.UriParam;

public class StructureMapTransformProvider extends ca.uhn.fhir.jpa.rp.r4.StructureMapResourceProvider {

	@Override
	public MethodOutcome create(HttpServletRequest theRequest, StructureMap theResource, String theConditional,
			RequestDetails theRequestDetails) {
		return super.create(theRequest, theResource, theConditional, theRequestDetails);
	}

	private IWorkerContext workerContext = null;
	private StructureMapUtilities utils = null;

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureMapTransformProvider.class);

	private void init() {
		if (workerContext == null) {
			workerContext = new TransformWorkerContext(getContext(), new DefaultProfileValidationSupport(), this);
			utils = new StructureMapUtilities(workerContext);
		}
	}

	@Override
	public StructureMap read(HttpServletRequest theRequest, IIdType theId, RequestDetails theRequestDetails) {
		StructureMap structureMap = super.read(theRequest, theId, theRequestDetails);
		renderMap(structureMap);
		return structureMap;
	}

	private StructureMap renderMap(StructureMap structureMap) {
		if (structureMap != null && structureMap.getText() != null && !structureMap.getText().hasDiv()) {
			init();
			Narrative narrative = new Narrative();
			narrative.setStatus(NarrativeStatus.GENERATED);
			narrative.setDivAsString("<div><pre>" + StructureMapUtilities.render(structureMap) + "</pre></div>");
			structureMap.setText(narrative);
		}
		return structureMap;
	}

//	@Operation(name = "$parse", idempotent = false, returnParameters = {
//			@OperationParam(name = "return", type = StructureMap.class, min = 1, max = 1) })
//	public IBaseResource parse(@OperationParam(name = "map", min = 1, max = 1) final StringType mapType,
//			@OperationParam(name = "updateOrCreate", min = 0, max = 1) final BooleanType updateOrCreate) {
//		init();
//		String map = mapType.asStringValue();
//		log.debug(map);
//		try {
//			StructureMap structureMap = utils.parse(map, "map");
//			if (updateOrCreate != null && updateOrCreate.booleanValue()) {
//				try {
//					StructureMap oldMap = getMapByUrl(structureMap.getUrl());
//					if (oldMap != null) {
//						getDao().update(structureMap);
//					} else {
//						getDao().create(structureMap);
//					}
//				} catch (FHIRException e) {
//					OperationOutcome error = new OperationOutcome();
//					error.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.PROCESSING).setDiagnostics(
//							"Error creating or updating map: " + structureMap.getUrl() + " " + e.getMessage());
//					this.setContext(null);
//					return error;
//				}
//			}
//			return renderMap(structureMap);
//		} catch (FHIRException e) {
//			OperationOutcome error = new OperationOutcome();
//			error.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.PROCESSING)
//					.setDiagnostics("Error parsing map: " + e.getMessage());
//			this.setContext(null);
//			return error;
//		}
//	}

	@Operation(name = "$transform", idempotent = true, returnParameters = {
			@OperationParam(name = "return", type = IBase.class, min = 1, max = 1) })
	public IBaseResource transform(@OperationParam(name = "source", min = 0, max = 1) final UriType source,
			@OperationParam(name = "content", min = 0, max = 1) final IBaseResource content,
			@OperationParam(name = "map", min = 0, max = 1) final StringType mapType) {
		init();
		
		StructureMap map = null;
		if (mapType!=null) {
			String mapAsString = mapType.asStringValue();
			log.debug(mapAsString);
			try {
				StructureMap structureMap = utils.parse(mapAsString, "map");
				map =  renderMap(structureMap);
			} catch (FHIRException e) {
				OperationOutcome error = new OperationOutcome();
				error.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.PROCESSING)
						.setDiagnostics("Error parsing map: " + e.getMessage());
				this.setContext(null);
				return error;
			}
		} else {
			map = getMapByUrl(source.asStringValue());
		}
		if (map == null) {
			OperationOutcome error = new OperationOutcome();
			error.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.PROCESSING)
					.setDiagnostics("Unable to find map " + source.asStringValue());
			this.setContext(null);
			return error;
		}
		Parameters retVal = new Parameters();
		try {
			String typeName = utils.getTargetType(map).getType();
			Resource target = ResourceFactory.createResource(typeName);
			utils.transform(null, (Resource) content, map, target);
			retVal.addParameter().setName("return").setResource(target);
		} catch (FHIRException e) {
			OperationOutcome error = new OperationOutcome();
			error.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.PROCESSING)
					.setDiagnostics("Error transforming map: " + source.asStringValue() + " " + e.getMessage());
			this.setContext(null);
			return error;
		}

		return retVal;
	}

	public StructureMap getMapByUrl(String url) {
		
/*		SearchParameterMap map = new SearchParameterMap();
		map.add(StructureMap.SP_URL, new UriParam(url));

		ca.uhn.fhir.rest.api.server.IBundleProvider result = getDao().search(map);
		if (result != null && result.size() > 0) {
			if (result.size() > 1) {
				log.error("Multiple (" + result.size() + ") StructureMaps found for: " + url);
			}
			List<IBaseResource> maps = result.getResources(0, 1);
			return (StructureMap) maps.get(0);
		}
		log.error("StructureMap " + url + " not found");
		return null;
		*/
		return workerContext.fetchResource(StructureMap.class, url);
	}

}
