package pl.edu.pg.eti.bsk.filetransferer.data;

import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DataReceiver implements Runnable {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private int port;

    private PublicKey receivedPublicKey;
    private SecretKey sessionKey;

    public DataReceiver(int port, SecretKey sessionKey) {
        this.port = port;
        this.sessionKey = sessionKey;
    }

    public void start() {
        try {
            System.out.println("Server is running on the port: " + port);
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for receiving public key, encrypting and sending back the session key
     */
    private void sendSessionKey() {
        //in read public key
        try {
            System.out.println("Waiting for public key.");
            byte[] publicKeyBytes = Base64.getDecoder().decode(in.readLine());
            System.out.println("Received public key.");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            receivedPublicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            //encode sessionKey with receivedPublicKey
            byte[] encryptedSessionKey = EncryptionUtils.encryptSessionKey(sessionKey, receivedPublicKey);
            //send back encryptedSessionKey
            out.println(Base64.getEncoder().encodeToString(encryptedSessionKey));
            System.out.println("Session key sent.");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                NoSuchPaddingException e) {
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

    public void receiveTextMessage() {
        try {
            IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode(in.readLine()));
            String rcv = in.readLine();
            byte[] bytes = Base64.getDecoder().decode(rcv);
            String msg = new String(
                    EncryptionUtils.decryptAes("AES/CBC/PKCS5Padding", bytes, sessionKey, iv),
                    StandardCharsets.UTF_8
            );
            System.out.println("received message: " + msg);
            //System.out.println("encrypted message: "+new String(Base64.getDecoder().decode(rcv)));
            out.println("ok");
        } catch (IOException | InvalidKeyException | BadPaddingException |
                NoSuchAlgorithmException | IllegalBlockSizeException |
                NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessageHeader() {

    }

    @Override
    public void run() {
        start();
        sendSessionKey();
        //System.out.println(Base64.getEncoder().encodeToString(sessionKey.getEncoded()));
        while (!Thread.interrupted()) {
            receiveTextMessage();
        }
        stop();
    }

}
