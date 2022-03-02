package pl.edu.pg.eti.bsk.filetransferer.data;

import org.junit.Test;
import pl.edu.pg.eti.bsk.filetransferer.MessageCli;

public class P2PTest {

    @Test
    public void testConnection() {
        MessageCli cli = new MessageCli(
                1437, "127.0.0.1", 2137
        );

        cli.startServer();
        System.out.println("test server started");
        cli.connect();
        while (true) {

        }
    }

}
