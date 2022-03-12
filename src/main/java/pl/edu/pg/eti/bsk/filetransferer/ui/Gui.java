package pl.edu.pg.eti.bsk.filetransferer.ui;

import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.data.DataReceiver;
import pl.edu.pg.eti.bsk.filetransferer.data.DataSender;
import pl.edu.pg.eti.bsk.filetransferer.data.MessageHeaderCreator;
import pl.edu.pg.eti.bsk.filetransferer.data.SynchronizedStorage;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;

import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class Gui {

    private Runnable serverRunnable;
    private Runnable clientRunnable;

    private KeyPair rsaKeyPair;
    private SecretKey sessionKey;

    private SynchronizedStorage storage;

    private MessageHeaderCreator creator;

    private String receivedFilesDir;
    private String filesToUploadDir;

    private static final int RSA_KEY_SIZE = 2048;
    private static final int SECRET_KEY_SIZE = 256;

    private byte encryptionMethod;

    private boolean timeTestMode;

    public Gui(boolean timeTestMode) {
        try {
            this.timeTestMode = timeTestMode;
            rsaKeyPair = EncryptionUtils.generateRsaKeyPair(RSA_KEY_SIZE);
            sessionKey = EncryptionUtils.generateSecretKey(SECRET_KEY_SIZE);
            storage = new SynchronizedStorage(
                    rsaKeyPair, sessionKey
            );
            creator = new MessageHeaderCreator(storage);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.encryptionMethod = Constants.ENCRYPTION_TYPE_CBC;
        initStartServerPanel();
    }

    private void initStartServerPanel() {
        JFrame f = new JFrame();

        JTextField t = new JTextField();
        t.setBounds(50, 100, 220, 30);
        f.add(t);

        JLabel l1 = new JLabel("start server at port:");
        l1.setBounds(50, 70, 160, 30);
        f.add(l1);

        JButton b = new JButton("start server");
        b.setBounds(50, 150, 230, 40);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!t.getText().equals("")) {
                    System.out.println(t.getText());
                    serverRunnable = new DataReceiver(Integer.parseInt(t.getText()), storage, sessionKey);
                    Thread serverThread = new Thread(serverRunnable);
                    serverThread.setDaemon(true);
                    serverThread.start();
                    f.setVisible(false);
                    initConnectionPanel();
                }
            }
        });

        f.add(b);

        f.setSize(400, 300);
        f.setLayout(null);
        f.setVisible(true);
    }

    private void initConnectionPanel() {
        JFrame f = new JFrame();

        JTextField t = new JTextField();
        t.setBounds(50, 100, 160, 30);
        f.add(t);

        JTextField t2 = new JTextField();
        t2.setBounds(220, 100, 60, 30);
        f.add(t2);

        JLabel l1 = new JLabel("ip address");
        JLabel l2 = new JLabel("port");
        l1.setBounds(50, 70, 160, 30);
        l2.setBounds(220, 70, 60, 30);
        f.add(l1);
        f.add(l2);

        JButton b = new JButton("connect");
        b.setBounds(50, 150, 230, 40);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!t.getText().equals("") && !t2.getText().equals("")) {
                    System.out.println(t.getText() + ":" + t2.getText());
                    clientRunnable = new DataSender(
                            t.getText(),
                            Integer.parseInt(t2.getText()),
                            storage,
                            rsaKeyPair.getPublic(),
                            rsaKeyPair.getPrivate()
                    );
                    Thread clientThread = new Thread(clientRunnable);
                    clientThread.setDaemon(true);
                    clientThread.start();
                    ((DataSender)clientRunnable).setTestTransferSpeed(timeTestMode);
                    f.setVisible(false);
                    initMessagesGui();
                }
            }
        });

        f.add(b);

        f.setSize(400, 300);
        f.setLayout(null);
        f.setVisible(true);
    }

    private void initMessagesGui() {
        JFrame f = new JFrame();

        JLabel l1 = new JLabel("file:");
        l1.setBounds(10, 10 + 40, 40, 30);
        f.add(l1);

        JTextField t = new JTextField();
        t.setBounds(50, 10 + 40, 200, 30);
        f.add(t);

        JButton b1 = new JButton("SEND");
        b1.setBounds(260, 10 + 40, 80, 30);
        f.add(b1);

        JProgressBar p1 = new JProgressBar();
        p1.setBounds(10, 52 + 40, 330, 20);
        p1.setMaximum(100);
        p1.setValue(0);
        f.add(p1);

        ((DataSender) clientRunnable).setProgressBar(p1);

        JLabel pl = new JLabel("");
        pl.setBounds(10, 74 + 40, 369, 30);
        f.add(pl);

        ((DataSender) clientRunnable).setProgressLabel(pl);

        JLabel l2 = new JLabel("text:");
        l2.setBounds(10, 120 + 40, 40, 30);
        f.add(l2);

        JTextArea a1 = new JTextArea();
        a1.setBounds(50, 120 + 40, 200, 69);
        f.add(a1);

        JButton b2 = new JButton("SEND");
        b2.setBounds(260, 120 + 40, 80, 30);
        f.add(b2);

        b2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = a1.getText();
                System.out.println(msg);
                if (!msg.equals("")) {
                    a1.setText("");
                    ((DataSender) clientRunnable).sendTextMessage(
                            creator.createTextMessageHeader(encryptionMethod),
                            msg
                    );
                }
            }
        });

        JButton b69 = new JButton("Encryption method: CBC");
        b69.setBounds(10, 10, 330, 30);
        f.add(b69);

        b69.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
                    b69.setText("Encryption method: ECB");
                    encryptionMethod = Constants.ENCRYPTION_TYPE_ECB;
                } else if (encryptionMethod == Constants.ENCRYPTION_TYPE_ECB) {
                    b69.setText("Encryption method: CBC");
                    encryptionMethod = Constants.ENCRYPTION_TYPE_CBC;
                }
            }
        });


        JLabel l3 = new JLabel("received files dir:");
        l3.setBounds(10, 200 + 40, 140, 30);
        f.add(l3);

        JTextField t3 = new JTextField(Constants.DEFAULT_RECEIVED_FILES_DIR);
        t3.setBounds(10, 230 + 40, 240, 30);
        f.add(t3);

        ((DataReceiver) serverRunnable).setReceivedFilesDir(Constants.DEFAULT_RECEIVED_FILES_DIR);

        JButton b3 = new JButton("SET");
        b3.setBounds(260, 230 + 40, 80, 30);
        f.add(b3);

        b3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((DataReceiver) serverRunnable).setReceivedFilesDir(t3.getText());
            }
        });

        JLabel l4 = new JLabel("files to upload dir:");
        l4.setBounds(10, 260 + 40, 140, 30);
        f.add(l4);

        JTextField t4 = new JTextField(Constants.DEFAULT_UPLOAD_FILES_DIR);
        t4.setBounds(10, 290 + 40, 240, 30);
        f.add(t4);

        JButton b4 = new JButton("SET");
        b4.setBounds(260, 290 + 40, 80, 30);
        f.add(b4);

        b4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((DataSender) clientRunnable).setFilesToUploadDir(t4.getText());
            }
        });
        JLabel a3 = new JLabel("last received text:");
        a3.setBounds(10, 320 + 40, 200, 30);
        f.add(a3);

        JLabel a2 = new JLabel("nothing to show yet...");
        a2.setBounds(10, 340 + 40, 400, 69);

        f.add(a2);

        JLabel a5 = new JLabel("last received file:");
        a5.setBounds(10, 400 + 40, 200, 69);
        f.add(a5);

        JLabel a4 = new JLabel("nothing to show yet...");
        a4.setBounds(10, 420 + 40, 300, 69);
        f.add(a4);


        b1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = t.getText();
                if (!msg.equals("") && !t4.getText().equals("")) {
                    t.setText("");
                    ((DataSender) clientRunnable).sendFile(
                            creator.createFileMessageHeader(
                                    msg,
                                    encryptionMethod
                            ),
                            msg,
                            f
                    );
                }
            }
        });


        ((DataReceiver) serverRunnable).setLabels(a4, a2);

        f.setSize(400, 560);
        f.setLayout(null);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
