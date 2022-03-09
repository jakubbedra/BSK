package pl.edu.pg.eti.bsk.filetransferer.data;

import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

public class SynchronizedStorage {

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKey sessionKey;

    private String receivedFilesDir;
    private String filesToUploadDir;

    private Optional<PublicKey> receivedPublicKey;
    private Optional<SecretKey> receivedSessionKey;

    private Optional<MessageHeader> header;
    private Optional<String> textMessage;

    //private Optional<String> lastReceivedText;
    //private Optional<String> lastReceivedFile;

    public SynchronizedStorage(KeyPair keyPair, SecretKey sessionKey) {
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        this.sessionKey = sessionKey;
        receivedFilesDir = Constants.DEFAULT_RECEIVED_FILES_DIR;
        filesToUploadDir = Constants.DEFAULT_UPLOAD_FILES_DIR;
        receivedPublicKey = Optional.empty();
        receivedSessionKey = Optional.empty();
        header = Optional.empty();
        textMessage = Optional.empty();
    }

    public synchronized void changeReceivedFilesDir(String receivedFilesDir) {
        this.receivedFilesDir = receivedFilesDir;
    }

    public synchronized void changeFilesToUploadDir(String filesToUploadDir) {
        this.filesToUploadDir = filesToUploadDir;
    }

    public synchronized void putReceivedPublicKey(PublicKey receivedPublicKey) {
        this.receivedPublicKey = Optional.of(receivedPublicKey);
    }

    public synchronized void putReceivedSessionKey(SecretKey sessionKey) {
        this.receivedSessionKey = Optional.of(sessionKey);
    }

    /**
     * Method used to take input metadata for the next message in the form of a header
     */
    public synchronized void putMessageHeader(MessageHeader header) {
        this.header = Optional.of(header);
    }

    public synchronized void putTextMessage(String textMessage) {
        this.textMessage = Optional.of(textMessage);
    }

    public synchronized PublicKey getPublicKey() {
        return publicKey;
    }

    public synchronized PrivateKey getPrivateKey() {
        return privateKey;
    }

    public synchronized SecretKey getSessionKey() {
        return sessionKey;
    }

    public synchronized PublicKey getReceivedPublicKey() {
        while (receivedPublicKey.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return receivedPublicKey.get();
    }

    public synchronized SecretKey getReceivedSessionKey() {
        while (receivedSessionKey.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return receivedSessionKey.get();
    }

    public synchronized String getReceivedFilesDir() {
        return receivedFilesDir;
    }

    public synchronized String getFilesToUploadDir() {
        return filesToUploadDir;
    }

    /**
     * Method for taking (and removing) the last message header
     * (might be changed to a collection later on instead of an optional)
     */
    public synchronized MessageHeader takeMessageHeader() {
        while (header.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        MessageHeader header = this.header.get();
        this.header = Optional.empty();
        return header;
    }

    /**
     * Method for taking (and removing) the last text message
     * (might be changed to a collection later on instead of an optional)
     */
    public synchronized String takeTextMessage() {
        while (textMessage.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String textMessage = this.textMessage.get();
        this.textMessage = Optional.empty();
        return textMessage;
    }

}
