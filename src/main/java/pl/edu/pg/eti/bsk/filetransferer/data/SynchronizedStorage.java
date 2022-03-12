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

    private Optional<PublicKey> receivedPublicKey;
    private Optional<SecretKey> receivedSessionKey;

    public SynchronizedStorage(KeyPair keyPair, SecretKey sessionKey) {
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        this.sessionKey = sessionKey;
        receivedPublicKey = Optional.empty();
        receivedSessionKey = Optional.empty();
    }

    public synchronized void putReceivedPublicKey(PublicKey receivedPublicKey) {
        this.receivedPublicKey = Optional.of(receivedPublicKey);
    }

    public synchronized void putReceivedSessionKey(SecretKey sessionKey) {
        this.receivedSessionKey = Optional.of(sessionKey);
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

}
