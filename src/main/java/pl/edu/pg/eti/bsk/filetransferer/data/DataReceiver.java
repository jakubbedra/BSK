package pl.edu.pg.eti.bsk.filetransferer.data;

import lombok.Setter;
import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DataReceiver implements Runnable {

    private JLabel lastReceivedText;
    private JLabel lastReceivedFile;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    @Setter
    private String receivedFilesDir;

    private int port;

    private SynchronizedStorage storage;

    public DataReceiver(int port, SynchronizedStorage storage, SecretKey sessionKey) {
        this.port = port;
        this.storage = storage;
        receivedFilesDir = Constants.DEFAULT_RECEIVED_FILES_DIR;
//        this.sessionKey = sessionKey;
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
            storage.putReceivedPublicKey(
                    kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes))
            );
            //encode sessionKey with receivedPublicKey
            byte[] encryptedSessionKey = EncryptionUtils.encryptSessionKey(
                    storage.getSessionKey(), storage.getReceivedPublicKey()
            );
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

    public void receiveTextMessage(byte encryptionMethod, IvParameterSpec iv) {
        try {
            String msg = "";
            if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
                //IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode(in.readLine()));
                String rcv = in.readLine();
                byte[] bytes = Base64.getDecoder().decode(rcv);
                msg = new String(
                        EncryptionUtils.decryptAesCbc(bytes, storage.getSessionKey(), iv),
                        StandardCharsets.UTF_8
                );
            } else if (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB) {
                String rcv = in.readLine();
                byte[] bytes = Base64.getDecoder().decode(rcv);
                msg = new String(
                        EncryptionUtils.decryptAesEcb(bytes, storage.getSessionKey()),
                        StandardCharsets.UTF_8
                );
            }
            System.out.println("received message: " + msg);
            lastReceivedText.setText(msg);
            //System.out.println("encrypted message: "+new String(Base64.getDecoder().decode(rcv)));
            out.println("ok");
        } catch (IOException | InvalidKeyException | BadPaddingException |
                NoSuchAlgorithmException | IllegalBlockSizeException |
                NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(String filename, IvParameterSpec iv) {
        try {
            //reading the count
            String countAsString = new String(
                    receiveAndDecryptData(Constants.ENCRYPTION_TYPE_CBC, iv)
            );
            long count = Long.parseLong(countAsString);
            System.out.println(count);

            OutputStream fileOutput = new FileOutputStream(receivedFilesDir + filename);

            byte[] buffer = new byte[Constants.BYTE_BUFFER_SIZE];
            int lengthRead = 0;

            for (long i = 0; i < count; i++) {
                lengthRead = Integer.parseInt(
                        new String(receiveAndDecryptData(Constants.ENCRYPTION_TYPE_CBC, iv), StandardCharsets.UTF_8)
                );
                buffer = receiveAndDecryptData(Constants.ENCRYPTION_TYPE_CBC, iv);
                fileOutput.write(buffer, 0, lengthRead);
                fileOutput.flush();
                System.out.print(i+",");
            }
            fileOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MessageHeader receiveMessageHeader() {
        try {
            return EncryptionUtils.decryptMessageHeader(
                    Base64.getDecoder().decode(in.readLine()), storage.getPrivateKey()
            );
        } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] receiveAndDecryptData(byte encryptionMethod, IvParameterSpec iv) {
        try {
            if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
                byte[] bytes = Base64.getDecoder().decode(in.readLine());
                return EncryptionUtils.decryptAesCbc(bytes, storage.getSessionKey(), iv);
            } else if (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB) {
                byte[] bytes = Base64.getDecoder().decode(in.readLine());
                return EncryptionUtils.decryptAesEcb(bytes, storage.getSessionKey());
            }
        } catch (IOException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void receiveMessage(MessageHeader header) {
        if (header.getMessageType() == Constants.MESSAGE_TYPE_TEXT) {
            receiveTextMessage(header.getEncryptionMethod(), new IvParameterSpec(header.getIv()));
        } else if (header.getMessageType() == Constants.MESSAGE_TYPE_FILE) {
            receiveFile(header.getFilename(), new IvParameterSpec(header.getIv()));
        }
    }

    public void setLabels(JLabel lastReceivedFile, JLabel lastReceivedText) {
        this.lastReceivedFile = lastReceivedFile;
        this.lastReceivedText = lastReceivedText;
    }

    @Override
    public void run() {
        start();
        sendSessionKey();
        while (!Thread.interrupted()) {
            MessageHeader header = receiveMessageHeader();
            receiveMessage(header);
        }
        stop();
    }

}
