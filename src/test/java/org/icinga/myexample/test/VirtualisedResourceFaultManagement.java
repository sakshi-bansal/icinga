package org.icinga.myexample.test;

import org.icinga.myexample.MyPlugin;
import org.openbaton.catalogue.mano.common.monitoring.ObjectSelection;
import org.openbaton.catalogue.mano.common.monitoring.AlarmEndpoint;
import org.openbaton.catalogue.mano.common.monitoring.PerceivedSeverity;
import org.openbaton.catalogue.nfvo.EndpointType;
import org.openbaton.exceptions.MonitoringException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.BeforeClass;

//TODO: Set correct time intervals and test occurences

public class VirtualisedResourceFaultManagement {
  private static MyPlugin myPlugin;
  private static List<String> subscriptionIds;

  @BeforeClass
  public static void init() throws RemoteException, InterruptedException, MonitoringException {
System.out.println("init");
    //myPlugin = new MyPlugin();
    subscriptionIds = new ArrayList();
  }
/*
  @Test
  public void testSubscribeForFault() throws MonitoringException, InterruptedException {
    //Alarm endpoint is the default openbaton FMS
    AlarmEndpoint alarmEndpoint = new AlarmEndpoint("fault-manager-of-container1","container1",
						    EndpointType.REST,"http://localhost:9000/alarm/vr",
						    PerceivedSeverity.WARNING);
    String id = myPlugin.subscribeForFault(alarmEndpoint);
    subscriptionIds.add(id);
  }

  @Test
  public void testUnsubscribeForFault() throws MonitoringException, InterruptedException {
    myPlugin.unsubscribeForFault(subscriptionIds.get(0));
  }

  @Test
  public void testGetAlarmList() throws MonitoringException, InterruptedException {
  }*/
} 
