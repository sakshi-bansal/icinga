package org.icinga.myexample.test;

import org.icinga.myexample.MyPlugin;
import org.openbaton.catalogue.mano.common.monitoring.ObjectSelection;
import org.openbaton.exceptions.MonitoringException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.BeforeClass;
import org.openbaton.catalogue.mano.common.monitoring.AlarmEndpoint;
import org.openbaton.catalogue.mano.common.monitoring.PerceivedSeverity;
import org.openbaton.catalogue.nfvo.EndpointType;
import org.openbaton.catalogue.mano.common.monitoring.ThresholdType;
import org.openbaton.catalogue.mano.common.monitoring.ThresholdDetails;

//TODO: Set correct time intervals and test occurences

public class VirtualisedResourcePerformanceManagement {
  private static MyPlugin myPlugin;
  private List<String> pmjobIds = new ArrayList();
  private List<String> thresholdIds = new ArrayList();

  @BeforeClass
  public static void init() throws RemoteException, InterruptedException, MonitoringException {
    myPlugin = new MyPlugin();
  }

  @Test
  public void testqueryPMJob() throws MonitoringException, InterruptedException {
    List<String> hostnames = new ArrayList();
    List<String> metrics = new ArrayList();
    hostnames.add("container1");
    metrics.add("disk");
    String period = "0";
    myPlugin.queryPMJob (hostnames, metrics, period);
  }

  @Test
  public void testcreatePMJob() throws MonitoringException, InterruptedException {
    String pmjobId;
    ObjectSelection objectSelection = addObjects("localhost", "container1");
    List<String> performanceMetrics = addPerformanceMetrics("disk", "ping4", "1");

    //TODO: set correct durations
    pmjobId = myPlugin.createPMJob(objectSelection, performanceMetrics,
 				   new ArrayList<String>(), 10, 0);
    pmjobIds.add(pmjobId);

    ObjectSelection objectSelection2 = addObjects("container1");
    List<String> performanceMetrics2 = addPerformanceMetrics("dns", "1");

    //TODO: set correct durations
    pmjobId = myPlugin.createPMJob(objectSelection2, performanceMetrics2,
 				   new ArrayList<String>(), 10, 0);
    pmjobIds.add(pmjobId);
  }

  @Test
  public void testdeletePMJob() throws MonitoringException, InterruptedException {
    if (pmjobIds.size() > 0) {
      List <String> pmjobId = new ArrayList();
      //Only testing with first PMJob
      pmjobId.add(pmjobIds.get(0));
      myPlugin.deletePMJob(pmjobId);
      pmjobIds.remove(pmjobIds.get(0));
    }
  }

  @Test
  public void testcreateThreshold() throws MonitoringException, InterruptedException {
    AlarmEndpoint alarmEndpoint = new AlarmEndpoint("fault-manager-of-container1","container1",
                                                    EndpointType.REST,"http://localhost:9000/alarm/vr",
                                                    PerceivedSeverity.WARNING);
    String id = myPlugin.subscribeForFault(alarmEndpoint);

    ObjectSelection objectSelector = addObjects("container1");
    ThresholdDetails thresholdDetails =
        new ThresholdDetails("", "", PerceivedSeverity.CRITICAL, "", "");
    thresholdDetails.setPerceivedSeverity(PerceivedSeverity.CRITICAL);
    String thresholdId = myPlugin.createThreshold(
            objectSelector, "ping4", ThresholdType.SINGLE_VALUE, thresholdDetails);
    thresholdIds.add(thresholdId);
  }

  @Test
  public void testdeleteThreshold() throws MonitoringException, InterruptedException {
    if (thresholdIds.size() > 0) {
      List<String> thresholdIdsToDelete = new ArrayList<>();
      thresholdIdsToDelete.add(thresholdIds.get(0));
      List<String> thresholdIdsDeleted = myPlugin.deleteThreshold(thresholdIdsToDelete);
    }
  }

  private ObjectSelection addObjects(String... args) {
    ObjectSelection objectSelection  = new ObjectSelection();
    for (String arg : args) {
      objectSelection.addObjectInstanceId(arg);
    }
    return objectSelection;
  }

  private List<String> addPerformanceMetrics(String... args) {
    List<String> performanceMetrics = new ArrayList<>();
    for (String arg : args) {
      performanceMetrics.add(arg);
    }
    return performanceMetrics;
  }
} 
