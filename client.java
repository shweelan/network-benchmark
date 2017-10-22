package nbm.client;

import java.io.*;
import java.net.*;
import java.util.*;
import nbm.message.*;


class Config {
  private static ArrayList<String> hosts = new ArrayList<String>(Arrays.asList("localhost"));
  private static int port = 2912; // default port
  private static int clientsCount = 10; // num of threads
  private static int chunkSize = 1024; // bytes
  private static long delayBetweenChunks = 0; // Milliseconds
  private static long duration = 1000 * 10; // Milliseconds
  private static boolean useDownlink = false;

  public static void print() {
    System.out.println("HOSTS : " + hosts);
    System.out.println("PORT : " + port);
    System.out.println("CLIENTS COUNT : " + clientsCount);
    System.out.println("CHUNK SIZE : " + chunkSize);
    System.out.println("DELAY BETWEEN CHUNKS : " + delayBetweenChunks);
    System.out.println("DURATION : " + duration / 1000); // Seconds
    System.out.println("USE DOWNLINK : " + useDownlink);
  }

  public static void handleCLArgs(String args[]) throws Exception {
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-hosts" :
        case "-h" :
          String[] _hosts = args[++i].split(",");
          if (_hosts.length > 0) {
            hosts.clear();
            for (int j = 0; j < _hosts.length; j++) {
              hosts.add(_hosts[j].trim());
            }
          }
          break;

        case "-port" :
        case "-p" :
          port = Integer.parseInt(args[++i]);
          break;

        case "-clientscount" :
        case "-cc" :
          clientsCount = Integer.parseInt(args[++i]);
          break;

        case "-chunksize" :
        case "-cs" :
          chunkSize = Integer.parseInt(args[++i]);
          break;

        case "-chunkdelay" :
        case "-cd" :
          delayBetweenChunks = Integer.parseInt(args[++i]);
          break;

        case "-duration" :
        case "-d" :
          duration = Integer.parseInt(args[++i]) * 1000; // Milliseconds
          break;

        case "-usedownlink" :
        case "-udl" :
          useDownlink = true;
          break;
      }
    }

  }

  public static ArrayList<String> getHosts() {
    return hosts;
  }

  public static int getPort() {
    return port;
  }

  public static int getClientsCount() {
    return clientsCount;
  }

  public static int getChunkSize() {
    return chunkSize;
  }

  public static long getDelayBetweenChunks() {
    return delayBetweenChunks;
  }

  public static long getDuration() {
    return duration;
  }

  public static boolean getUseDownlink() {
    return useDownlink;
  }
}


class Result {
  private String threadName;
  private long initTs;
  private long startTs;
  private long endTs;
  private long msgSent;

  public Result (String threadName, long initTs, long startTs, long endTs, long msgSent) {
    this.threadName = threadName;
    this.initTs = initTs;
    this.startTs = startTs;
    this.endTs = endTs;
    this.msgSent = msgSent;
  }

  public String getThreadName() {
    return threadName;
  }

  public long getInitTs() {
    return initTs;
  }

  public long getStartTs() {
    return startTs;
  }

  public long getEndTs() {
    return endTs;
  }

  public long getMsgSent() {
    return msgSent;
  }

  public String toString() {
    String[] list = new String[5];
    list[0] = "Thread " + threadName;
    list[1] = "Initiated at " + String.valueOf(initTs);
    list[2] = "Started at " + String.valueOf(startTs);
    list[3] = "Ended at " + String.valueOf(endTs);
    list[4] = "Sent " + String.valueOf(msgSent) + " Messages";
    String str = String.join(", ", list);
    return str;
  }

}

class Results {
  private static List<Result> results = Collections.synchronizedList(new ArrayList<Result>());

  public static void add(Result data) {
    results.add(data);
  }

  public static List<Result> all () {
    return results;
  }
}

class ClientWorker implements Runnable {
  private int port;
  private String host;
  private Socket socket;
  private String id;
  private long initTs;
  private long msgCount = 0;

  public ClientWorker(long initTs, String host) {
    this.initTs = initTs;
    this.port = Config.getPort();
    String[] split = host.split(":");
    if (split.length > 1) {
      this.port = Integer.parseInt(split[1].trim());
    }
    this.host = split[0].trim();
  }

  public void run() {
    long startTs = System.currentTimeMillis();
    long currentTs = startTs;
    try {
      // TODO wait for server (retries)
      this.socket = new Socket(this.host, this.port);
      this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getLocalPort());
      System.out.println("CLient `" + this.id + "` connected to `" + this.host + ":" + this.port + "`!");
      PrintStream outputStream = new PrintStream(this.socket.getOutputStream());
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      String chunk = ((Config.getUseDownlink()) ? "[-usedownlink]" : "") + Message.getChunk(Config.getChunkSize(), 'S');
      long duration = Config.getDuration();
      long delayBetweenChunks = Config.getDelayBetweenChunks();
      long endTs = this.initTs + duration;
      try {
        while((currentTs = System.currentTimeMillis()) < endTs) {
          String id = this.id + '_' + String.valueOf(++this.msgCount);
          Message msg = new Message(id, chunk);
          outputStream.println(msg.toString());
          outputStream.flush();
          /*
          if (this.msgCount % 250 == 0) {
            System.out.println("Client `" + this.id + "` Started @ " + this.initTs + ", Messages Sent : " + msgCount + ", Time Remaining : " + (endTs - currentTs) + " MS!");
          }
          //*/
          /* TODO discus it with prof, how to ensure uniformal resources (cpu time) distribution with other threads
          Thread.sleep(1);
          //*/

          if (delayBetweenChunks > 0) {
            Thread.sleep(delayBetweenChunks);
          }
          // ignoring the input to protect the OS from freezing
          while (inputStream.ready()) {
            inputStream.read();
          }
        }
        outputStream.println("bye");
        outputStream.flush();
      }
      finally {
        inputStream.close();
        outputStream.close();
        this.socket.close();
        System.out.println("Client `" + this.id + "` sent " + this.msgCount + " messages!");
        System.out.println("Client `" + this.id + "` disconnected!");
        Results.add(new Result(Thread.currentThread().getName(), this.initTs, startTs, currentTs, this.msgCount));
      }
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }
}


class Client {
  public static void main(String args[]) throws Exception {
    Config.handleCLArgs(args);
    Config.print();
    int i = 0;
    long start = System.currentTimeMillis();
    // Distribute the load on hosts
    int hostId = 0;
    ArrayList<String> hosts = Config.getHosts();
    Thread[] threads = new Thread[Config.getClientsCount()];
    while(i < Config.getClientsCount()) {
      threads[i] = new Thread(new ClientWorker(start, hosts.get(hostId)), "#" + i);
      threads[i].start();
      if (++hostId == hosts.size()) {
        hostId = 0;
      }
      i++;
    }
    for (Thread thread : threads) {
      thread.join();
    }
    //long end = System.currentTimeMillis();
    long msgsSent = 0;
    for (Result r : Results.all()) {
      msgsSent += r.getMsgSent();
      System.out.println(r);
    }
    float throughput = ((8 * Config.getChunkSize() * msgsSent) / (float) (Config.getDuration() / 1000)) / (float) (1024 * 1024); // 1 byte = 8 bit
    System.out.println("FINAL RESULT : " + Config.getClientsCount() + "," + Config.getDuration() / 1000 + "," + Config.getChunkSize() + "," + msgsSent + "," + throughput);
  }
}
