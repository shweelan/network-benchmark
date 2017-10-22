package nbm.server;

import java.net.*;
import java.io.*;
import nbm.common.*;


class ServerWorker implements Runnable {
  private Socket socket;
  private String id;

  public ServerWorker(Socket socket) {
    this.socket = socket;
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getPort());
    System.out.println("Connection `" + this.id + "` connected!");
  }

  public void run() {
    try {
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      PrintStream outputStream = new PrintStream(this.socket.getOutputStream());
      try {
        String inputLine;
        while ((inputLine = inputStream.readLine()) != null) {
          if (inputLine.equals("bye")) {
            break;
          }
          Message msg = new Message(inputLine);
          if(msg.getBody().startsWith("[-usedownlink]")) {
            outputStream.println(inputLine);
            outputStream.flush();
          }
        }
      }
      finally {
        inputStream.close();
        outputStream.close();
        this.socket.close();
        System.out.println("Connection `" + this.id + "` disconnected!");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class Server {
  public static void main(String args[]) throws Exception {
    int port = (args.length > 0 ? Integer.parseInt(args[0]) : 2912);
    ServerSocket server = new ServerSocket(port);
    System.out.println("Server listening on port " + server.getLocalPort());
    while (true) {
      Socket socket = server.accept();
      new Thread(new ServerWorker(socket)).start();
    }
  }
}
