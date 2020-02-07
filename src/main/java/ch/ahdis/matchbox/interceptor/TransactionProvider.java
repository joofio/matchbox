package ch.ahdis.matchbox.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.method.BaseMethodBinding;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

public class TransactionProvider {

  private RestfulServer restfulServer;

  public TransactionProvider(RestfulServer restfulServer) {
    super();
    this.restfulServer = restfulServer;
  }

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionProvider.class);

  @Transaction
  public IBundleProvider transaction(@TransactionParam Bundle theBundle) {
    log.debug("transaction");
    List<IBaseResource> entries = new ArrayList<IBaseResource>();

    ServletRequestDetails requestDetails = new ServletRequestDetails(null);

    for (BundleEntryComponent nextEntry : theBundle.getEntry()) {
      if (nextEntry.getRequest() != null && (nextEntry.getRequest().getMethod() == HTTPVerb.POST)) {
        requestDetails.setResourceName(nextEntry.getResource().getResourceType().name());
        requestDetails.setRequestType(RequestTypeEnum.POST);
        BaseMethodBinding<?> resourceMethod = restfulServer.determineResourceMethod(requestDetails, "");
        if (resourceMethod != null) {
          MethodOutcome created;
          try {
            created = (MethodOutcome) resourceMethod.getMethod().invoke(resourceMethod.getProvider(),
                nextEntry.getResource());
            if (created.getCreated()) {
              entries.add(created.getResource());
            }
          } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            OperationOutcome oo = new OperationOutcome();
            String msg = "Creation failed for "+requestDetails.getResourceName();
            oo.addIssue().setSeverity(IssueSeverity.FATAL).setDetails((new CodeableConcept()).setText(msg));
            throw new InternalErrorException(msg, oo);
          }
        } else {
          OperationOutcome oo = new OperationOutcome();
          String msg = "ResourceProvider not available for "+requestDetails.getResourceName();
          oo.addIssue().setSeverity(IssueSeverity.FATAL).setDetails((new CodeableConcept()).setText(msg));
          throw new InternalErrorException(msg, oo);
        }
      } else {
        OperationOutcome oo = new OperationOutcome();
        String msg = "Method not spefified or implemented yet in transaction";
        oo.addIssue().setSeverity(IssueSeverity.FATAL).setDetails((new CodeableConcept()).setText(msg));
        throw new InternalErrorException(msg, oo);
      }
    }
    return new SimpleBundleProvider(entries);
  }

}
