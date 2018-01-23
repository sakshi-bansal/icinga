package org.icinga.myexample;

public class IcingaNotification {

  private String triggerId;
  private int triggerStates; // UP/DOWN
  private int triggerSeverity; // OK/WARNING/CRITICAL/UNKNOWN
  private String hostname;
  private String metricName;
  private int eventId;
  private String eventDate;
  private String eventTime;

  public IcingaNotification() {}

  public String getTriggerId() {
    return triggerId;
  }

  public int getTriggerSeverity() {
    return triggerSeverity;
  }

  public String getHostname() {
    return hostname;
  }
}
