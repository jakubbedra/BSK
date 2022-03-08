package pl.edu.pg.eti.bsk.filetransferer;

import pl.edu.pg.eti.bsk.filetransferer.data.DataSender;
import pl.edu.pg.eti.bsk.filetransferer.data.DataReceiver;
import pl.edu.pg.eti.bsk.filetransferer.data.SynchronizedStorage;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class MessageCli {

    private Thread serverThread;
    private Thread clientThread;

    private KeyPair rsaKeyPair;
    private SecretKey sessionKey;

    private SynchronizedStorage storage;

    private static final int RSA_KEY_SIZE = 2048;
    private static final int SECRET_KEY_SIZE = 256;

    public MessageCli(int port, String connectIp, int connectPort) {
        try {
            rsaKeyPair = EncryptionUtils.generateRsaKeyPair(RSA_KEY_SIZE);
            sessionKey = EncryptionUtils.generateSecretKey(SECRET_KEY_SIZE);
            storage = new SynchronizedStorage(
                    rsaKeyPair, sessionKey
            );
            serverThread = new Thread(new DataReceiver(port, storage, sessionKey));
            clientThread = new Thread(
                    new DataSender(connectIp, connectPort, storage, rsaKeyPair.getPublic(), rsaKeyPair.getPrivate())
            );
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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
