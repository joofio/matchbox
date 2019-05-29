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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.spring.boot.autoconfigure.FhirRestfulServerCustomizer;
import ch.ahdis.matchbox.interceptor.VersionInterceptor;
import ch.ahdis.matchbox.operation.Convert;

@SpringBootApplication
public class MatchboxApplication {

	@Autowired
	private ApplicationContext appContext;

	@Bean
	public FhirRestfulServerCustomizer fhirServerCustomizer() {
		return (server) -> {

			String[] beans = appContext.getBeanDefinitionNames();
			Arrays.sort(beans);
			for (String bean : beans) {
				System.out.println(bean);
			}

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
			CorsInterceptor interceptor = new CorsInterceptor(config);
			server.registerInterceptor(interceptor);

			server.registerInterceptor(new VersionInterceptor());

			server.registerProvider(new Convert());
			server.registerProvider(appContext.getBean("mySystemProviderR4", JpaSystemProviderR4.class));
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(MatchboxApplication.class, args);
	}
//	
//	@Bean
//	public DaoConfig daoConfig() {
//	    DaoConfig retVal = new DaoConfig();
//	    retVal.setReuseCachedSearchResultsForMillis((long) 0);
//	    return retVal;
//	}

}
