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

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ch.ahdis.mapping.fml.MappingLanguageTransfomerByIg;

/**
 * Operation $convert on Resource @link https://www.hl7.org/fhir/resource-operation-convert.html
 * Actually currently more a declaration than an implementation, but it works :-)
 * - Convertion between versions is handled with VersionInterceptor @see ch.ahdis.matchbox.interceptor.VersionInterceptor
 * - Convertion between fhir+xml and fhir+json is automatically handled in the hapi-fhir base request handling ...
 */
public class Convert {
	
	static private MappingLanguageTransfomerByIg chmed16af = null; 

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Convert.class);

	public Convert() {
		if (chmed16af==null) {
			chmed16af = new MappingLanguageTransfomerByIg("ch.mediplan.chmed16af#dev");
		}
	}

	@Operation(name = "$convert", idempotent = true, returnParameters = {
			@OperationParam(name = "output", type = IBase.class, min = 1, max = 1) })
	public IBaseResource convert(@OperationParam(name = "input", min = 1, max = 1) final IBaseResource content,
			@OperationParam(name = "ig", min = 0, max = 1) final String ig,
			@OperationParam(name = "from", min = 0, max = 1) final String from,
			@OperationParam(name = "to", min = 0, max = 1) final String to,
			HttpServletRequest theRequest) {
		
		log.debug("$convert");
		
		try {
			if (to !=null && from != null) {
				log.debug("convert chmed16af from "+ from +" to " + to );
				IBaseResource output = chmed16af.convertResource((Resource) content, from, to);
				log.debug("converted chmed16af from "+ from +" to " + to );				
				return output;
			}
		} catch (FHIRException e) {
			log.error("convert operation failed", e);
		}
		return content;
	}

}
