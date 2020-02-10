package ch.ahdis.matchbox;

import java.util.ArrayList;
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
import java.util.List;

import org.hl7.fhir.r5.utils.IResourceValidator.BestPracticeWarningLevel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ch.ahdis.matchbox.interceptor.MappingLanguageInterceptor;
import ch.ahdis.matchbox.interceptor.TransactionProvider;
import ch.ahdis.matchbox.interceptor.ValidateOperationInterceptor;
import ch.ahdis.matchbox.mappinglanguage.ImplementationGuideProvider;
import ch.ahdis.matchbox.mappinglanguage.StructureMapTransformProvider;
import ch.ahdis.matchbox.operation.Validate;
import ch.ahdis.matchbox.spring.boot.autoconfigure.FhirRestfulServerCustomizer;
import ch.ahdis.matchbox.validation.FhirInstanceValidator;

@SpringBootApplication
public class MatchboxApplication {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MatchboxApplication.class);

  private final boolean JPA = false;

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

      if (!JPA) {

//        log.debug("registering VersionInterceptor");
//      server.registerInterceptor(new VersionInterceptor());
//        server.registerProvider(new Convert());

        server.registerInterceptor(new MappingLanguageInterceptor());

        server.registerProvider(new Validate());
        server.registerProvider(new TransactionProvider(server));

        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();

        FhirInstanceValidator validatorModule = new FhirInstanceValidator(null);
        validatorModule.setBestPracticeWarningLevel(BestPracticeWarningLevel.Warning);
        ValidateOperationInterceptor validatorInterceptor = new ValidateOperationInterceptor();
        validatorInterceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
        validatorInterceptor.setValidatorModules(Collections.singletonList(validatorModule));
        server.registerInterceptor(validatorInterceptor);

        ImplementationGuideProvider implementationGuideProvider = new ImplementationGuideProvider(
            validatorModule.getContext());

        implementationGuideProvider.addPropertyChangeListener(validatorModule);
        resourceProviders.add(implementationGuideProvider);

        resourceProviders.add(new StructureMapTransformProvider(validatorModule.getContext()));

        server.setResourceProviders(resourceProviders);
      }

      log.debug("fhirServerCustomizer finished");
    };
  }

  public static void main(String[] args) {
    SpringApplication.run(MatchboxApplication.class, args);
  }

}
