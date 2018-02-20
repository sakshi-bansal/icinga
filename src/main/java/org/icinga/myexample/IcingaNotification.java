package org.icinga.myexample;

import org.openbaton.catalogue.mano.common.monitoring.*;

public class IcingaNotification {

  private String triggerId;
  private String triggerStates; // UP/DOWN
  private PerceivedSeverity triggerSeverity; // OK/WARNING/CRITICAL/UNKNOWN
  private String hostname;
  private String metricName;
  private int eventId;
  private String eventDate;
  private int alarmType; //0:new alarm, 1:state change alarm
  private String eventTime;

  public IcingaNotification(String triggerId, PerceivedSeverity triggerSeverity,
			    String hostname, String metricName, int alarmType) {
    this.triggerId = triggerId;
    this.triggerSeverity = triggerSeverity;
    this.hostname = hostname;
    this.metricName = metricName;
  }

  public String getTriggerId() {
    return triggerId;
  }

  public PerceivedSeverity getTriggerSeverity() {
    return triggerSeverity;
  }

  public String getHostname() {
    return hostname;
  }

  public String getMetricname() {
    return metricName;
  }

  public int getAlarmType() {
    return alarmType;
  }
}
