package nbm.main;

import java.io.*;
import java.net.URLClassLoader;
import java.net.InetAddress;
import java.util.*;


class Main {
  // REGEX from https://stackoverflow.com/questions/5946471/splitting-at-space-if-not-between-quotes
  private static final String CLIENT_CONF_REGEX = "[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
  private static final String SERVER_CONF_ID = new String("SERVER:");
  private static final String CLIENT_CONF_ID = new String("CLIENT:");

  public static void main(String args[]) throws Exception {
    // read config file
    File file = new File(args[0]);
    BufferedReader fileStream = new BufferedReader(new FileReader(file));
    String configLine;
    ArrayList<String> servers = new ArrayList<String>();
    ArrayList<String> clients = new ArrayList<String>();
    while((configLine = fileStream.readLine()) != null) {
      if (configLine.toUpperCase().startsWith(SERVER_CONF_ID)) {
        servers.add(configLine.substring(SERVER_CONF_ID.length()).trim());
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

    ArrayList<Process> localServersProcesses = new ArrayList<Process>();
    // Start the servers
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
        // TODO run localserver
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
        // TODO ssh and run
      }
    }

    // Start the clients
    for (String clientConfig : clients) {
      Process clientProcess = null;
      BufferedReader inputStream = null;
      try {
        List<String> command = new ArrayList<String>();
        command.add(javaExec);
        command.add("-classpath");
        command.add(classPath);
        command.add("nbm.client.Client");
        boolean useAvailableHosts = true;
        for (String conf : clientConfig.split(CLIENT_CONF_REGEX)) {
          command.add(conf);
          if (conf.equals("-h") || conf.equals("-hosts")) {
            useAvailableHosts = false;
          }
        }
        if (useAvailableHosts) {
          command.add("-h");
          command.add(String.join(", ", servers));
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        clientProcess = builder.start();
        inputStream = new BufferedReader(new InputStreamReader(clientProcess.getInputStream()));
        String inputLine;
        while((inputLine = inputStream.readLine()) != null) {
          System.out.println("From client process : " + inputLine);
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

    // TODO kill the servers
    for (Process localServerProcess : localServersProcesses) {
      localServerProcess.destroy();
    }
  }
}
