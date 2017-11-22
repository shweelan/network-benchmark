package nbm.main;

import java.io.*;
import java.net.URLClassLoader;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;


class Main {
  // REGEX from https://stackoverflow.com/questions/5946471/splitting-at-space-if-not-between-quotes
  private static final String CLIENT_CONF_REGEX = new String("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
  private static final String ALIVE_SERVER_CONF_ID = new String("ALIVE_SERVER:");
  private static final String SERVER_CONF_ID = new String("SERVER:");
  private static final String CLIENT_CONF_ID = new String("CLIENT:");
  private static final int CLIENT_PROCESS_TIMEOUT = 90; //SECONDS

  public static void main(String args[]) throws Exception {
    // read config file
    File file = new File(args[0]);
    BufferedReader fileStream = new BufferedReader(new FileReader(file));
    String configLine;
    ArrayList<String> servers = new ArrayList<String>();
    ArrayList<String> clients = new ArrayList<String>();
    ArrayList<String> aliveServers = new ArrayList<String>();
    while((configLine = fileStream.readLine()) != null) {
      if (configLine.toUpperCase().startsWith(SERVER_CONF_ID)) {
        servers.add(configLine.substring(SERVER_CONF_ID.length()).trim());
      }
      else if (configLine.toUpperCase().startsWith(ALIVE_SERVER_CONF_ID)) {
        aliveServers.add(configLine.substring(ALIVE_SERVER_CONF_ID.length()).trim());
      }
      else if (configLine.toUpperCase().startsWith(CLIENT_CONF_ID)) {
        clients.add(configLine.substring(CLIENT_CONF_ID.length()).trim());
      }
      else {
        System.out.println("Bad Config line, `" + configLine + "` !");
      }
    }
    fileStream.close();

    String javaExec = System.getProperty("java.home") + "/bin/java";
    String classPath = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs()[0].getFile();

    // Start the servers
    ArrayList<Process> localServersProcesses = new ArrayList<Process>();
    HashMap<String, List<String>> remoteServersProcesses = new HashMap<String, List<String>>();

    for (String serverConfig : servers) {
      int port = 0;
      String[] split = serverConfig.split(":");
      String host = split[0].trim();
      if (split.length > 1) {
        port = Integer.parseInt(split[1].trim());
      }
      final InetAddress addr = InetAddress.getByName(host);
      Process serverProcess = null;
      if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
        // run local server
        List<String> command = new ArrayList<String>();
        command.add(javaExec);
        command.add("-classpath");
        command.add(classPath);
        command.add("nbm.server.Server");
        if (port != 0) {
          command.add(String.valueOf(port));
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        serverProcess = builder.start();
        localServersProcesses.add(serverProcess);
      }
      else {
        // run remote server
        List<String> command = new ArrayList<String>();
        // NOTE you need to have jvm installed, project cloned /root, and the project must be compiled using make command
        // NOTE you need to have ssh on port 22 (forced because testing on virtual machine)
        // TODO configurable sshing [username, ssh port, other flags]
        command.add("ssh");
        command.add("ec2-user@" + host);
        command.add("-p");
        command.add("22");
        command.add("cd");
        command.add("/home/ec2-user/network_benchmark"); // TODO configurable working directory
        command.add(";");
        command.add("nohup");
        command.add("java");
        command.add("-classpath");
        command.add("build/");
        command.add("nbm.server.Server");
        if (port != 0) {
          command.add(String.valueOf(port));
        }
        command.add(">>");
        command.add("/tmp/" + port + "_out.log");
        command.add("2>>");
        command.add("/tmp/" + port + "_err.log");
        command.add("<");
        command.add("/dev/null");
        command.add("&");
        command.add("echo");
        command.add("$!");
        ProcessBuilder builder = new ProcessBuilder(command);
        serverProcess = builder.start();
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
        String inputLine = inputStream.readLine();
        if (inputLine != null) {
          try {
            if (!remoteServersProcesses.containsKey(host)) {
              remoteServersProcesses.put(host, new ArrayList<String>());
            }
            // parseInt to validate pid
            remoteServersProcesses.get(host).add(String.valueOf(Integer.parseInt(inputLine)));
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
        inputStream.close();
      }
    }

    // Start the clients
    final String RES_PREFIX = "FINAL RESULT : ";
    long now = System.currentTimeMillis();
    BufferedWriter resWriter = new BufferedWriter(new FileWriter("final_result_" + now + ".csv", true));
    resWriter.write("TestNumber,NumClients,Duration(Sec),MessageSize(Bytes),MessagesSent,Throughput(MegaBits/Sec),");
    resWriter.write("LatencyDruation(Sec),LatencyMessagesSent,MinLatency(MS),MaxLatency(MS),MedianLatency(MS),AverageLatency(MS)");
    resWriter.newLine();
    resWriter.flush();
    String availableHosts = new String("");
    for (String server : servers) {
      if (availableHosts.length() > 0) {
        availableHosts += ",";
      }
      availableHosts += server;
    }
    for (String aliveServer : aliveServers) {
      if (availableHosts.length() > 0) {
        availableHosts += ",";
      }
      availableHosts += aliveServer;
    }
    int clientIndex = 0;
    for (String clientConfig : clients) {
      Process clientProcess = null;
      BufferedReader inputStream = null;
      Thread thread = null;
      try {
        List<String> command = new ArrayList<String>();
        command.add(javaExec);
        command.add("-classpath");
        command.add(classPath);
        command.add("-agentlib:hprof=cpu=samples,depth=25,thread=y,interval=10,file=final_result_cpu_" + now + "_" + ++clientIndex + ".log");
        command.add("nbm.client.Client");
        boolean useAvailableHosts = true;
        for (String conf : clientConfig.split(CLIENT_CONF_REGEX)) {
          command.add(conf);
          if (conf.equals("-h") || conf.equals("-hosts")) {
            useAvailableHosts = false;
          }
        }
        if (useAvailableHosts && availableHosts.length() > 0) {
          command.add("-h");
          command.add(availableHosts);
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        clientProcess = builder.start();
        inputStream = new BufferedReader(new InputStreamReader(clientProcess.getInputStream()));
        final BufferedReader is = inputStream;
        final int ci = clientIndex;
        thread = new Thread(new Runnable() {
          public void run() {
            try {
              String inputLine;
              while((inputLine = is.readLine()) != null) {
                System.out.println("From client process : " + inputLine);
                if (inputLine.startsWith(RES_PREFIX)) {
                  resWriter.write(ci + "," + inputLine.substring(RES_PREFIX.length()));
                  resWriter.newLine();
                  resWriter.flush();
                }
              }
            }
            catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
        thread.start();
        if (!clientProcess.waitFor(CLIENT_PROCESS_TIMEOUT, TimeUnit.SECONDS)) {
          clientProcess.destroy();
          inputStream.close();
          resWriter.write(clientIndex + ",fail,fail,fail,fail,fail,fail,fail,fail,fail,fail,fail");
          resWriter.newLine();
          resWriter.flush();
        }
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
      finally {
        if (clientProcess != null) {
          inputStream.close();
          clientProcess.destroy();
        }
      }
    }
    resWriter.close();

    // kill the servers
    for (Process localServerProcess : localServersProcesses) {
      localServerProcess.destroy();
    }
    for (String key : remoteServersProcesses.keySet()) {
      List<String> command = new ArrayList<String>();
      // NOTE you need to have ssh on port 22 (forced because testing on virtual machine)
      // TODO configurable sshing [username, ssh port, other flags]
      command.add("ssh");
      command.add("ec2-user@" + key);
      command.add("-p");
      command.add("22");
      command.add("kill");
      command.add("-9");
      command.addAll(remoteServersProcesses.get(key));
      (new ProcessBuilder(command)).start();
    }
  }
}
