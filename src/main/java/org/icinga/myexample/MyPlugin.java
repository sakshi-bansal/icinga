package org.icinga.myexample;

import com.google.gson.*;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import org.openbaton.catalogue.mano.common.monitoring.*;
import org.openbaton.exceptions.MonitoringException;
import org.openbaton.monitoring.interfaces.MonitoringPlugin;
import org.openbaton.catalogue.nfvo.Item;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.InvocationTargetException;
import org.openbaton.plugin.PluginStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MyPlugin extends MonitoringPlugin {
  private String icingaApiURL;
  private String host;
  private String port;
  private String version;
  private String username;
  private String password;
  private String params;
  private IcingaApi icingaApi;
  private String nrsc;
  private Integer nrsp;
  private Map<String, PMJob> pmJobs;

  public MyPlugin() throws RemoteException, MonitoringException {
    init ();
  }

  private void init() throws RemoteException, MonitoringException {
    loadProperties();

    host     = properties.getProperty("icinga-host");
    port     = properties.getProperty("icinga-port");
    version  = properties.getProperty("icinga-version");
    icingaApiURL = "https://" + host + ":" + port + version + "/objects/services/";
    username = properties.getProperty("username");
    password = properties.getProperty("password");
    params = username + ":" + password;
    nrsc     = properties.getProperty("notification-receiver-server-context");
    String serverPort = properties.getProperty("notification-receiver-server-port", "3081");
    nrsp = Integer.parseInt(serverPort);

    icingaApi = new IcingaApi (icingaApiURL, params);
    pmJobs = new HashMap<>();
  }

  public Item getMeasurement (String hostname, String metric) {
    Item data = new Item();
    try{
      String line = icingaApi.getCommand (hostname, metric);
      if (line.contains("error") || line.contains("Bad")) {
        System.out.print ("Unable to process request");
      } else {
          data.setValue(line);
	  return data;
      }
    } catch (Exception e) {
       System.out.print ("Unable to process request");
    }
    return null;
  }

 //TODO : when priod > 0
 public List<Item> getMeasurementResults (List<String> hostnames, List<String> metrics, String period) {
    List<Item> results = new ArrayList<>();
    Item result;

    for (String host:hostnames) {
      for (String metric:metrics) {
        result = getMeasurement (host, metric);
        if (result != null)
          results.add (result);
        else
          System.out.print ("Error: No object found for host " + host + " and metric " + metric);
      }
    }
    return results;
  }

  @Override
  public List<Alarm> getAlarmList(String vnfId, PerceivedSeverity perceivedSeverity) {
    return null;
  }

@Override
  public void notifyFault(AlarmEndpoint endpoint, AbstractVirtualizedResourceAlarm event) {
  }
  @Override
  public String subscribeForFault(AlarmEndpoint endpoint) throws MonitoringException {
    return null;
  }
  @Override
  public String unsubscribeForFault(String alarmEndpointId) {
    return null;
  }

  @Override
  public String createPMJob(
   ObjectSelection objectSelection,
      List<String> performanceMetrics,
      List<String> performanceMetricGroup,
      Integer collectionPeriod,
      Integer reportingPeriod)
      throws MonitoringException {
    if (objectSelection == null || objectSelection.getObjectInstanceIds().isEmpty())
      throw new MonitoringException("The objectSelection is null or empty");
    if (performanceMetrics == null && performanceMetricGroup == null)
      throw new MonitoringException(
          "At least performanceMetrics or performanceMetricGroup need to be present");
    if (collectionPeriod < 0 || reportingPeriod < 0)
      throw new MonitoringException("The collectionPeriod or reportingPeriod is negative");

    PMJob pmjob = new PMJob (objectSelection, performanceMetrics, performanceMetricGroup,
                 	     collectionPeriod, reportingPeriod);
    try {
      for (String host : objectSelection.getObjectInstanceIds()) {
        for (String metric : performanceMetrics) {
          icingaApi.putCommand (host, metric, collectionPeriod);
        }
      }
    } catch (Exception e) {
        System.out.print ("Failed to create new PMJob");
    }

    pmJobs.put(pmjob.getPMJobId(), pmjob);
    return pmjob.getPMJobId();
  }

  @Override
  public List<String> deletePMJob(List<String> pmJobIdsToDelete) throws MonitoringException {
    ObjectSelection objectSelection = null;
    List<String> performanceMetrics = new ArrayList();
 
    if (pmJobIdsToDelete == null)
      throw new MonitoringException("The hostnames or metrics is null");

    try{
      for (String pmjobId: pmJobIdsToDelete) {
      PMJob pmJob = pmJobs.get(pmjobId);
      objectSelection = pmJob.getobjectSelection();
      performanceMetrics = pmJob.getperformanceMetrics();
      for (String hostname : objectSelection.getObjectInstanceIds()) {
        for (String metric : performanceMetrics) {
          icingaApi.deleteCommand(hostname, metric); 
	}
      }
     }
    } catch (Exception e) {
        System.out.print ("Failed to create new PMJob");
    }
    return null;
  }

  @Override
  public List<Item> queryPMJob(List<String> hostnames, List<String> metrics, String period)
      throws MonitoringException {
    int timePeriod = Integer.parseInt(period);
    if (hostnames == null || metrics == null)
      throw new MonitoringException("The hostnames or metrics is null");
    if (timePeriod < 0)
      throw new MonitoringException("The period is negative");

    List<Item> results = getMeasurementResults (hostnames, metrics, period);
    return results;
  }

  @Override
  public void subscribe() {}

  @Override
  public void notifyInfo() {}

  @Override
  public String createThreshold(
      ObjectSelection objectSelector,
      String performanceMetric,
      ThresholdType thresholdType,
      ThresholdDetails thresholdDetails)
      throws MonitoringException {
    return null;
  }
  @Override
  public List<String> deleteThreshold(List<String> thresholdIds) throws MonitoringException {
    return null;
  }
  @Override
  public void queryThreshold(String queryFilter) {}

  public static void main(String[] args)
      throws IOException, InstantiationException, TimeoutException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException, InterruptedException {
    Logger log = LoggerFactory.getLogger(MyPlugin.class);
    PluginStarter.registerPlugin(MyPlugin.class, "icinga", "localhost", 5672, 1);
  }

}
