package ch.ahdis.matchbox.validation;
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
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.Validate;
import org.hl7.fhir.convertors.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.PathEngineException;
import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r5.model.FhirPublication;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.TypeDetails;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.utils.FHIRPathEngine.IEvaluationContext;
import org.hl7.fhir.r5.utils.IResourceValidator.BestPracticeWarningLevel;
import org.hl7.fhir.r5.utils.IResourceValidator.IdStatus;
import org.hl7.fhir.r5.validation.InstanceValidator;
import org.hl7.fhir.r5.validation.ValidationEngine;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.validation.IValidationContext;
import ca.uhn.fhir.validation.IValidatorModule;

public class FhirInstanceValidator extends BaseValidatorBridge implements IValidatorModule {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirInstanceValidator.class);

	private boolean myAnyExtensionsAllowed = true;
	private BestPracticeWarningLevel myBestPracticeWarningLevel;
	private DocumentBuilderFactory myDocBuilderFactory;
	private boolean myNoTerminologyChecks;
	private StructureDefinition myStructureDefintion;
	private List<String> extensionDomains = Collections.emptyList();

	private IValidationSupport myValidationSupport;
	private ValidationEngine validationEngine = null;

	/**
	 * Constructor
	 * <p>
	 * Uses {@link DefaultProfileValidationSupport} for {@link IValidationSupport
	 * validation support}
	 */
	public FhirInstanceValidator() {
		this(new DefaultProfileValidationSupport());
		if (validationEngine == null) {
			try {
				ourLog.info("Setting up validation engine");
				validationEngine = new ValidationEngine("hl7.fhir.core#4.0.0", null, null, FhirPublication.R4);
				ourLog.info("Loading ch-core");
				validationEngine.loadIg("ch.fhir.ig.core#dev");
				ourLog.info("Loading ch-core done");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			validationEngine.getContext().setCanRunWithoutTerminology(true);
		}
	}

	/**
	 * Constructor which uses the given validation support
	 *
	 * @param theValidationSupport The validation support
	 */
	public FhirInstanceValidator(IValidationSupport theValidationSupport) {
		myDocBuilderFactory = DocumentBuilderFactory.newInstance();
		myDocBuilderFactory.setNamespaceAware(true);
		myValidationSupport = theValidationSupport;
	}

	/**
	 * Every element in a resource or data type includes an optional
	 * <it>extension</it> child element which is identified by it's
	 * {@code url attribute}. There exists a number of predefined extension urls or
	 * extension domains:
	 * <ul>
	 * <li>any url which contains {@code example.org}, {@code nema.org}, or
	 * {@code acme.com}.</li>
	 * <li>any url which starts with
	 * {@code http://hl7.org/fhir/StructureDefinition/}.</li>
	 * </ul>
	 * It is possible to extend this list of known extension by defining custom
	 * extensions: Any url which starts which one of the elements in the list of
	 * custom extension domains is considered as known.
	 * <p>
	 * Any unknown extension domain will result in an information message when
	 * validating a resource.
	 * </p>
	 */
	public FhirInstanceValidator setCustomExtensionDomains(List<String> extensionDomains) {
		this.extensionDomains = extensionDomains;
		return this;
	}

	/**
	 * Every element in a resource or data type includes an optional
	 * <it>extension</it> child element which is identified by it's
	 * {@code url attribute}. There exists a number of predefined extension urls or
	 * extension domains:
	 * <ul>
	 * <li>any url which contains {@code example.org}, {@code nema.org}, or
	 * {@code acme.com}.</li>
	 * <li>any url which starts with
	 * {@code http://hl7.org/fhir/StructureDefinition/}.</li>
	 * </ul>
	 * It is possible to extend this list of known extension by defining custom
	 * extensions: Any url which starts which one of the elements in the list of
	 * custom extension domains is considered as known.
	 * <p>
	 * Any unknown extension domain will result in an information message when
	 * validating a resource.
	 * </p>
	 */
	public FhirInstanceValidator setCustomExtensionDomains(String... extensionDomains) {
		this.extensionDomains = Arrays.asList(extensionDomains);
		return this;
	}

	private String determineResourceName(Document theDocument) {
		NodeList list = theDocument.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i) instanceof Element) {
				return list.item(i).getLocalName();
			}
		}
		return theDocument.getDocumentElement().getLocalName();
	}

	private ArrayList<String> determineIfProfilesSpecified(Document theDocument) {
		ArrayList<String> profileNames = new ArrayList<String>();
		NodeList list = theDocument.getChildNodes().item(0).getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeName().compareToIgnoreCase("meta") == 0) {
				NodeList metaList = list.item(i).getChildNodes();
				for (int j = 0; j < metaList.getLength(); j++) {
					if (metaList.item(j).getNodeName().compareToIgnoreCase("profile") == 0) {
						profileNames.add(metaList.item(j).getAttributes().item(0).getNodeValue());
					}
				}
				break;
			}
		}
		return profileNames;
	}

	private StructureDefinition findStructureDefinitionForResourceName(final FhirContext theCtx, String resourceName) {
		String sdName = null;
		try {
			// Test if a URL was passed in specifying the structure definition and test if
			// "StructureDefinition" is part of the URL
			URL testIfUrl = new URL(resourceName);
			sdName = resourceName;
		} catch (MalformedURLException e) {
			sdName = "http://hl7.org/fhir/StructureDefinition/" + resourceName;
		}
		StructureDefinition profile = null;
		try {
			profile = myStructureDefintion != null ? myStructureDefintion
					: (StructureDefinition) VersionConvertor_40_50
							.convertResource(myValidationSupport.fetchStructureDefinition(theCtx, sdName));
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		return profile;
	}

	/**
	 * Returns the "best practice" warning level (default is
	 * {@link BestPracticeWarningLevel#Hint}).
	 * <p>
	 * The FHIR Instance Validator has a number of checks for best practices in
	 * terms of FHIR usage. If this setting is set to
	 * {@link BestPracticeWarningLevel#Error}, any resource data which does not meet
	 * these best practices will be reported at the ERROR level. If this setting is
	 * set to {@link BestPracticeWarningLevel#Ignore}, best practice guielines will
	 * be ignored.
	 * </p>
	 * 
	 * @see #setBestPracticeWarningLevel(BestPracticeWarningLevel)
	 */
	public BestPracticeWarningLevel getBestPracticeWarningLevel() {
		return myBestPracticeWarningLevel;
	}

	/**
	 * Sets the "best practice warning level". When validating, any deviations from
	 * best practices will be reported at this level.
	 * <p>
	 * The FHIR Instance Validator has a number of checks for best practices in
	 * terms of FHIR usage. If this setting is set to
	 * {@link BestPracticeWarningLevel#Error}, any resource data which does not meet
	 * these best practices will be reported at the ERROR level. If this setting is
	 * set to {@link BestPracticeWarningLevel#Ignore}, best practice guielines will
	 * be ignored.
	 * </p>
	 *
	 * @param theBestPracticeWarningLevel The level, must not be <code>null</code>
	 */
	public void setBestPracticeWarningLevel(BestPracticeWarningLevel theBestPracticeWarningLevel) {
		Validate.notNull(theBestPracticeWarningLevel);
		myBestPracticeWarningLevel = theBestPracticeWarningLevel;
	}

	/**
	 * Returns the {@link IValidationSupport validation support} in use by this
	 * validator. Default is an instance of {@link DefaultProfileValidationSupport}
	 * if the no-arguments constructor for this object was used.
	 */
	public IValidationSupport getValidationSupport() {
		return myValidationSupport;
	}

	/**
	 * Sets the {@link IValidationSupport validation support} in use by this
	 * validator. Default is an instance of {@link DefaultProfileValidationSupport}
	 * if the no-arguments constructor for this object was used.
	 */
	public void setValidationSupport(IValidationSupport theValidationSupport) {
		myValidationSupport = theValidationSupport;
	}

	/**
	 * If set to {@literal true} (default is true) extensions which are not known to
	 * the validator (e.g. because they have not been explicitly declared in a
	 * profile) will be validated but will not cause an error.
	 */
	public boolean isAnyExtensionsAllowed() {
		return myAnyExtensionsAllowed;
	}

	/**
	 * If set to {@literal true} (default is true) extensions which are not known to
	 * the validator (e.g. because they have not been explicitly declared in a
	 * profile) will be validated but will not cause an error.
	 */
	public void setAnyExtensionsAllowed(boolean theAnyExtensionsAllowed) {
		myAnyExtensionsAllowed = theAnyExtensionsAllowed;
	}

	/**
	 * If set to {@literal true} (default is false) the valueSet will not be
	 * validate
	 */
	public boolean isNoTerminologyChecks() {
		return myNoTerminologyChecks;
	}

	/**
	 * If set to {@literal true} (default is false) the valueSet will not be
	 * validate
	 */
	public void setNoTerminologyChecks(final boolean theNoTerminologyChecks) {
		myNoTerminologyChecks = theNoTerminologyChecks;
	}

	public void setStructureDefintion(StructureDefinition theStructureDefintion) {
		myStructureDefintion = theStructureDefintion;
	}

	protected List<ValidationMessage> validate(final FhirContext theCtx, String theInput, EncodingEnum theEncoding) {

		InstanceValidator v;
		IEvaluationContext evaluationCtx = new NullEvaluationContext();
		try {
			v = new InstanceValidator(validationEngine.getContext(), evaluationCtx);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}

		v.setBestPracticeWarningLevel(getBestPracticeWarningLevel());
		v.setAnyExtensionsAllowed(isAnyExtensionsAllowed());
		v.setResourceIdRule(IdStatus.OPTIONAL);
		v.setNoTerminologyChecks(isNoTerminologyChecks());
		v.getExtensionDomains().addAll(extensionDomains);

		List<ValidationMessage> messages = new ArrayList<>();

		if (theEncoding == EncodingEnum.XML) {
			Document document;
			try {
				DocumentBuilder builder = myDocBuilderFactory.newDocumentBuilder();
				InputSource src = new InputSource(new StringReader(theInput));
				document = builder.parse(src);
			} catch (Exception e2) {
				ourLog.error("Failure to parse XML input", e2);
				ValidationMessage m = new ValidationMessage();
				m.setLevel(IssueSeverity.FATAL);
				m.setMessage("Failed to parse input, it does not appear to be valid XML:" + e2.getMessage());
				return Collections.singletonList(m);
			}

			// Determine if meta/profiles are present...
			ArrayList<String> resourceNames = determineIfProfilesSpecified(document);
			if (resourceNames.isEmpty()) {
				resourceNames.add(determineResourceName(document));
			}

			for (String resourceName : resourceNames) {
				StructureDefinition profile = findStructureDefinitionForResourceName(theCtx, resourceName);
				if (profile != null) {
					try {
						v.validate(null, messages, document, profile.getUrl());
					} catch (Exception e) {
						ourLog.error("Failure during validation", e);
						throw new InternalErrorException("Unexpected failure while validating resource", e);
					}
				} else {
					profile = findStructureDefinitionForResourceName(theCtx, determineResourceName(document));
					if (profile != null) {
						try {
							v.validate(null, messages, document, profile.getUrl());
						} catch (Exception e) {
							ourLog.error("Failure during validation", e);
							throw new InternalErrorException("Unexpected failure while validating resource", e);
						}
					}
				}
			}
		} else if (theEncoding == EncodingEnum.JSON) {
			Gson gson = new GsonBuilder().create();
			JsonObject json = gson.fromJson(theInput, JsonObject.class);

			ArrayList<String> resourceNames = new ArrayList<String>();
			JsonArray profiles = null;
			try {
				profiles = json.getAsJsonObject("meta").getAsJsonArray("profile");
				for (JsonElement element : profiles) {
					resourceNames.add(element.getAsString());
				}
			} catch (Exception e) {
				resourceNames.add(json.get("resourceType").getAsString());
			}

			for (String resourceName : resourceNames) {
				StructureDefinition profile = findStructureDefinitionForResourceName(theCtx, resourceName);
				if (profile != null) {
					try {
						v.validate(null, messages, json, profile.getUrl());
					} catch (Exception e) {
						throw new InternalErrorException("Unexpected failure while validating resource", e);
					}
				} else {
					profile = findStructureDefinitionForResourceName(theCtx, json.get("resourceType").getAsString());
					if (profile != null) {
						try {
							v.validate(null, messages, json, profile.getUrl());
						} catch (Exception e) {
							ourLog.error("Failure during validation", e);
							throw new InternalErrorException("Unexpected failure while validating resource", e);
						}
					}
				}
			}
		} else {
			throw new IllegalArgumentException("Unknown encoding: " + theEncoding);
		}

		for (int i = 0; i < messages.size(); i++) {
			ValidationMessage next = messages.get(i);
			if ("Binding has no source, so can't be checked".equals(next.getMessage())) {
				messages.remove(i);
				i--;
			}
		}
		return messages;
	}

	@Override
	protected List<ValidationMessage> validate(IValidationContext<?> theCtx) {
		return validate(theCtx.getFhirContext(), theCtx.getResourceAsString(), theCtx.getResourceAsStringEncoding());
	}

	public static class NullEvaluationContext implements IEvaluationContext {

		@Override
		public org.hl7.fhir.r5.model.Base resolveConstant(Object appContext, String name, boolean beforeContext)
				throws PathEngineException {
			return null;
		}

		@Override
		public TypeDetails resolveConstantType(Object appContext, String name) throws PathEngineException {
			return null;
		}

		@Override
		public boolean log(String argument, List<org.hl7.fhir.r5.model.Base> focus) {
			return false;
		}

		@Override
		public FunctionDetails resolveFunction(String functionName) {
			return null;
		}

		@Override
		public TypeDetails checkFunction(Object appContext, String functionName, List<TypeDetails> parameters)
				throws PathEngineException {
			return null;
		}

		@Override
		public List<org.hl7.fhir.r5.model.Base> executeFunction(Object appContext, String functionName,
				List<List<org.hl7.fhir.r5.model.Base>> parameters) {
			return null;
		}

		@Override
		public org.hl7.fhir.r5.model.Base resolveReference(Object appContext, String url) throws FHIRException {
			return null;
		}

		@Override
		public boolean conformsToProfile(Object appContext, org.hl7.fhir.r5.model.Base item, String url)
				throws FHIRException {
			return false;
		}

		@Override
		public ValueSet resolveValueSet(Object appContext, String url) {
			return null;
		}

	}

}
