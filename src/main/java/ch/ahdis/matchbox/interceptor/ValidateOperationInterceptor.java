package ch.ahdis.matchbox.interceptor;

import static org.apache.commons.lang3.StringUtils.isBlank;

/*
 * #%L
 * HAPI FHIR - Server Framework
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.interceptor.BaseValidatingInterceptor;
import ca.uhn.fhir.rest.server.method.ResourceParameter;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;

/**
 * This interceptor intercepts each incoming request and if it invokes the $validate operation validates that resource. The
 * interceptor may be configured to run any validator modules, and will then add headers to the response or fail the
 * request with an {@link UnprocessableEntityException HTTP 422 Unprocessable Entity}.
 * NOTE: currently only a syntax is supported where the profile url is specififed, the Parameter sy
 */
public class ValidateOperationInterceptor extends BaseValidatingInterceptor<String> {

	/**
	 * X-HAPI-Request-Validation
	 */
	public static final String DEFAULT_RESPONSE_HEADER_NAME = "X-FHIR-Request-Validation";

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ValidateOperationInterceptor.class);

	/**
	 * A {@link RequestDetails#getUserData() user data} entry will be created with this
	 * key which contains the {@link ValidationResult} from validating the request.
	 */
	public static final String REQUEST_VALIDATION_RESULT = ValidateOperationInterceptor.class.getName() + "_REQUEST_VALIDATION_RESULT";

	private boolean myAddValidationResultsToResponseOperationOutcome = true;

  @Override
	protected ValidationResult doValidate(FhirValidator theValidator, String theRequest, ValidationOptions options)  {
	  return theValidator.validateWithResult(theRequest, options);
	}

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest, HttpServletResponse theResponse) throws AuthenticationException {
	  
    if (!isValidateOperation(theRequestDetails)) {
      return true;
    }
	  
		EncodingEnum encoding = RestfulServerUtils.determineRequestEncodingNoDefault(theRequestDetails);
		if (encoding == null) {
			ourLog.trace("Incoming request does not appear to be FHIR, not going to validate");
			return true;
		}

		Charset charset = ResourceParameter.determineRequestCharset(theRequestDetails);
		String requestText = new String(theRequestDetails.loadRequestContents(), charset);

		if (isBlank(requestText)) {
			ourLog.trace("Incoming request does not have a body");
			return true;
		}

		ValidationResult validationResult = validate(requestText, theRequestDetails);

		// The JPA server will use this
		theRequestDetails.getUserData().put(REQUEST_VALIDATION_RESULT, validationResult);

		return true;
	}

  private boolean isValidateOperation(RequestDetails theRequestDetails) {
    return (theRequestDetails.getRestOperationType() != null && "$validate".equals(theRequestDetails.getOperation()));
  }

	/**
	 * If set to {@literal true} (default is true), the validation results
	 * will be added to the OperationOutcome being returned to the client,
	 * unless the response being returned is not an OperationOutcome
	 * to begin with (e.g. if the client has requested 
	 * <code>Return: prefer=representation</code>)
	 */
	public boolean isAddValidationResultsToResponseOperationOutcome() {
		return myAddValidationResultsToResponseOperationOutcome;
	}

	@Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
	public boolean outgoingResponse(RequestDetails theRequestDetails, IBaseResource theResponseObject) {
		if (myAddValidationResultsToResponseOperationOutcome) {
			if (theResponseObject instanceof IBaseOperationOutcome) {
				IBaseOperationOutcome oo = (IBaseOperationOutcome) theResponseObject;

				if (theRequestDetails != null) {
					ValidationResult validationResult = (ValidationResult) theRequestDetails.getUserData().get(ValidateOperationInterceptor.REQUEST_VALIDATION_RESULT);
					if (validationResult != null) {
						validationResult.populateOperationOutcome(oo);
					}
				}

			}
		}

		return true;
	}

  protected String provideDefaultResponseHeaderName() {
		return DEFAULT_RESPONSE_HEADER_NAME;
	}

	/**
	 * If set to {@literal true} (default is true), the validation results
	 * will be added to the OperationOutcome being returned to the client,
	 * unless the response being returned is not an OperationOutcome
	 * to begin with (e.g. if the client has requested 
	 * <code>Return: prefer=representation</code>)
	 */
	public void setAddValidationResultsToResponseOperationOutcome(boolean theAddValidationResultsToResponseOperationOutcome) {
		myAddValidationResultsToResponseOperationOutcome = theAddValidationResultsToResponseOperationOutcome;
	}

	/**
	 * Sets the name of the response header to add validation failures to
	 * 
	 * @see #DEFAULT_RESPONSE_HEADER_NAME
	 * @see #setAddResponseHeaderOnSeverity(ResultSeverityEnum)
	 */
	@Override
	public void setResponseHeaderName(String theResponseHeaderName) {
		super.setResponseHeaderName(theResponseHeaderName);
	}

}
