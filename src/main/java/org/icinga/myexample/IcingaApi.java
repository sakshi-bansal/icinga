package org.icinga.myexample;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IcingaApi {
  private String url;
  private String params;
  private String header;

  public IcingaApi(String url, String params) {
    header = "Accept: application/json";
    this.url = url;
    this.params = params;
  }

  public String sendRequest (ProcessBuilder icingaApi) {
   try{
     Process connection = icingaApi.start();
     InputStream result = connection.getInputStream();
     BufferedReader content = new BufferedReader (new InputStreamReader (result));
     return content.readLine();
    } catch (Exception e) {
       System.out.print ("Unable to process request");
    }
    return null;
  }

  public String allResults () {
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, url);
    return sendRequest(icingaApi);
  }

  public String getCommand (String hostname, String metric) {
    String sendurl = url + hostname + "!" + metric;
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, sendurl);
    return sendRequest(icingaApi);
  }

  public String putCommand (String hostname, String metric, Integer collectionPeriod, int force) {
    String sendurl = url + hostname + "!" + metric;
    String attrs = "{\"templates\":[\"generic-service\"],\"attrs\":{\"check_command\":\""+metric+"\", \"check_interval\":"+collectionPeriod+"}}";
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, "-H", header, "-X", "PUT", sendurl, "-d", attrs);
    String result = sendRequest(icingaApi);
    if (result.contains("does not exist") && force == 1) {
      sendurl = "https://localhost:5665/v1/objects/hosts/" + hostname;
      String check_command = "hostalive";
      attrs = "{\"templates\":[\"generic-host\"],\"attrs\":{\"address\":\""+hostname+"\", \"check_command\":\"hostalive\", \"vars.os\":\"Linux\"}}";
      icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, "-H", header, "-X", "PUT", sendurl, "-d", attrs);
      result = sendRequest(icingaApi);
    } else return result;

    sendurl = url + hostname + "!" + metric;
    attrs = "{\"templates\":[\"generic-service\"],\"attrs\":{\"check_command\":\""+metric+"\", \"check_interval\":"+collectionPeriod+"}}";
    icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, "-H", header, "-X", "PUT", sendurl, "-d", attrs);
    result = sendRequest(icingaApi);
return result;
  }

  public String deleteCommand (String hostname, String metric) {
    String sendurl = url + hostname + "!" + metric;
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, "-H", header, "-X", "DELETE", sendurl);
    return sendRequest(icingaApi);
  }
}
