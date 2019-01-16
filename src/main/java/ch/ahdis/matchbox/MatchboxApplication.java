package ch.ahdis.matchbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MatchboxApplication {

//	@Bean
//	public FhirRestfulServerCustomizer fhirServerCustomizer(StructureMapTransformProvider provider) {
//	   return (server) -> {
//	      server.getPlainProviders().add(provider);
//	   };
//	}
	
	public static void main(String[] args) {
		SpringApplication.run(MatchboxApplication.class, args);
	}

}

