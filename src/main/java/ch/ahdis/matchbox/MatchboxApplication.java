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
import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ch.ahdis.matchbox.interceptor.MappingLanguageInterceptor;
import ch.ahdis.matchbox.interceptor.VersionInterceptor;
import ch.ahdis.matchbox.operation.Convert;
import ch.ahdis.matchbox.spring.boot.autoconfigure.FhirRestfulServerCustomizer;

@SpringBootApplication
public class MatchboxApplication {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MatchboxApplication.class);

	@Autowired
	private ApplicationContext appContext;

	@Bean
	public FhirRestfulServerCustomizer fhirServerCustomizer() {
		return (server) -> {

			log.debug("fhirServerCustomizer");

			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedHeader("x-fhir-starter");
			config.addAllowedHeader("Origin");
			config.addAllowedHeader("Accept");
			config.addAllowedHeader("X-Requested-With");
			config.addAllowedHeader("Content-Type");

			config.addAllowedOrigin("*");

			config.addExposedHeader("Location");
			config.addExposedHeader("Content-Location");
			config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

			// Create the interceptor and register it
			log.debug("registering CorsInterceptor");
			CorsInterceptor interceptor = new CorsInterceptor(config);
			server.registerInterceptor(interceptor);

			log.debug("registering VersionInterceptor");
			server.registerInterceptor(new VersionInterceptor());

			
			server.registerInterceptor(new MappingLanguageInterceptor());

			server.registerProvider(new Convert());

			log.debug("registering JpaSystemProviderR4");
			server.registerProvider(appContext.getBean("mySystemProviderR4", JpaSystemProviderR4.class));
			
			
    IValidatorModule validatorModule = appContext.getBean("myInstanceValidatorR4", IValidatorModule.class);
    if (validatorModule != null) {
        RequestValidatingInterceptor validatorInterceptor = new RequestValidatingInterceptor();
        validatorInterceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
        validatorInterceptor.setValidatorModules(Collections.singletonList(validatorModule));
        server.registerInterceptor(validatorInterceptor);
    }

			log.debug("fhirServerCustomizer finished");
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(MatchboxApplication.class, args);
	}

}
