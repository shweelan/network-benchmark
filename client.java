package nbm.client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import nbm.common.*;


class Config {
  private static ArrayList<String> hosts = new ArrayList<String>(Arrays.asList("localhost"));
  private static int port = 2912; // default port
  private static int clientsCount = 10; // num of threads
  private static int chunkSize = 1024; // bytes
  private static long delayBetweenChunks = 0; // Milliseconds
  private static long duration = 1000 * 10; // Milliseconds
  private static long latencyDruation = 1000 * 10; // Milliseconds
  private static boolean useDownlink = false;

  public static void print() {
    System.out.println("HOSTS : " + hosts);
    System.out.println("PORT : " + port);
    System.out.println("CLIENTS COUNT : " + clientsCount);
    System.out.println("CHUNK SIZE : " + chunkSize);
    System.out.println("DELAY BETWEEN CHUNKS : " + delayBetweenChunks);
    System.out.println("LATENCY DURATION : " + latencyDruation / 1000); // Seconds
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

        case "-latencyduration" :
        case "-ld" :
          latencyDruation = Math.max(1, Integer.parseInt(args[++i])) * 1000; // Milliseconds
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

  public static long getLatencyDruation() {
    return latencyDruation;
  }
}


class Result {
  private String threadName;
  private long duration;
  private long msgSent;
  private long latencyDruation;
  private long minLatency;
  private long maxLatency;
  private long medianLatency;
  private float averageLatency;
  private long latencyMsgSent;

  public Result (String threadName, long duration, long msgSent, long latencyDruation, long minLatency, long maxLatency, long medianLatency, float averageLatency, long latencyMsgSent) {
    this.threadName = threadName;
    this.duration = duration;
    this.msgSent = msgSent;
    this.latencyDruation = latencyDruation;
    this.minLatency = minLatency;
    this.maxLatency = maxLatency;
    this.medianLatency = medianLatency;
    this.averageLatency = averageLatency;
    this.latencyMsgSent = latencyMsgSent;
  }

  public String getThreadName() {
    return threadName;
  }

  public long getDuration() {
    return duration;
  }

  public long getMsgSent() {
    return msgSent;
  }

  public long getLatencyDruation() {
    return latencyDruation;
  }

  public long getMinLatency() {
    return minLatency;
  }

  public long getMaxLatency() {
    return maxLatency;
  }

  public long getMedianLatency() {
    return medianLatency;
  }

  public float getAverageLatency() {
    return averageLatency;
  }

  public long getLatencyMsgSent() {
    return latencyMsgSent;
  }


  public String toString() {
    String[] list = new String[9];
    list[0] = "Thread " + threadName;
    list[1] = "Took " + String.valueOf(duration);
    list[2] = "Messages sent " + String.valueOf(msgSent) + " Messages";
    list[3] = "Latency test took " + String.valueOf(latencyDruation);
    list[4] = "Min latency " + String.valueOf(minLatency);
    list[5] = "Max latency " + String.valueOf(maxLatency);
    list[6] = "Median latency " + String.valueOf(medianLatency);
    list[7] = "Average latency " + String.valueOf(averageLatency);
    list[8] = "Latency messages sent " + String.valueOf(latencyMsgSent);
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
      String latencyChunk = "[-usedownlink]" + Message.getChunk(Config.getChunkSize(), 'S');
      long latencyDruation = Config.getLatencyDruation();
      long endTs = this.initTs + latencyDruation;
      long pings = 0;
      long pongs = 0;
      HashMap<String, Long> roundTripTimes = new HashMap<String, Long>();
      while(true) {
        if ((currentTs = System.currentTimeMillis()) < endTs) {
          String id = this.id + "_LATENCY_" + String.valueOf(++pings);
          Message msg = new Message(id, latencyChunk);
          roundTripTimes.put(id, currentTs);
          outputStream.println(msg.toString());
          outputStream.flush();
        }
        if (pongs < pings) {
          while (inputStream.ready()) {
            String inputLine = inputStream.readLine();
            long resTs = System.currentTimeMillis();
            Message response = new Message(inputLine);
            roundTripTimes.replace(response.getId(), resTs - roundTripTimes.get(response.getId()));
            pongs++;
          }
        } else {
          break;
        }
      }
      List<Long> latencies = new ArrayList<Long>(roundTripTimes.values());
      Collections.sort(latencies);
      long minLatency = latencies.get(0);
      long maxLatency = latencies.get(latencies.size() - 1);
      int mid = latencies.size() / 2;
      long medianLatency = latencies.get(mid) + ((latencies.size() % 2 == 0) ? latencies.get(mid - 1): 0);
      long sum = 0;
      for(Long latency : latencies) {
        sum += latency;
      }
      float averageLatency = sum / (float) latencies.size();
      roundTripTimes.clear();
      latencies.clear();

      System.out.println("LATENCY: min: " + minLatency + ", max: " + maxLatency + ", median: " + medianLatency + ", average: " + averageLatency);

      String chunk = ((Config.getUseDownlink()) ? "[-usedownlink]" : "") + Message.getChunk(Config.getChunkSize(), 'S');
      long duration = Config.getDuration();
      long delayBetweenChunks = Config.getDelayBetweenChunks();
      endTs = this.initTs + latencyDruation + duration;
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
        Results.add(new Result(Thread.currentThread().getName(), duration, this.msgCount, latencyDruation, minLatency, maxLatency, medianLatency, averageLatency, pings));
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
    System.out.print("CONFIG CSV : ");
    System.out.print(Config.getClientsCount() + ",");
    System.out.print(Config.getChunkSize() + ",");
    System.out.print(Config.getDuration() / 1000 + ",");
    System.out.println(Config.getLatencyDruation() / 1000);
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
    long msgSent = 0;
    long minLatency = Long.MAX_VALUE;
    long maxLatency = 0;
    List<Long> medianLatencies = new ArrayList<Long>();
    List<Float> averageLatencies = new ArrayList<Float>();
    long latencyMsgSent = 0;
    for (Result r : Results.all()) {
      msgSent += r.getMsgSent();
      latencyMsgSent += r.getLatencyMsgSent();
      minLatency = Math.min(minLatency, r.getMinLatency());
      maxLatency = Math.max(maxLatency, r.getMaxLatency());
      medianLatencies.add(r.getMedianLatency());
      averageLatencies.add(r.getAverageLatency());
      System.out.println(r);
    }
    Collections.sort(medianLatencies);
    // NOTE this is not overall median; it is median of median.
    int mid = medianLatencies.size() / 2;
    long medianLatency = medianLatencies.get(mid) + ((medianLatencies.size() % 2 == 0) ? medianLatencies.get(mid - 1): 0);
    float sum = 0;
    for(Float avgLatency : averageLatencies) {
      sum += avgLatency;
    }
    float averageLatency = sum / averageLatencies.size();
    float throughput = ((8 * Config.getChunkSize() * msgSent) / (float) (Config.getDuration() / 1000)) / (float) (1024 * 1024); // 1 byte = 8 bit
    System.out.print("FINAL RESULT CSV : ");
    System.out.print(msgSent + ",");
    System.out.print(throughput + ",");
    System.out.print(latencyMsgSent + ",");
    System.out.print(minLatency+ ",");
    System.out.print(maxLatency+ ",");
    System.out.print(medianLatency+ ",");
    System.out.println(averageLatency);
  }
}
