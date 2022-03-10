package pl.edu.pg.eti.bsk.filetransferer.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class DataSender implements Runnable {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private String ip;
    private int port;

    @Getter
    private AtomicInteger fileUploadProgress;

    /*
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKey receivedSessionKey;
*/

    private SynchronizedStorage storage;

    public DataSender(String ip, int port, SynchronizedStorage storage, PublicKey publicKey, PrivateKey privateKey) {
        this.ip = ip;
        this.port = port;
        this.storage = storage;
        fileUploadProgress = new AtomicInteger(100);
        //        this.publicKey = publicKey;
//        this.privateKey = privateKey;
    }

    public void startConnection() {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //receiving "ok" msg from server
            if (in.readLine().equals("ok")) {
                System.out.println("Client successfully connected to: " + ip + ":" + port);
            } else {
                System.out.println("Internal server error. Program terminated.");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for sending public key and receiving encrypted session key
     */
    private void receiveSessionKey() {
        //out send publicKey
        try {
            System.out.println("Sending public key.");
            out.println(Base64.getEncoder().encodeToString(storage.getPublicKey().getEncoded()));
            byte[] encryptedSessionKey = Base64.getDecoder().decode(in.readLine());
            storage.putReceivedSessionKey(
                    EncryptionUtils.decryptSessionKey(encryptedSessionKey, storage.getPrivateKey())
            );
            System.out.println("Session key received.");
        } catch (NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            System.out.println("Connection to: " + ip + ":" + port + " was closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String sendTextMessage(byte encryptionMethod, String msg, IvParameterSpec iv) {
        try {
            String base64 = "";
            if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
                //IvParameterSpec iv = EncryptionUtils.generateIv();
                //String iv64 = Base64.getEncoder().encodeToString(iv.getIV());
                //out.println(iv64);
                base64 = Base64.getEncoder().encodeToString(
                        EncryptionUtils.encryptAesCbc(
                                msg.getBytes(StandardCharsets.UTF_8),
                                storage.getReceivedSessionKey(),
                                iv
                        ));
            } else {
                base64 = Base64.getEncoder().encodeToString(
                        EncryptionUtils.encryptAesEcb(
                                msg.getBytes(StandardCharsets.UTF_8),
                                storage.getReceivedSessionKey()
                        ));
            }
            out.println(base64);
            return in.readLine();
        } catch (IOException | InvalidKeyException | BadPaddingException |
                NoSuchAlgorithmException | IllegalBlockSizeException |
                NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void sendFile(String path) {
        //sending the iv
        IvParameterSpec iv = EncryptionUtils.generateIv();
        String iv64 = Base64.getEncoder().encodeToString(iv.getIV());
        out.println(iv64);
        //set default path to downloads
        String sampleFile = "C:\\Users\\theKonfyrm\\Desktop\\bsk-files-to-send\\node-v16.13.2-x64.msi";
        sampleFile = path;
        File fileToSend = new File(sampleFile);
        try {
            //sending the filename
            encryptAndSendDataWithSessionKey(
                    fileToSend.getName().getBytes(StandardCharsets.UTF_8),
                    Constants.ENCRYPTION_TYPE_CBC,
                    iv
            );
            //calculating and sending the number of messages that will be sent
            String numberOfMessages = "" + (fileToSend.length() / (long) Constants.BYTE_BUFFER_SIZE);
            encryptAndSendDataWithSessionKey(
                    numberOfMessages.getBytes(StandardCharsets.UTF_8),
                    Constants.ENCRYPTION_TYPE_CBC,
                    iv
            );
            InputStream fileInputStream = new BufferedInputStream(new FileInputStream(fileToSend));

            byte[] buffer = new byte[Constants.BYTE_BUFFER_SIZE];
            int lengthRead = 0;
            while ((lengthRead = fileInputStream.read(buffer)) > 0) {
                //fileOutput.write(buffer, 0, lengthRead);
                encryptAndSendDataWithSessionKey(
                        (lengthRead + "").getBytes(StandardCharsets.UTF_8),
                        Constants.ENCRYPTION_TYPE_CBC,
                        iv
                );
                encryptAndSendDataWithSessionKey(buffer, Constants.ENCRYPTION_TYPE_CBC, iv);
            }
        } catch (IOException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method for sending the message header containing metadata ciphered with received public key
     */
    private void sendMessageHeader(MessageHeader header) {
        ObjectMapper mapper = new ObjectMapper();
        try {
//            String messageHeaderAsString = mapper.writeValueAsString(header);
//            encryptAndSendData(messageHeaderAsString.getBytes(StandardCharsets.UTF_8), "AES/CBC/PKCS5Padding", iv);
            String header64 = Base64.getEncoder().encodeToString(
                    EncryptionUtils.encryptMessageHeader(header, storage.getReceivedPublicKey())
            );
            out.println(header64);
        } catch (JsonProcessingException | NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for encoding and sending data (file and text messages)
     */
    private void encryptAndSendDataWithSessionKey(byte[] data, byte encryptionMethod, IvParameterSpec iv)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            String base64 = Base64.getEncoder().encodeToString(
                    EncryptionUtils.encryptAesCbc(
                            data,
                            storage.getReceivedSessionKey(),
                            iv
                    ));
            out.println(base64);
        } else if (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB) {
            String base64 = Base64.getEncoder().encodeToString(
                    EncryptionUtils.encryptAesEcb(
                            data,
                            storage.getReceivedSessionKey()
                    ));
            out.println(base64);
        }
    }

    public void sendTextMessage(MessageHeader header, String content) {
        sendMessageHeader(header);
        sendTextMessage(header.getEncryptionMethod(), content, new IvParameterSpec(header.getIv()));
    }

    @Override
    public void run() {
        startConnection();
        receiveSessionKey();

        while (!Thread.interrupted()) {
            //do nothing, simply: run
        }
        stopConnection();
    }

}
