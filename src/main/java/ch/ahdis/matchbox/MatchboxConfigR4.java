package ch.ahdis.matchbox;

import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.r5.utils.IResourceValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.model.sched.ISchedulerService;
import ca.uhn.fhir.jpa.sched.AutowiringSpringBeanJobFactory;
import ca.uhn.fhir.jpa.sched.HapiSchedulerServiceImpl;

@Configuration
@ConditionalOnProperty(name = "hapi.fhir.version", havingValue = "R4")
public class MatchboxConfigR4 extends BaseJavaConfigR4 {
  
  private static final String HAPI_DEFAULT_SCHEDULER_GROUP = "HAPI";

  @Bean
  public DefaultProfileValidationSupport defaultProfileValidationSupport() {
    return new DefaultProfileValidationSupport();
  }
  
  @Bean
  public ISchedulerService schedulerService() {
    return new HapiSchedulerServiceImpl().setDefaultGroup(HAPI_DEFAULT_SCHEDULER_GROUP);
  }

  @Bean
  public AutowiringSpringBeanJobFactory schedulerJobFactory() {
    return new AutowiringSpringBeanJobFactory();
  }
  
  @Bean(name = "myDaoRegistry")
  @Lazy
  public ca.uhn.fhir.jpa.dao.DaoRegistry daoRegistryR4() {
    return new ca.uhn.fhir.jpa.dao.DaoRegistry();
  }

}
