package pl.edu.pg.eti.bsk.filetransferer;

import pl.edu.pg.eti.bsk.filetransferer.data.DataSender;
import pl.edu.pg.eti.bsk.filetransferer.data.DataReceiver;
import pl.edu.pg.eti.bsk.filetransferer.data.MessageHeaderCreator;
import pl.edu.pg.eti.bsk.filetransferer.data.SynchronizedStorage;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class MessageCli {

    private Runnable serverRunnable;
    private Runnable clientRunnable;

    private KeyPair rsaKeyPair;
    private SecretKey sessionKey;

    private SynchronizedStorage storage;

    private MessageHeaderCreator creator;

    private static final int RSA_KEY_SIZE = 2048;
    private static final int SECRET_KEY_SIZE = 256;

    public MessageCli(int port, String connectIp, int connectPort) {
        try {
            rsaKeyPair = EncryptionUtils.generateRsaKeyPair(RSA_KEY_SIZE);
            sessionKey = EncryptionUtils.generateSecretKey(SECRET_KEY_SIZE);
            storage = new SynchronizedStorage(
                    rsaKeyPair, sessionKey
            );
            serverRunnable = new DataReceiver(port, storage, sessionKey);
            clientRunnable = new DataSender(
                    connectIp, connectPort, storage, rsaKeyPair.getPublic(), rsaKeyPair.getPrivate()
            );
            creator = new MessageHeaderCreator(storage);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        Thread serverThread = new Thread(serverRunnable);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void connect() {
        Thread clientThread = new Thread(clientRunnable);
        clientThread.setDaemon(true);
        clientThread.start();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        //while () {
        //}

        while (true) {
            String msg = scanner.nextLine();
            creator.createTextMessageHeader(msg, Constants.ENCRYPTION_TYPE_CBC);
        }
    }

    public void createAndSendTextMessage(String msg, byte encryptionMethod) {
        creator.createTextMessageHeader(msg, encryptionMethod);
    }

    public void stopThreads() {
        //clientThread.interrupt();
        //serverThread.interrupt();
    }

}
