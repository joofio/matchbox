package ch.ahdis.matchbox.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * #%L
 * Matchbox Server
 * %%
 * Copyright (C) 2018 - 2020 ahdis
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

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.util.StopWatch;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationOptions;

/**
 * Operation $validate
 */
public class ValidationProvider {

  
//  @Autowired
//  protected IValidationSupport myValidationSupport;
//
  protected FhirContext myFhirCtx;
//  
//  @Autowired
//  protected DefaultProfileValidationSupport defaultProfileValidationSuport;
  
  protected FhirInstanceValidator instanceValidator;

  public ValidationProvider(FhirInstanceValidator instanceValidator, FhirContext myFhirCtx) {
    this.instanceValidator = instanceValidator;
    this.myFhirCtx = myFhirCtx;
  }

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ValidationProvider.class);

  @Operation(name = "$validate", manualRequest=true, idempotent = true, returnParameters = {
      @OperationParam(name = "return", type = IBase.class, min = 1, max = 1) })
  public IBaseResource validate(HttpServletRequest theRequest) {
    log.debug("$validate");
    
    StopWatch sw = new StopWatch();
    sw.startTask("Total");

    String profile = null;

    ValidationOptions validationOptions = new ValidationOptions();
    if (theRequest.getParameter("profile")!=null) {
      // oe: @OperationParam(name = "profile" is not working in 5.1.0 JPA (was working before with 5.0.o Plain server)
      profile= theRequest.getParameter("profile");
    }
    
    if (profile != null) {
      if (instanceValidator.findStructureDefinitionForResourceName(profile)==null) {
        SingleValidationMessage m = new SingleValidationMessage();
        m.setSeverity(ResultSeverityEnum.ERROR);
        m.setMessage("Validation for profile "+ profile + " not supported by this server, but additional ig's could be configured.");
        m.setLocationCol(0);
        m.setLocationLine(0);
        ArrayList<SingleValidationMessage> addedValidationMessages = new ArrayList<>();
        addedValidationMessages.add(m);
        return (new ValidationResult(myFhirCtx, addedValidationMessages)).toOperationOutcome();
      }
      validationOptions.addProfileIfNotBlank(profile);
    }
    
    byte[] bytes = null;
    String contentString = "";
    try {
      bytes = IOUtils.toByteArray(theRequest.getInputStream());
      contentString = new String(bytes);
    } catch (IOException e) {
    }
    // ValidationResult result = 
    
    EncodingEnum encoding = EncodingEnum.forContentType(theRequest.getContentType());
    if (encoding == null) {
      encoding = EncodingEnum.detectEncoding(contentString);
    }
    List<ValidationMessage> messages = instanceValidator.validate(contentString, encoding,validationOptions);

    ArrayList<SingleValidationMessage> addedValidationMessages = new ArrayList<>();
    for (ValidationMessage riMessage : messages) {
      SingleValidationMessage hapiMessage = new SingleValidationMessage();
      if (riMessage.getCol() != -1) {
        hapiMessage.setLocationCol(riMessage.getCol());
      }
      if (riMessage.getLine() != -1) {
        hapiMessage.setLocationLine(riMessage.getLine());
      }
      hapiMessage.setLocationString(riMessage.getLocation());
      hapiMessage.setMessage(riMessage.getMessage());
      if (riMessage.getLevel() != null) {
        hapiMessage.setSeverity(ResultSeverityEnum.fromCode(riMessage.getLevel().toCode()));
      }
      addedValidationMessages.add(hapiMessage);
    }
    sw.endCurrentTask();

    if (profile != null) {
      SingleValidationMessage m = new SingleValidationMessage();
      m.setSeverity(ResultSeverityEnum.INFORMATION);
      m.setMessage("Validation for profile "+ profile +" " + (addedValidationMessages.size()==0 ? "No Issues detected. " : "") + sw.formatTaskDurations());
      m.setLocationCol(0);
      m.setLocationLine(0);
      addedValidationMessages.add(m);
    }

    return  new ValidationResult(myFhirCtx, addedValidationMessages).toOperationOutcome();
  }

}
