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
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.jpa.packages.IPackageInstallerSvc;
import ca.uhn.fhir.jpa.packages.PackageInstallationSpec;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import ch.ahdis.matchbox.spring.boot.autoconfigure.FhirAutoConfiguration;
import ch.ahdis.matchbox.spring.boot.autoconfigure.FhirProperties.Ig;
import ch.ahdis.matchbox.spring.boot.autoconfigure.FhirRestfulServerCustomizer;

@SpringBootApplication
public class MatchboxApplication {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MatchboxApplication.class);

  private final boolean JPA = true;

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  private FhirAutoConfiguration autoConfiguration;

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

//        server.registerInterceptor(new MappingLanguageInterceptor());
//
//        server.registerProvider(new Validate());
//        server.registerProvider(new TransactionProvider(server));
//
//        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
//
//        FhirInstanceValidator validatorModule = new FhirInstanceValidator(null);
//        validatorModule.setBestPracticeWarningLevel(BestPracticeWarningLevel.Warning);
//        ValidateOperationInterceptor validatorInterceptor = new ValidateOperationInterceptor();
//        validatorInterceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
//        validatorInterceptor.setValidatorModules(Collections.singletonList(validatorModule));
//        server.registerInterceptor(validatorInterceptor);
//
//        ImplementationGuideProvider implementationGuideProvider = new ImplementationGuideProvider(
//            validatorModule.getContext());
//
//        implementationGuideProvider.addPropertyChangeListener(validatorModule);
//        resourceProviders.add(implementationGuideProvider);
//
//        resourceProviders.add(new StructureMapTransformProvider(validatorModule.getContext()));
//
//        server.setResourceProviders(resourceProviders);
      } else {
        log.debug("registering JpaSystemProviderR4");
        server.registerProvider(appContext.getBean("mySystemProviderR4", JpaSystemProviderR4.class));
      }

      /*
       * This interceptor formats the output using nice colourful HTML output when the
       * request is detected to come from a browser.
       */
      ResponseHighlighterInterceptor responseHighlighterInterceptor = new ResponseHighlighterInterceptor();
      server.registerInterceptor(responseHighlighterInterceptor);
      log.debug("fhirServerCustomizer finished");

      if (autoConfiguration != null && autoConfiguration.getProperties() != null) {
        List<Ig> igs = autoConfiguration.getProperties().getIgs();
        if (igs != null) {
          for (Ig ig : igs) {
            String url = ig.getUrl();
            String name = ig.getName();
            String ver = ig.getVersion();

            log.debug("Installing IG: {}, {}, {}", url, name, ver);
            appContext.getBean(IPackageInstallerSvc.class)
                .install(new PackageInstallationSpec().setPackageUrl(url).setName(name).setVersion(ver)
                    .setInstallMode(PackageInstallationSpec.InstallModeEnum.STORE_AND_INSTALL));

          }
        }
      }

    };
  }

  public static void main(String[] args) {
    SpringApplication.run(MatchboxApplication.class, args);
  }

}
