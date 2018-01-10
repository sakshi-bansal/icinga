package org.icinga.myexample.test;

import org.icinga.myexample.MyPlugin;
import org.openbaton.catalogue.mano.common.monitoring.ObjectSelection;
import org.openbaton.exceptions.MonitoringException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.BeforeClass;

//TODO: Set correct time intervals and test occurences

public class VirtualisedResourcePerformanceManagement {
  private static MyPlugin myPlugin;
  private List<String> pmjobIds = new ArrayList();

  @BeforeClass
  public static void init() throws RemoteException, InterruptedException, MonitoringException {
    myPlugin = new MyPlugin();
  }

  @Test
  public void testqueryPMJob() throws MonitoringException, InterruptedException {
    List<String> hostnames = new ArrayList();
    List<String> metrics = new ArrayList();
    hostnames.add("container1");
    metrics.add("ping4");
    metrics.add("disk");
    String period = "0";
    myPlugin.queryPMJob (hostnames, metrics, period);
  }

  @Test
  public void testcreatePMJob() throws MonitoringException, InterruptedException {
    String pmjobId;
    ObjectSelection objectSelection = addObjects("localhost", "container1");
    List<String> performanceMetrics = addPerformanceMetrics("disk", "ping");

    //TODO: set correct durations
    pmjobId = myPlugin.createPMJob(objectSelection, performanceMetrics,
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
