package ch.ahdis.matchbox;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.dao.FulltextSearchSvcImpl;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.sched.ISchedulerService;
import ca.uhn.fhir.jpa.sched.AutowiringSpringBeanJobFactory;
import ca.uhn.fhir.jpa.sched.HapiSchedulerServiceImpl;
import ca.uhn.fhir.jpa.subscription.channel.api.IChannelFactory;
import ca.uhn.fhir.jpa.subscription.channel.impl.LinkedBlockingChannelFactory;
import ca.uhn.fhir.jpa.subscription.channel.subscription.IChannelNamer;
import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionChannelFactory;
import ca.uhn.fhir.jpa.subscription.match.matcher.matching.SubscriptionStrategyEvaluator;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.SubscriptionMatcherInterceptor;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.SubscriptionSubmitInterceptorLoader;
import ca.uhn.fhir.jpa.subscription.submit.interceptor.SubscriptionValidatingInterceptor;
import ca.uhn.fhir.jpa.subscription.triggering.ISubscriptionTriggeringSvc;
import ca.uhn.fhir.jpa.subscription.triggering.SubscriptionTriggeringSvcImpl;

@Configuration
@ConditionalOnProperty(name = "hapi.fhir.version", havingValue = "R4")
public class MatchboxConfigR4 extends BaseJavaConfigR4 {
  
  private static final String HAPI_DEFAULT_SCHEDULER_GROUP = "HAPI";

//  @Bean
//  public DefaultProfileValidationSupport defaultProfileValidationSupport() {
//    return new DefaultProfileValidationSupport();
//  }
  
  @Bean
  public ISchedulerService schedulerService() {
    return new HapiSchedulerServiceImpl().setDefaultGroup(HAPI_DEFAULT_SCHEDULER_GROUP);
  }

  @Bean
  public AutowiringSpringBeanJobFactory schedulerJobFactory() {
    return new AutowiringSpringBeanJobFactory();
  }
  
//  @Bean(name = "myDaoRegistry")
//  @Lazy
//  public ca.uhn.fhir.jpa.dao.DaoRegistry daoRegistryR4() {
//    return new ca.uhn.fhir.jpa.dao.DaoRegistry();
//  }
  

  @Primary
  @Bean
  public JpaTransactionManager hapiTransactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  @Bean
  @ConditionalOnMissingBean
  @ConfigurationProperties("hapi.fhir.jpa")
  public PartitionSettings partitionSettings() {
    return new PartitionSettings();
  }
  
  @Bean
  @Lazy
  public ISubscriptionTriggeringSvc subscriptionTriggeringSvc() {
    return new SubscriptionTriggeringSvcImpl();
  }
  
  
  @Bean
  public SubscriptionMatcherInterceptor subscriptionMatcherInterceptor() {
    return new SubscriptionMatcherInterceptor();
  }

  @Bean
  public SubscriptionValidatingInterceptor subscriptionValidatingInterceptor() {
    return new SubscriptionValidatingInterceptor();
  }

  @Bean
  public SubscriptionSubmitInterceptorLoader subscriptionMatcherInterceptorLoader() {
    return new SubscriptionSubmitInterceptorLoader();
  }

  @Bean
  public IChannelFactory queueChannelFactory(IChannelNamer theChannelNamer) {
    return new LinkedBlockingChannelFactory(theChannelNamer);
  }

  @Bean
  public SubscriptionChannelFactory subscriptionChannelFactory(IChannelFactory theQueueChannelFactory) {
    return new SubscriptionChannelFactory(theQueueChannelFactory);
  }

  @Bean
  public IChannelNamer channelNamer() {
    return (theNameComponent, theChannelSettings) -> theNameComponent;
  }

  @Bean
  public SubscriptionCanonicalizer subscriptionCanonicalizer(FhirContext theFhirContext) {
    return new SubscriptionCanonicalizer(theFhirContext);
  }

  @Bean
  public SubscriptionStrategyEvaluator subscriptionStrategyEvaluator() {
    return new SubscriptionStrategyEvaluator();
  }

//  @Bean(autowire = Autowire.BY_TYPE)
//  public IFulltextSearchSvc searchDaoR4() {
//    FulltextSearchSvcImpl searchDao = new FulltextSearchSvcImpl();
//    return searchDao;
//  }  
}
