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

}
