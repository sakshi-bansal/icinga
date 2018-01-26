package org.icinga.myexample;

import org.openbaton.catalogue.nfvo.EndpointType;
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
import org.openbaton.catalogue.util.IdGenerator;
import org.openbaton.catalogue.mano.common.faultmanagement.VirtualizedResourceAlarmNotification;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

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
  private List<String> pmjobIds = new ArrayList();
  private List<AlarmEndpoint> alarmSubscriptions;
  private List<VRAlarm> vrAlarms;
  private Gson mapper;

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
    alarmSubscriptions = new ArrayList<>();
    vrAlarms = new ArrayList<>();
    mapper = new GsonBuilder().setPrettyPrinting().create();

//test ();
  }

  private PerceivedSeverity getPerceivedSeverity(int triggerSeverity) {
    /* Icinga severity
       1 : WARNING
       2 : CRITICAL
       3 : UNKNOWN
    */
    switch (triggerSeverity) {
      case 1:
        return PerceivedSeverity.WARNING;
      case 2:
        return PerceivedSeverity.CRITICAL;
      case 3:
        return PerceivedSeverity.MINOR;
    }
    return null;
  }

  private List<AlarmEndpoint> getSubscribers(IcingaNotification notification) {
    List<AlarmEndpoint> subscribersForNotification = new ArrayList<>();
    for (AlarmEndpoint ae : alarmSubscriptions) {
      if (notification.getHostname().equals(ae.getResourceId())) {
        subscribersForNotification.add(ae); 
      }
    }
    return subscribersForNotification;
  }

  private HttpResponse<String> restCallWithJson(
      String url, String json, HttpMethod method, String contentType) throws UnirestException {
    HttpResponse<String> response = null;
    response = Unirest.put(url)
                      .header("Content-type", contentType)
                      .header("KeepAliveTimeout", "5000")
                      .body(json)
                      .asString();
    return response;
  }

  @Override
  public List<Alarm> getAlarmList(String vnfId, PerceivedSeverity perceivedSeverity) {
    List<Alarm> alarms = new ArrayList();
    for (VRAlarm vralarm : vrAlarms) {
      if (perceivedSeverity.equals(vralarm.getPerceivedSeverity())) {
        alarms.add((Alarm) vralarm);
      }
    }
    return alarms;
  }

  @Override
  public void notifyFault(AlarmEndpoint endpoint, AbstractVirtualizedResourceAlarm event) {
    try {
        VirtualizedResourceAlarmNotification vran = (VirtualizedResourceAlarmNotification) event;
        String jsonAlarm = mapper.toJson(vran, VirtualizedResourceAlarmNotification.class);
        restCallWithJson(endpoint.getEndpoint(), jsonAlarm, HttpMethod.PUT, "application/json");
    } catch (Exception e) {
      System.out.print ("Unable to send fault notification");
    }
  }

  private VRAlarm createAlarm(IcingaNotification notification) {
    VRAlarm vrAlarm = new VRAlarm();
    vrAlarm.setThresholdId(notification.getTriggerId());
    vrAlarm.setAlarmState(AlarmState.FIRED);
    vrAlarm.setManagedObject(notification.getHostname());
    vrAlarm.setPerceivedSeverity(getPerceivedSeverity(notification.getTriggerSeverity()));

    //EventTime: Time when the fault was observed.
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    vrAlarm.setEventTime(dateFormat.format(date));
    vrAlarms.add(vrAlarm);
    return vrAlarm;
  }

  public void handleNotification (IcingaNotification notification) {
    List<AlarmEndpoint> subscribers = getSubscribers(notification);
    if (subscribers.isEmpty()) {
      return;
    }

    VRAlarm vrAlarm = createAlarm(notification);
    AbstractVirtualizedResourceAlarm alarmNotification =
            new VirtualizedResourceAlarmNotification(vrAlarm.getThresholdId(), vrAlarm);
    for (AlarmEndpoint ae : subscribers) {
      notifyFault(ae, alarmNotification);
    }
  }

  @Override
  public String subscribeForFault(AlarmEndpoint endpoint) throws MonitoringException {
    String subscriptionId = IdGenerator.createId();
    System.out.print("subid " + subscriptionId);
    endpoint.setId(subscriptionId);
    alarmSubscriptions.add(endpoint);
    return subscriptionId;
  }

  @Override
  public String unsubscribeForFault(String alarmEndpointId) {
    Iterator<AlarmEndpoint> iterator = alarmSubscriptions.iterator();
    while (iterator.hasNext()) {
      AlarmEndpoint temp = iterator.next();
      if (temp.getId().equals(alarmEndpointId)) {
        iterator.remove();
        return alarmEndpointId;
      }
    }
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

  //Getting all measurements for all services
  public Item getAllMeasurement () {
    Item data = new Item();
    try{
      String line = icingaApi.allResults ();
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

/*  public void test()  throws RemoteException, MonitoringException{
    AlarmEndpoint alarmEndpoint = new AlarmEndpoint("fault-manager-of-container1","container1",
                                                    EndpointType.REST,"http://localhost:9000/alarm/vr",
                                                    PerceivedSeverity.WARNING);
    String id = subscribeForFault(alarmEndpoint);

    String pmjobId;
    ObjectSelection objectSelection = addObjects("container1");
    List<String> performanceMetrics = addPerformanceMetrics("disk");

    //TODO: set correct durations
    pmjobId = createPMJob(objectSelection, performanceMetrics,
                                   new ArrayList<String>(), 10, 0);
    pmjobIds.add(pmjobId);

    ObjectSelection objectSelection2 = addObjects("container1");
    List<String> performanceMetrics2 = addPerformanceMetrics("dns");

    //TODO: set correct durations
    pmjobId = createPMJob(objectSelection2, performanceMetrics2,
                                   new ArrayList<String>(), 10, 0);
    pmjobIds.add(pmjobId);

//Delete PMJOB id
      List <String> pmjobId2 = new ArrayList();
      //Only testing with first PMJob
      pmjobId2.add(pmjobIds.get(0));
      deletePMJob(pmjobId2);
      pmjobIds.remove(pmjobIds.get(0));
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
  }*/

  public static void main(String[] args)
      throws IOException, InstantiationException, TimeoutException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException, InterruptedException {
    Logger log = LoggerFactory.getLogger(MyPlugin.class);
    PluginStarter.registerPlugin(MyPlugin.class, "icinga", "localhost", 5672, 1);
  }

}
