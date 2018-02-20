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
import org.openbaton.catalogue.mano.common.faultmanagement.VirtualizedResourceAlarmStateChangedNotification;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
  private Map<String, String> triggeredThreshold;
  private Map<String, Threshold> thresholds;
  private Map<String, ScheduledExecutorService> schedulers;

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
    triggeredThreshold = new HashMap<>();
    thresholds = new HashMap<>();
    schedulers = new HashMap<>();

//test ();
testThreshold();
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
    return PerceivedSeverity.INDETERMINATE;
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
        if (event instanceof VirtualizedResourceAlarmNotification) {
          VirtualizedResourceAlarmNotification vran = (VirtualizedResourceAlarmNotification) event;
          String jsonAlarm = mapper.toJson(vran, VirtualizedResourceAlarmNotification.class);
          restCallWithJson(endpoint.getEndpoint(), jsonAlarm, HttpMethod.PUT, "application/json");
        } else if (event instanceof VirtualizedResourceAlarmStateChangedNotification) {
          VirtualizedResourceAlarmStateChangedNotification vrascn =
            (VirtualizedResourceAlarmStateChangedNotification) event;
          String jsonAlarm = mapper.toJson(vrascn, VirtualizedResourceAlarmStateChangedNotification.class);
          restCallWithJson(endpoint.getEndpoint(), jsonAlarm, HttpMethod.PUT, "application/json");
	}

    } catch (Exception e) {
    }
  }

  private VRAlarm createAlarm(IcingaNotification notification) {
    VRAlarm vrAlarm = new VRAlarm();
    vrAlarm.setThresholdId(notification.getTriggerId());
    vrAlarm.setAlarmState(AlarmState.FIRED);
    vrAlarm.setManagedObject(notification.getHostname());
    vrAlarm.setPerceivedSeverity(notification.getTriggerSeverity());

    //EventTime: Time when the fault was observed.
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    vrAlarm.setEventTime(dateFormat.format(date));
    vrAlarms.add(vrAlarm);
    return vrAlarm;
  }

  public void handleNotification (IcingaNotification notification) {
    AbstractVirtualizedResourceAlarm alarmNotification;
    List<AlarmEndpoint> subscribers = getSubscribers(notification);
    if (subscribers.isEmpty()) {
      return;
    }

    if (notification.getAlarmType() == 0) {
      VRAlarm vrAlarm = createAlarm(notification);
      alarmNotification = new VirtualizedResourceAlarmNotification(vrAlarm.getThresholdId(), vrAlarm);
    } else {
      AlarmState alarmState = AlarmState.CLEARED;
      alarmNotification = new VirtualizedResourceAlarmStateChangedNotification(notification.getTriggerId(), alarmState);
    }

    for (AlarmEndpoint ae : subscribers) {
      notifyFault(ae, alarmNotification);
    }
  }

  @Override
  public String subscribeForFault(AlarmEndpoint endpoint) throws MonitoringException {
    String subscriptionId = IdGenerator.createId();
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
    int force = Integer.parseInt(performanceMetrics.get(performanceMetrics.size() - 1));
    try {
      for (String host : objectSelection.getObjectInstanceIds()) {
        for (String metric : performanceMetrics) {
          String result = icingaApi.putCommand (host, metric, collectionPeriod, force);
          if (result == null) {
	    System.out.println ("Unable to create PMJob with hostname " + host + " and metric " + metric);
          }
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

  private boolean isNewNotification (String thresholdId) {
    if (triggeredThreshold.get(thresholdId) == null)
	return true;
    return false;
  }

  private int getState (String result) {
    String key = null;
    String value = null;
    int state = -1;

    try {
      JSONObject jsonobj = new JSONObject(result);
      JSONArray getArray = jsonobj.getJSONArray("results");
      jsonobj = getArray.getJSONObject(0);
      Iterator<String> keys = jsonobj.keys();
      while(keys.hasNext()){
         key = keys.next();
         value = jsonobj.getString(key);
      }

      jsonobj = new JSONObject(value);
      keys = jsonobj.keys();
      while(keys.hasNext()){
        key = keys.next();
	if (key.equals("state")) {
          state = Integer.parseInt(jsonobj.getString(key));
          return state;
	}
      }
    } catch (JSONException e) {
      System.out.println("Unable to get the status");
    }
    return state;
  }

  public class CheckThreshold implements Runnable {
    private String hostname;
    private String performanceMetric;
    private Threshold threshold;
    private int flag;

    public CheckThreshold (String hostname, String performanceMetric,
                           Threshold threshold) {
      this.hostname = hostname;
      this.performanceMetric = performanceMetric;
      this.threshold = threshold;
      flag = 1;
    }
   
    @Override
    public void run() {
    	  Item result = new Item();
    	  IcingaNotification icingaNotification;
    	  PerceivedSeverity perceivedSeverity = null;
    	  String thresholdId = threshold.getThresholdId();
    	  PerceivedSeverity thresholdPerceivedSeverity = threshold.getThresholdDetails().getPerceivedSeverity();
    	  result = getMeasurement(hostname, performanceMetric);
	  int state = getState(result.getValue());
          perceivedSeverity = getPerceivedSeverity(state);
    	  if (perceivedSeverity.equals(thresholdPerceivedSeverity) &&
	      isNewNotification(thresholdId)) {
            triggeredThreshold.put(thresholdId, hostname);
	    icingaNotification = new IcingaNotification(thresholdId, perceivedSeverity,
                                                        hostname, performanceMetric, 0);
            handleNotification(icingaNotification);
	    flag = 1;
          } else if (!isNewNotification(thresholdId) && state == 0 && flag == 1) {
            icingaNotification = new IcingaNotification(thresholdId, perceivedSeverity,
                                                        hostname, performanceMetric, 1);
	    handleNotification(icingaNotification);
	    flag = 0;
          }
    }
  }

  @Override
  public String createThreshold(
      ObjectSelection objectSelector,
      String performanceMetric,
      ThresholdType thresholdType,
      ThresholdDetails thresholdDetails)
      throws MonitoringException {
    if (objectSelector == null)
      throw new MonitoringException("The objectSelector is null or empty");
    if ((performanceMetric == null && performanceMetric.isEmpty()))
      throw new MonitoringException("The performanceMetric needs to be present");
    if (thresholdDetails == null) throw new MonitoringException("The thresholdDetails is null");

    Threshold threshold;
    threshold = new Threshold(objectSelector, performanceMetric, thresholdType, thresholdDetails);
    List<String> hostnames = objectSelector.getObjectInstanceIds();
    String hostname = hostnames.get(0);

    threshold.setThresholdId(IdGenerator.createUUID());

    CheckThreshold checkThreshold = new CheckThreshold(hostname, performanceMetric, threshold);
    checkThreshold.run();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(checkThreshold, 3, 3, TimeUnit.SECONDS);
    thresholds.put(threshold.getThresholdId(), threshold);
    schedulers.put(threshold.getThresholdId(), scheduler);
    return threshold.getThresholdId();
  }

  void shutdown(ExecutorService pool) {
    pool.shutdown();
    try {
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public List<String> deleteThreshold(List<String> thresholdIds) throws MonitoringException {
    if (thresholdIds == null) throw new MonitoringException("The list of thresholdIds ids is null");
    if (thresholdIds.isEmpty()) throw new MonitoringException("The list of thresholdIds is empty");
    List<String> thresholdIdsDeleted = new ArrayList<>();
    try {
      for (String thresholdIdToDelete : thresholdIds) {
        Threshold thresholdToDelete = thresholds.get(thresholdIdToDelete);
        if (thresholdToDelete != null) {
	  triggeredThreshold.remove(thresholdIdToDelete);
          thresholds.remove(thresholdIdToDelete);
	  ScheduledExecutorService scheduler = schedulers.get(thresholdIdToDelete);
 	  shutdown(scheduler);
	  thresholdIdsDeleted.add(thresholdIdToDelete);
        }
      }
    } catch (Exception e) {
      throw new MonitoringException("The thresholds cannot be deleted: " + e.getMessage(), e);
    }
    return thresholdIdsDeleted;
  }

  @Override
  public void queryThreshold(String queryFilter) {
    if (triggeredThreshold.get(queryFilter) == null) {
      System.out.println ("Threshold has been triggered");
    } else if (thresholds.get(queryFilter) == null) {
      System.out.println ("Threshold does not exists");
    }
  }

  public void testThreshold()  throws RemoteException, MonitoringException{
        subscribeForFault(
        new AlarmEndpoint(
            "faults-consumer",
            null,
            EndpointType.REST,
            "http://localhost:9000/alarm/vr",
            PerceivedSeverity.MINOR));

    AlarmEndpoint alarmEndpoint = new AlarmEndpoint("fault-manager-of-container1","container1",
                                                    EndpointType.REST,"http://localhost:9000/alarm/vr",
                                                    PerceivedSeverity.WARNING);
    String id = subscribeForFault(alarmEndpoint);

    ObjectSelection objectSelector = addObjects("container1");
    ThresholdDetails thresholdDetails =
        new ThresholdDetails("last(0)", "=", PerceivedSeverity.CRITICAL, "0", "|");
    thresholdDetails.setPerceivedSeverity(PerceivedSeverity.CRITICAL);
    String thresholdId = createThreshold(
            objectSelector, "ping4", ThresholdType.SINGLE_VALUE, thresholdDetails);

//    List<String> thresholdIdsToDelete = new ArrayList<>();
 //   thresholdIdsToDelete.add(thresholdId);

   // List<String> thresholdIdsDeleted = deleteThreshold(thresholdIdsToDelete);
}
/*
  public void test()  throws RemoteException, MonitoringException{
    AlarmEndpoint alarmEndpoint = new AlarmEndpoint("fault-manager-of-container1","container1",
                                                    EndpointType.REST,"http://localhost:9000/alarm/vr",
                                                    PerceivedSeverity.WARNING);
    String id = subscribeForFault(alarmEndpoint);

    String pmjobId;
    //ObjectSelection objectSelection = addObjects("172.17.0.2");
    //ObjectSelection objectSelection = addObjects("172.17.0.2", "172.17.0.3", "172.17.0.4", "172.17.0.5", "172.17.0.6");

   // ObjectSelection objectSelection = addObjects("172.17.0.2", "172.17.0.3", "172.17.0.4", "172.17.0.5", "172.17.0.6", "172.17.0.7", "172.17.0.8", "172.17.0.9", "172.17.0.10", "172.17.0.11");

    //ObjectSelection objectSelection = addObjects("172.17.0.2", "172.17.0.3", "172.17.0.4", "172.17.0.5", "172.17.0.6", "172.17.0.7", "172.17.0.8", "172.17.0.9", "172.17.0.10", "172.17.0.11", "172.17.0.12", "172.17.0.13", "172.17.0.14", "172.17.0.15", "172.17.0.16");

    //ObjectSelection objectSelection = addObjects("172.17.0.2", "172.17.0.3", "172.17.0.4", "172.17.0.5", "172.17.0.6", "172.17.0.7", "172.17.0.8", "172.17.0.9", "172.17.0.10", "172.17.0.11", "172.17.0.12", "172.17.0.13", "172.17.0.14", "172.17.0.15", "172.17.0.16", "172.17.0.17", "172.17.0.18", "172.17.0.19", "172.17.0.20", "172.17.0.21");

    //List<String> performanceMetrics = addPerformanceMetrics("ping4", "1");
    //List<String> performanceMetrics = addPerformanceMetrics("ping4", "tcp", "udp", "ssl", "disk", "mem", "swap", "procs", "ssh", "users", "1");

    //TODO: set correct durations
    //pmjobId = createPMJob(objectSelection, performanceMetrics,
      //                             new ArrayList<String>(), 10, 0);
    //pmjobIds.add(pmjobId);

    /*ObjectSelection objectSelection2 = addObjects("container1");
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

    long timestamp = System.currentTimeMillis();
    System.out.println ("Starting time " + timestamp);

    List<String> hostnames = new ArrayList();
    List<String> metrics = new ArrayList();
    hostnames.add("172.17.0.2");
    hostnames.add("172.17.0.3");
    hostnames.add("172.17.0.4");
    hostnames.add("172.17.0.5");
    hostnames.add("172.17.0.6");
    hostnames.add("172.17.0.7");
    hostnames.add("172.17.0.8");
    hostnames.add("172.17.0.9");
    hostnames.add("172.17.0.10");
    hostnames.add("172.17.0.11");
    hostnames.add("172.17.0.12");
    hostnames.add("172.17.0.13");
    hostnames.add("172.17.0.14");
    hostnames.add("172.17.0.15");
    hostnames.add("172.17.0.16");
    hostnames.add("172.17.0.17");
    hostnames.add("172.17.0.18");
    hostnames.add("172.17.0.19");
    hostnames.add("172.17.0.20");
    hostnames.add("172.17.0.21");
    metrics.add("ping4");
    metrics.add("tcp");
    metrics.add("udp");
    metrics.add("ssl");
    metrics.add("ssh");
    metrics.add("mem");
    metrics.add("disk");
    metrics.add("procs");
    metrics.add("users");
    metrics.add("swap");
    String period = "0";
    queryPMJob (hostnames, metrics, period);
    timestamp = System.currentTimeMillis();
    System.out.println ("Ending time " + timestamp);
  }
*/ 
  private ObjectSelection addObjects(String... args) {
    ObjectSelection objectSelection  = new ObjectSelection();
    for (String arg : args) {
      objectSelection.addObjectInstanceId(arg);
    }
    return objectSelection;
  }
/*
  private List<String> addPerformanceMetrics(String... args) {
    List<String> performanceMetrics = new ArrayList<>();
    for (String arg : args) {
      performanceMetrics.add(arg);
    }
    return performanceMetrics;
  }
*/
  public static void main(String[] args)
      throws IOException, InstantiationException, TimeoutException, IllegalAccessException,
          InvocationTargetException, NoSuchMethodException, InterruptedException {
    Logger log = LoggerFactory.getLogger(MyPlugin.class);
    PluginStarter.registerPlugin(MyPlugin.class, "icinga", "localhost", 5672, 1);
  }

}
