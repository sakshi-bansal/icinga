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
  private String nrsc;
  private Integer nrsp;

  public MyPlugin() throws RemoteException, MonitoringException {
    init ();
  }

  private void init() throws RemoteException, MonitoringException {
    loadProperties();
    host     = properties.getProperty("icinga-host");
    port     = properties.getProperty("icinga-port");
    version  = properties.getProperty("icinga-version");
    icingaApiURL = "https://" + host + ":" + port + version;
    username = properties.getProperty("username");
    password = properties.getProperty("password");
    params = username + ":" + password;
    nrsc     = properties.getProperty("notification-receiver-server-context");
    String serverPort = properties.getProperty("notification-receiver-server-port", "3081");
    nrsp = Integer.parseInt(serverPort);

    testMeasurementResults ();
  }

  public String getMeasurement (String host, String metric) {
    try{
      String url =  icingaApiURL +"/objects/services/" + host + "!" + metric;
      ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, url);
      Process connection = icingaApi.start();
      InputStream result = connection.getInputStream();
      BufferedReader content = new BufferedReader (new InputStreamReader (result));
      String data = content.readLine();
      System.out.print (data);
      if (!data.contains("error"))
	return data;
     } catch (Exception e) {
       System.out.print ("Unable to process request");
     }
    return null;
  }

 public List<String> getMeasurementResults (List<String> hostnames, List<String> metrics) {
    List<String> results = new ArrayList<>();
    String result;

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

  void testMeasurementResults () {
    ArrayList<String> hosts = new ArrayList();
    ArrayList<String> metrics = new ArrayList();
    hosts.add("container1");
    hosts.add("localhost");
    metrics.add("ping4");
    metrics.add("disk");

    getMeasurementResults (hosts, metrics);
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
    return null;
  }
  @Override
  public List<String> deletePMJob(List<String> pmJobIdsToDelete) throws MonitoringException {
    return null;
  }

  @Override
  public List<Item> queryPMJob(List<String> hostnames, List<String> metrics, String period)
      throws MonitoringException {
    return null;
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
