package org.icinga.myexample;

public class IcingaNotification {

  private String triggerId;
  private String triggerStates; // UP/DOWN
  private String triggerSeverity; // OK/WARNING/CRITICAL/UNKNOWN
  private String hostName;
  private String metricName;
  private int eventId;
  private String eventDate;
  private String eventTime;

  public IcingaNotification() {}

  public String getTriggerId() {
    return triggerId;
  }

  public TriggerStates getTriggerStates() {
    return triggerStates;
  }

  public String getTriggerSeverity() {
    return triggerSeverity;
  }

  public String getMetricName() {
    return itemName;
  }

  public String getHostName() {
    return hostName;
  }

  public int getEventId() {
    return eventId;
  }

  public String getEventDate() {
    return eventDate;
  }

  public String getEventTime() {
    return eventTime;
  }

}
