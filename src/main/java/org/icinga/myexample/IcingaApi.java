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

  public String getCommand (String hostname, String metric) {
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, url);
    return sendRequest(icingaApi);
  }

  public String putCommand (String hostname, String metric, Integer collectionPeriod) {
    String sendurl = url + hostname + "!" + metric;
    System.out.println("Create PMJob " + hostname + " " + metric);
    String attrs = "{\"templates\":[\"generic-service\"],\"attrs\":{\"check_command\":\""+metric+"\", \"check_interval\":"+collectionPeriod+"}}";
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, "-H", header, "-X", "PUT", sendurl, "-d", attrs);
    return sendRequest(icingaApi);
  }

  public String deleteCommand (String hostname, String metric) {
    System.out.println("Delete PMJob " + hostname + " " + metric);
    String sendurl = url + hostname + "!" + metric;
    ProcessBuilder icingaApi = new ProcessBuilder("curl", "-k", "-s", "-u", params, "-H", header, "-X", "DELETE", sendurl);
    return sendRequest(icingaApi);
  }
}
