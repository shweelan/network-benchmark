package nbm.main;

import java.io.*;
import java.net.URLClassLoader;
import java.util.*;


class Main {
  // REGEX from https://stackoverflow.com/questions/5946471/splitting-at-space-if-not-between-quotes
  private static final String REGEX = "[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

  public static void main(String args[]) throws Exception {
    File file = new File(args[0]);
    BufferedReader fileStream = new BufferedReader(new FileReader(file));
    String configLine;
    while((configLine = fileStream.readLine()) != null) {
      System.out.println("fdsfdsfsd " + configLine);
      Process clientProcess = null;
      BufferedReader inputStream = null;
      try {
        String javaExec = System.getProperty("java.home") + "/bin/java";
        String classPath = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs()[0].getFile();
        List<String> command = new ArrayList<String>();
        command.add(javaExec);
        command.add("-classpath");
        command.add(classPath);
        command.add("nbm.client.Client");
        for (String conf : configLine.split(REGEX)) {
          command.add(conf);
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
  }
}
