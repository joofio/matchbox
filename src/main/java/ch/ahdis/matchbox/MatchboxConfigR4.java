package ch.ahdis.matchbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.rp.r4.StructureMapResourceProvider;
import ch.ahdis.matchbox.mappinglanguage.StructureMapTransformProvider;

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
	
}
