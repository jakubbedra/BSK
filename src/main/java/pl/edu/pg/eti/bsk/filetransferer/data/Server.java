package pl.edu.pg.eti.bsk.filetransferer.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.println("Server is running on the port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
            System.out.println("Server stopped running.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void awaitTestMessage() {
        try {
            String rcv = in.readLine();
            System.out.println("received message: " + rcv);
            out.println("message received successfully, now go fuck yourself :)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        start();
        while (!Thread.interrupted()) {
            awaitTestMessage();
        }
        stop();
    }

}
