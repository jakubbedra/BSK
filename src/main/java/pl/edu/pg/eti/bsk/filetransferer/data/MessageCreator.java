package pl.edu.pg.eti.bsk.filetransferer.data;

import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.spec.IvParameterSpec;

public class MessageCreator {

    private SynchronizedStorage storage;

    public MessageCreator(SynchronizedStorage storage) {
        this.storage = storage;
    }

    public void createTextMessage(String content, byte encryptionMethod) {
        byte[] ivBytes = {};
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            IvParameterSpec iv = EncryptionUtils.generateIv();
            ivBytes = iv.getIV();
        }
        MessageHeader header = new MessageHeader(
                Constants.MESSAGE_TYPE_TEXT,
                encryptionMethod,
                ivBytes,
                content.length(),
                ""
        );
        storage.putMessageHeader(header);
        storage.putTextMessage(content);
    }

    public void createFileMessage(String filename, String fileDir, byte encryptionMethod) {
        storage.changeFilesToUploadDir(fileDir);
        byte[] ivBytes = {};
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            IvParameterSpec iv = EncryptionUtils.generateIv();
            ivBytes = iv.getIV();
        }
        MessageHeader header = new MessageHeader(
                Constants.MESSAGE_TYPE_FILE,
                encryptionMethod,
                ivBytes,
                filename.length(),//hmmmm
                filename
        );
        storage.putMessageHeader(header);
    }

    public void changeReceivedFilesDir(String receivedFilesDir) {
        storage.changeReceivedFilesDir(receivedFilesDir);
    }

}