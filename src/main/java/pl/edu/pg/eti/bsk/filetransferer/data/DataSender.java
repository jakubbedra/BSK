package pl.edu.pg.eti.bsk.filetransferer.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;

public class DataSender implements Runnable {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private String ip;
    private int port;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKey receivedSessionKey;

    public DataSender(String ip, int port, PublicKey publicKey, PrivateKey privateKey) {
        this.ip = ip;
        this.port = port;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
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
            out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
            byte[] encryptedSessionKey = Base64.getDecoder().decode(in.readLine());
            receivedSessionKey = EncryptionUtils.decryptSessionKey(encryptedSessionKey, privateKey);
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

    private String sendTextMessage(byte encryptionMethod, String msg) {
        try {
            String base64 = "";
            if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
                IvParameterSpec iv = EncryptionUtils.generateIv();
                String iv64 = Base64.getEncoder().encodeToString(iv.getIV());
                out.println(iv64);
                base64 = Base64.getEncoder().encodeToString(
                        EncryptionUtils.encryptAesCbc(
                                msg.getBytes(StandardCharsets.UTF_8),
                                receivedSessionKey,
                                iv
                        ));
            } else {
                base64 = Base64.getEncoder().encodeToString(
                        EncryptionUtils.encryptAesEcb(
                                msg.getBytes(StandardCharsets.UTF_8),
                                receivedSessionKey
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
        File fileToSend = new File(sampleFile);
        try {
            //sending the filename
            encryptAndSendData(
                    fileToSend.getName().getBytes(StandardCharsets.UTF_8),
                    Constants.ENCRYPTION_TYPE_CBC,
                    iv
            );
            //calculating and sending the number of messages that will be sent
            String numberOfMessages = "" + (fileToSend.length() / (long) Constants.BYTE_BUFFER_SIZE);
            encryptAndSendData(
                    numberOfMessages.getBytes(StandardCharsets.UTF_8),
                    Constants.ENCRYPTION_TYPE_CBC,
                    iv
            );
            InputStream fileInputStream = new BufferedInputStream(new FileInputStream(fileToSend));

            byte[] buffer = new byte[Constants.BYTE_BUFFER_SIZE];
            int lengthRead = 0;
            while ((lengthRead = fileInputStream.read(buffer)) > 0) {
                //fileOutput.write(buffer, 0, lengthRead);
                encryptAndSendData(
                        (lengthRead + "").getBytes(StandardCharsets.UTF_8),
                        Constants.ENCRYPTION_TYPE_CBC,
                        iv
                );
                encryptAndSendData(buffer, Constants.ENCRYPTION_TYPE_CBC, iv);
            }
        } catch (IOException | InvalidAlgorithmParameterException |
                NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method for sending the message header containing metadata
     */
    private void sendMessageHeader(MessageHeader header) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String messageHeaderAsString = mapper.writeValueAsString(header);
//            encryptAndSendData(messageHeaderAsString.getBytes(StandardCharsets.UTF_8), "AES/CBC/PKCS5Padding", iv);
            String header64 = Base64.getEncoder().encodeToString(
                    messageHeaderAsString.getBytes(StandardCharsets.UTF_8)
            );
            out.println(header64);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for encoding and sending data
     */
    private void encryptAndSendData(byte[] data, byte encryptionMethod, IvParameterSpec iv)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            String base64 = Base64.getEncoder().encodeToString(
                    EncryptionUtils.encryptAesCbc(
                            data,
                            receivedSessionKey,
                            iv
                    ));
            out.println(base64);
        } else if (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB) {
            String base64 = Base64.getEncoder().encodeToString(
                    EncryptionUtils.encryptAesEcb(
                            data,
                            receivedSessionKey
                    ));
            out.println(base64);
        }
    }

    @Override
    public void run() {
        System.out.println("press any key to connect");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        startConnection();
        receiveSessionKey();

        //System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        /*
            ogolnie bedzie to tak zrobione ze przekazujemy za pomoca
            jakiegos sprytnego obiektu (synchronized collection czy cos i sprawdzamy czy cos sie pojawilo tam)
            i na podstawie tego albo sendTextMessage() albo sendFile()
            najpierw wgl wysylamy MessageHeader, mozemy go przerobic na .json tak wsm
            i na podstawie tego DataReceiver dowiaduje sie czy ma do czynienia z file czy text
         */
        while (!Thread.interrupted()) {
            String msg = scanner.nextLine();
            MessageHeader header = new MessageHeader(
                    Constants.MESSAGE_TYPE_TEXT,
                    Constants.ENCRYPTION_TYPE_CBC,
                    0,
                    "",
                    ""
            );
            sendMessageHeader(header);
            //sendTextMessage(header.getEncryptionMethod(), msg);
            sendFile("C:\\Users\\theKonfyrm\\Desktop\\bsk-files-to-send\\node-v16.13.2-x64.msi");
            //todo: choose message type
        }
        stopConnection();
    }

}
