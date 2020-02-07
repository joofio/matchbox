package ch.ahdis.matchbox.operation;

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

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

/**
 * Operation $validate
 * dummy, we just expose the operatin to be processed, the work will however be done in @ValidationOperationInterceptor,
 * we want to intercept the original body
 */
public class Validate {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Validate.class);
   
  @Operation(name = "$validate", idempotent = true, returnParameters = {
      @OperationParam(name = "return", type = IBase.class, min = 1, max = 1) })
  public IBaseResource validate(@OperationParam(name = "resource", min = 1, max = 1) final IBaseResource content,
      @OperationParam(name = "code", min = 0, max = 1) final String code,
      @OperationParam(name = "profile", min = 0, max = 1) final String profileUrl, HttpServletRequest theRequest) {
    log.debug("$validate");
    return new OperationOutcome();
  }

}
