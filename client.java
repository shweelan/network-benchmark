import java.io.*;
import java.net.*;
import java.util.*;


class Config {
  static String host = "localhost";
  static int port = 2912;
  static int clientsCount = 10; // num of threads
  static int chunkSize = 16; // bytes
  static long delayBetweenChunks = 0; // Milliseconds
  static long duration = 1000 * 10; // Milliseconds
  static String chunk = null;

  public static void print() {
    System.out.println("HOST : " + host);
    System.out.println("PORT : " + port);
    System.out.println("CLIENTS COUNT : " + clientsCount);
    System.out.println("CHUNK SIZE : " + chunkSize);
    System.out.println("DELAY BETWEEN CHUNKS : " + delayBetweenChunks);
    System.out.println("DURATION : " + duration / 1000); // Seconds
  }

  public static void handleCLArgs(String args[]) throws Exception {
    for (int i = 0, j = 1; i < args.length - 1; i += 2, j += 2) {
      switch (args[i]) {
        case "-host" :
        case "-h" :
          host = args[j];
          break;

        case "-port" :
        case "-p" :
          port = Integer.parseInt(args[j]);
          break;

        case "-clientscount" :
        case "-cc" :
          clientsCount = Integer.parseInt(args[j]);
          break;

        case "-chunksize" :
        case "-cs" :
          chunkSize = Integer.parseInt(args[j]);
          break;

        case "-chunkdelay" :
        case "-cd" :
          delayBetweenChunks = Integer.parseInt(args[j]);
          break;

        case "-duration" :
        case "-d" :
          duration = Integer.parseInt(args[j]) * 1000; // Milliseconds
          break;
      }
    }

  }

  public static String getHost() {
    return host;
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

  public static String getChunk() {
    if (chunk == null) {
      char[] data = new char[chunkSize];
      Arrays.fill(data, 'S');
      chunk = new String(data);
    }
    return chunk;
  }
}

class CLientWorker implements Runnable {
  private Socket socket;
  private String id;
  private long msgCount = 0;

  public CLientWorker() throws Exception {
    this.socket = new Socket(Config.getHost(), Config.getPort());
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getLocalPort());
    System.out.println("CLient `" + this.id + "` connected!");
  }

  public void run() {
    try {
      long start = System.currentTimeMillis();
      PrintStream outputStream = new PrintStream(this.socket.getOutputStream());
      //BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      String chunk = Config.getChunk();
      long duration = Config.getDuration();
      long delayBetweenChunks = Config.getDelayBetweenChunks();
      while(System.currentTimeMillis() < start + duration) {
        System.out.println("Client `" + this.id + "` started @ !" + start + ", Time Remaining : " + (duration - (System.currentTimeMillis() - start)) + " MS!");
        outputStream.println(chunk);
        outputStream.flush();
        if (++this.msgCount % 5 == 0) {
          Thread.sleep(Math.max(1, delayBetweenChunks)); // keep the OS alive
        }
        else if (delayBetweenChunks > 0) {
          Thread.sleep(delayBetweenChunks);
        }
      }
      outputStream.println("bye");
      outputStream.flush();
      //inputStream.close();
      outputStream.close();
      this.socket.close();
      // TODO more informatic statistics
      System.out.println("Client `" + this.id + "` sent " + this.msgCount + " messages!");
      System.out.println("Client `" + this.id + "` disconnected!");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}


class Client {
  /* TODO more informatic statistics
  public static long chunksSent = 0;
  public static long chunksReceived = 0;
  */
  public static void main(String args[]) throws Exception {
    Config.handleCLArgs(args);
    Config.print();

    int i = 0;
    while(i++ < Config.getClientsCount()) {
      new Thread(new CLientWorker()).start();
    }
  }
}
