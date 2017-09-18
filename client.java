package nbm.client;

import java.io.*;
import java.net.*;
import java.util.*;
import nbm.message.*;


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
}

class CLientWorker implements Runnable {
  private Socket socket;
  private String id;
  private long startTs;
  private long msgCount = 0;

  public CLientWorker(long startTs) throws Exception {
    this.socket = new Socket(Config.getHost(), Config.getPort());
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getLocalPort());
    this.startTs = startTs;
    System.out.println("CLient `" + this.id + "` connected!");
  }

  public void run() {
    try {
      PrintStream outputStream = new PrintStream(this.socket.getOutputStream());
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      String chunk = Message.getChunk(Config.getChunkSize(), 'S');
      long duration = Config.getDuration();
      long delayBetweenChunks = Config.getDelayBetweenChunks();
      long currentTs;
      long endTs = this.startTs + duration;
      try {
        while((currentTs = System.currentTimeMillis()) < endTs) {
          String id = this.id + '_' + String.valueOf(++this.msgCount);
          Message msg = new Message(currentTs, id, chunk);
          outputStream.println(msg.toString());
          outputStream.flush();
          if (this.msgCount % 250 == 0) {
            System.out.println("Client `" + this.id + "` Started @ !" + this.startTs + ", Messages Sent : " + msgCount + ", Time Remaining : " + (endTs - currentTs) + " MS!");
          }
          if (this.msgCount % 5 == 0) {
            Thread.sleep(Math.max(1, delayBetweenChunks)); // keep the OS alive
          }
          else if (delayBetweenChunks > 0) {
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
        // TODO more informatic statistics
        System.out.println("Client `" + this.id + "` sent " + this.msgCount + " messages!");
        System.out.println("Client `" + this.id + "` disconnected!");
      }
    }
    catch (Exception e) {
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
    long start = System.currentTimeMillis();
    while(i++ < Config.getClientsCount()) {
      new Thread(new CLientWorker(start)).start();
    }
  }
}
