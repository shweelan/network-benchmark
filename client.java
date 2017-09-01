import java.io.*;
import java.net.*;


class Config {
  public static final String host = "localhost";
  public static final int port = 2912;
  public static final int clientsCount = 100;//00;
  public static final int chunkSize = 1024;
  public static final long delayBetweenChunks = 1; // Milliseconds
  public static final long duration = 1000 * 30; // Milliseconds
  // TODO use args as config
}

class CLientWorker implements Runnable {
  private Socket socket;
  private String id;

  public CLientWorker() throws Exception {
    this.socket = new Socket(Config.host, Config.port);
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getLocalPort());
    System.out.println("CLient `" + this.id + "` connected!");
  }

  public void run() {
    try {
      long start = System.currentTimeMillis();
      PrintStream outputStream = new PrintStream(this.socket.getOutputStream());
      //BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      // TODO chunk size, and delays between chunks
      String chunk = "FUCK";
      int i = 0;
      while(System.currentTimeMillis() < start + Config.duration) {
        System.out.println("Client `" + this.id + "` started @ !" + start + ", Time Remaining : " + (Config.duration - (System.currentTimeMillis() - start)) + " MS");
        outputStream.println(chunk);
        outputStream.flush();
        if (i++ % 10 == 0) {
          Thread.sleep(Config.delayBetweenChunks);
        }
      }
      outputStream.println("bye");
      outputStream.flush();
      //inputStream.close();
      outputStream.close();
      this.socket.close();
      System.out.println("Client `" + this.id + "` disconnected!");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}


class Client {
  /* TODO counters
  public static long chunksSent = 0;
  public static long chunksReceived = 0;
  */
  public static void main(String args[]) throws Exception {
    int i = 0;
    while(i++ < Config.clientsCount) {
      new Thread(new CLientWorker()).start();
    }
  }
}
