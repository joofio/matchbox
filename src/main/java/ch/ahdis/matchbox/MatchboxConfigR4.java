package ch.ahdis.matchbox;
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
import org.hl7.fhir.r5.utils.IResourceValidator.BestPracticeWarningLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.rp.r4.StructureMapResourceProvider;
import ca.uhn.fhir.validation.IValidatorModule;
import ch.ahdis.matchbox.mappinglanguage.StructureMapTransformProvider;
import ch.ahdis.matchbox.validation.FhirInstanceValidator;

@Configuration
@ConditionalOnProperty(name = "hapi.fhir.version", havingValue = "R4")
public class MatchboxConfigR4 extends BaseJavaConfigR4 {

	@Override
	public StructureMapResourceProvider rpStructureMapR4() {
		StructureMapResourceProvider retVal = new StructureMapTransformProvider();
		retVal.setContext(fhirContextR4());
		retVal.setDao(daoStructureMapR4());
		return retVal;
	}
	
	@Bean(name = "myInstanceValidatorR4")
	@Lazy
	public IValidatorModule instanceValidatorR4() {
		// use an instance validator which incorporates the ValidationEngine from RI with R5 version
		FhirInstanceValidator val = new FhirInstanceValidator();
		val.setBestPracticeWarningLevel(BestPracticeWarningLevel.Warning);
		val.setValidationSupport(validationSupportChainR4());
		return val;
	}
	
}
