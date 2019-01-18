package ch.ahdis.matchbox;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.spring.boot.autoconfigure.FhirRestfulServerCustomizer;

@SpringBootApplication
public class MatchboxApplication {

	@Bean
	public FhirRestfulServerCustomizer fhirServerCustomizer() {
		return (server) -> {
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
