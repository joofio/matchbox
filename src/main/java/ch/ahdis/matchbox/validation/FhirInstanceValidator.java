package ch.ahdis.matchbox.validation;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.model.FhirPublication;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.utils.IResourceValidator.BestPracticeWarningLevel;
import org.hl7.fhir.r5.utils.IResourceValidator.IdStatus;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.InstanceValidatorFactory;
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
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.validation.IInstanceValidatorModule;
import ca.uhn.fhir.validation.IValidationContext;
import ca.uhn.fhir.validation.ValidationOptions;
import ch.ahdis.matchbox.mappinglanguage.ImplementationGuideProvider;

public class FhirInstanceValidator extends BaseValidatorBridge implements IInstanceValidatorModule, PropertyChangeListener{

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirInstanceValidator.class);

	private boolean myAnyExtensionsAllowed = true;
	private BestPracticeWarningLevel myBestPracticeWarningLevel;
	private DocumentBuilderFactory myDocBuilderFactory;
	private boolean myNoTerminologyChecks;
	private StructureDefinition myStructureDefintion;
	private List<String> extensionDomains = Collections.emptyList();

	private ValidationEngine validationEngine = null;
	
	/**
	 * Constructor
	 * <p>
	 * Uses {@link DefaultProfileValidationSupport} for {@link IValidationSupport
	 * validation support}
	 */
	public FhirInstanceValidator(IValidationSupport theValidationSupport) {
    myDocBuilderFactory = DocumentBuilderFactory.newInstance();
    myDocBuilderFactory.setNamespaceAware(true);
		if (validationEngine == null) {
			try {
				ourLog.info("Setting up validation engine");
        ourLog.info("Loading hl7.fhir.r4.core done");
				validationEngine = new ValidationEngine("hl7.fhir.r4.core", null, null, FhirPublication.R4);
				ourLog.info("Loading hl7.fhir.r4.core done");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			validationEngine.getContext().setCanRunWithoutTerminology(true);
			// FIXME is this correct? otherwise there is a NP in conformsTo function
			if (validationEngine.getContext().getValidatorFactory() == null) {
			  validationEngine.getContext().setValidatorFactory(new InstanceValidatorFactory());
			}
		}
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

	public StructureDefinition findStructureDefinitionForResourceName(String resourceName) {
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
					: this.validationEngine.getContext().getStructure(sdName);
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		return profile;
	}
	
	public SimpleWorkerContext getContext() {
	  return this.validationEngine.getContext();
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
	


	public synchronized List<ValidationMessage> validate(String theInput, EncodingEnum theEncoding, ValidationOptions options) {
	  
	  String forceProfile = null;
	  if (options!=null && options.getProfiles()!=null && options.getProfiles().size()>0) {
	    forceProfile = options.getProfiles().iterator().next();
	  }

		InstanceValidator v;
		try {
			v = new InstanceValidator(validationEngine.getContext(), null);
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
			  if (forceProfile!=null) {
	        resourceNames.add(forceProfile);
			  } else {
			    resourceNames.add(determineResourceName(document));
			  }
			} else {
        if (forceProfile!=null) {
          resourceNames.add(forceProfile);
        }			  
			}

			for (String resourceName : resourceNames) {
				StructureDefinition profile = findStructureDefinitionForResourceName(resourceName);
				if (profile != null) {
					try {
	          ValidationMessage m = new ValidationMessage();
	          m.setLevel(IssueSeverity.INFORMATION);
	          m.setMessage(theEncoding.getFormatContentType()+": "+profile.getName()+" version: " + profile.getVersion()+ " url: " + profile.getUrl());
	          messages.add(m);
						v.validate(null, messages, document, profile.getUrl());
					} catch (Exception e) {
						ourLog.error("Failure during validation", e);
						throw new InternalErrorException("Unexpected failure while validating resource", e);
					}
				} else {
	        ValidationMessage m = new ValidationMessage();
	        m.setLevel(IssueSeverity.ERROR);
	        m.setMessage("Profile could not be resolved to validate: " + resourceName);
	        messages.add(m);
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
        if (forceProfile!=null) {
          resourceNames.add(forceProfile);
        }       				
			} catch (Exception e) {
        if (forceProfile!=null) {
          resourceNames.add(forceProfile);
        } else {               
          resourceNames.add(json.get("resourceType").getAsString());
        }
			}

			for (String resourceName : resourceNames) {
				StructureDefinition profile = findStructureDefinitionForResourceName(resourceName);				
				if (profile != null) {
        try {
            ValidationMessage m = new ValidationMessage();
            m.setLevel(IssueSeverity.INFORMATION);
            m.setMessage(theEncoding.getFormatContentType()+": "+profile.getName()+" version: " + profile.getVersion()+ " url: " + profile.getUrl());
            messages.add(m);
            v.validate(null, messages, json, profile.getUrl());
          } catch (Exception e) {
            ourLog.error("Failure during validation", e);
            throw new InternalErrorException("Unexpected failure while validating resource", e);
          }
        } else {
          ValidationMessage m = new ValidationMessage();
          m.setLevel(IssueSeverity.ERROR);
          m.setMessage("Profile could not be resolved to validate: " + resourceName);
          messages.add(m);
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
		return validate(theCtx.getResourceAsString(), theCtx.getResourceAsStringEncoding(), theCtx.getOptions());
	}

  @Override
  public synchronized void propertyChange(PropertyChangeEvent evt)  {
    if (ImplementationGuideProvider.IG_LOAD.equals(evt.getPropertyName())) {
      String ig = (String) evt.getNewValue();
      try {
        ourLog.info("Loading "+ ig);
        validationEngine.loadIg(ig, true);
        ourLog.info("Loading "+ ig+" done");
        
      } catch (Exception e1) {
        ourLog.error("Error loading implemenation guide " + ig);
        e1.printStackTrace();
      }
    }
  }

}
