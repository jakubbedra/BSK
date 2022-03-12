package pl.edu.pg.eti.bsk.filetransferer.data;

import lombok.Setter;
import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class DataSender implements Runnable {

    @Setter
    private JProgressBar progressBar;
    @Setter
    private JLabel progressLabel;

    private Socket clientSocket;

    private OutputStream out;
    private InputStream in;

    private String ip;
    private int port;

    private SynchronizedStorage storage;

    @Setter
    private String filesToUploadDir;

    @Setter
    private boolean testTransferSpeed;

    public DataSender(String ip, int port, SynchronizedStorage storage, PublicKey publicKey, PrivateKey privateKey) {
        this.ip = ip;
        this.port = port;
        this.storage = storage;
        filesToUploadDir = Constants.DEFAULT_UPLOAD_FILES_DIR;
        testTransferSpeed = false;
    }

    public void startConnection() {
        try {
            clientSocket = new Socket(ip, port);
            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();

            //receiving "ok" msg from server
            byte[] rcv = new byte[Constants.BYTE_BUFFER_SIZE];
            in.read(rcv, 0, 2);
            if ((new String(rcv, 0, 2)).equals("ok")) {
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
        try {
            System.out.println("Sending public key.");
            int length = storage.getPublicKey().getEncoded().length;
            writeInt(length);
            out.write(storage.getPublicKey().getEncoded(), 0, length);
            out.flush();
            length = readInt();
            byte[] encryptedSessionKey = new byte[length];
            in.read(encryptedSessionKey, 0, length);
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

    private void sendTextMessage(byte encryptionMethod, String msg, IvParameterSpec iv) {
        try {
            if(testTransferSpeed){
                System.out.println("Sending text message using " +
                        (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB ? "ECB" : "CBC")
                        + " encryption.");
            }
            long before = System.currentTimeMillis();
            byte[] data = null;
            if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
                data = EncryptionUtils.encryptAesCbc(
                        msg.getBytes(StandardCharsets.UTF_8),
                        storage.getReceivedSessionKey(),
                        iv
                );
            } else {
                data = EncryptionUtils.encryptAesEcb(
                        msg.getBytes(StandardCharsets.UTF_8),
                        storage.getReceivedSessionKey()
                );
            }
            writeInt(data.length);
            out.write(data, 0, data.length);
            in.read(data, 0, 2);
            if (testTransferSpeed) {
                long now = System.currentTimeMillis();
                System.out.println("Message transferred in: " + (now - before) + "ms");
            }
        } catch (IOException | InvalidKeyException | BadPaddingException |
                NoSuchAlgorithmException | IllegalBlockSizeException |
                NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String filename, IvParameterSpec iv, byte encryptionMethod) {
        try {
            File fileToSend = new File(filesToUploadDir + filename);
            //calculating and sending the number of messages that will be sent
            System.out.println((double)fileToSend.length());
            System.out.println((double) Constants.BYTE_BUFFER_SIZE);
            System.out.println(((long)(Math.ceil((double)fileToSend.length() / (double) Constants.BYTE_BUFFER_SIZE))));
            String numberOfMessagesAsString = "" + ((long)(Math.ceil((double)fileToSend.length() / (double) Constants.BYTE_BUFFER_SIZE)));
            encryptAndSendDataWithSessionKey(
                    numberOfMessagesAsString.getBytes(StandardCharsets.UTF_8),
                    encryptionMethod,
                    iv
            );
            long begin = System.currentTimeMillis();
            InputStream fileInputStream = new BufferedInputStream(new FileInputStream(fileToSend));

            if (testTransferSpeed) {
                System.out.println("Sending file using " +
                        (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB ? "ECB" : "CBC")
                        + " encryption.");
            }

            byte[] buffer = new byte[Constants.BYTE_BUFFER_SIZE];
            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    int lengthRead = 0;
                    long sentMessages = 0;
                    progressBar.setValue(0);
                    progressBar.setMaximum(100);
                    progressLabel.setText("Sending " + filename + ": 0%");

                    while ((lengthRead = fileInputStream.read(buffer)) > 0) {
                        encryptAndSendDataWithSessionKey(
                                (lengthRead + "").getBytes(StandardCharsets.UTF_8),
                                encryptionMethod,
                                iv
                        );
                        encryptAndSendDataWithSessionKey(buffer, encryptionMethod, iv);
                        sentMessages++;
                        double numberOfMessages = (double) sentMessages / (double) (fileToSend.length() / (long) Constants.BYTE_BUFFER_SIZE);
                        int pbValue = (int) (numberOfMessages * 100.0);
                        progressBar.setValue(pbValue);
                        progressLabel.setText("Sending " + filename + ": " + pbValue + "%");
                        publish(pbValue);
                    }
                    progressBar.setValue(100);
                    progressLabel.setText("Sending " + filename + ": 100%");
                    if (testTransferSpeed) {
                        System.out.println("File transferred in: " + (System.currentTimeMillis() - begin) + "ms");
                    }
                    return null;
                }
            };
            worker.execute();
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
        try {
            byte[] headerBytesEncrypted = EncryptionUtils.encryptMessageHeader(header, storage.getReceivedPublicKey());
            writeInt(headerBytesEncrypted.length);
            out.write(headerBytesEncrypted);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                BadPaddingException | InvalidKeyException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for encoding and sending data (file and text messages)
     */
    private void encryptAndSendDataWithSessionKey(byte[] data, byte encryptionMethod, IvParameterSpec iv)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException, IOException {
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            byte[] encrypted = EncryptionUtils.encryptAesCbc(
                    data,
                    storage.getReceivedSessionKey(),
                    iv
            );
            writeInt(encrypted.length);
            out.write(encrypted, 0, encrypted.length);
        } else if (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB) {
            byte[] encrypted = EncryptionUtils.encryptAesEcb(
                    data,
                    storage.getReceivedSessionKey()
            );
            writeInt(encrypted.length);
            out.write(encrypted, 0, encrypted.length);
        }
    }

    public void sendTextMessage(MessageHeader header, String content) {
        sendMessageHeader(header);
        sendTextMessage(header.getEncryptionMethod(), content, new IvParameterSpec(header.getIv()));
    }

    public void sendFile(MessageHeader header, String filename, JFrame f) {
        sendMessageHeader(header);
        sendFile(filename, new IvParameterSpec(header.getIv()), header.getEncryptionMethod());
    }

    @Override
    public void run() {
        startConnection();
        receiveSessionKey();
    }

    private int readInt() throws IOException {
        byte[] intBytes = new byte[4];
        in.read(intBytes, 0, 4);
        ByteBuffer wrapped = ByteBuffer.wrap(intBytes, 0, 4);
        return wrapped.getInt();
    }

    private void writeInt(int i) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        byte[] intBytes = buffer.array();
        out.write(intBytes, 0, 4);
    }

}
