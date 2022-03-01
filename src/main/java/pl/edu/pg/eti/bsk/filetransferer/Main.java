package pl.edu.pg.eti.bsk.filetransferer;

import pl.edu.pg.eti.bsk.filetransferer.data.Server;

public class Main {

    public static void main(String[] args) {
        System.out.println("dupa");
        Server server = new Server();
        server.start(6666);
        server.awaitTestMessage();
    }

}
