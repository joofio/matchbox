package ch.ahdis.matchbox;

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
