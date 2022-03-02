package pl.edu.pg.eti.bsk.filetransferer;

import pl.edu.pg.eti.bsk.filetransferer.data.Client;
import pl.edu.pg.eti.bsk.filetransferer.data.Server;

public class MessageCli {

    private Thread serverThread;
    private Thread clientThread;

    public MessageCli(int port, String connectIp, int connectPort) {
        serverThread = new Thread(new Server(port));
        clientThread = new Thread(new Client(connectIp, connectPort));
    }

    public void startServer() {
        serverThread.start();
    }

    public void connect() {
        clientThread.start();
    }

    public void stopThreads() {
        clientThread.interrupt();
        serverThread.interrupt();
    }

}
