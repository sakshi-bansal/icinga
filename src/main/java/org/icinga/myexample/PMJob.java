package org.icinga.myexample; 

import java.util.List;
import java.util.Map;
import org.openbaton.catalogue.mano.common.monitoring.ObjectSelection;
import org.openbaton.catalogue.util.IdGenerator;

public class PMJob {
  private String pmjobId;
  private ObjectSelection objectSelection;
  private List<String> performanceMetrics;
  private List<String> performanceMetricGroup;
  private Integer reportingPeriod;
  private Integer collectionPeriod;

  public PMJob(ObjectSelection objectSelection, List<String> performanceMetrics,
	       List<String> performanceMetricGroup, Integer collectionPeriod,
	       Integer reportingPeriod) {
    this.objectSelection = objectSelection;
    this.performanceMetrics = performanceMetrics;
    this.performanceMetricGroup = performanceMetricGroup;
    this.collectionPeriod = collectionPeriod;
    this.reportingPeriod = reportingPeriod;
    this.pmjobId = IdGenerator.createUUID();
  }

  public String getPMJobId() {
    return pmjobId;
  }

  public ObjectSelection getobjectSelection() {
    return objectSelection;
  }

  public List<String> getperformanceMetrics() {
    return performanceMetrics;
  }
}
