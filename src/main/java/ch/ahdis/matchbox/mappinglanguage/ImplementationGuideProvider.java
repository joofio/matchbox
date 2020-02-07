package ch.ahdis.matchbox.mappinglanguage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.hl7.fhir.r4.model.ImplementationGuide;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.provider.HashMapResourceProvider;

public class ImplementationGuideProvider extends HashMapResourceProvider<ImplementationGuide> {
  
  public final static String IG_LOAD = "IG_LOAD";

  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener listener) {
      this.pcs.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
      this.pcs.removePropertyChangeListener(listener);
  }
  
  public ImplementationGuideProvider(FhirContext theFhirContext) {
    super(theFhirContext, ImplementationGuide.class);
  }

  protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImplementationGuideProvider.class);

  @Override
  public MethodOutcome create(ImplementationGuide theResource) {
    MethodOutcome outcome = super.create(theResource);
    if (outcome.getCreated() && theResource.getPackageId()!=null) {
      String ig = theResource.getPackageId();
      if (theResource.getVersion()!=null) {
        ig += "#" + theResource.getVersion();
      }
      this.pcs.firePropertyChange(IG_LOAD, null, ig);
    }
    return outcome;
  }
  
}
