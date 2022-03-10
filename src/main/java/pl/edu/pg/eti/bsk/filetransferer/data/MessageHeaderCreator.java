package pl.edu.pg.eti.bsk.filetransferer.data;

import pl.edu.pg.eti.bsk.filetransferer.Constants;
import pl.edu.pg.eti.bsk.filetransferer.logic.EncryptionUtils;
import pl.edu.pg.eti.bsk.filetransferer.messages.MessageHeader;

import javax.crypto.spec.IvParameterSpec;

public class MessageHeaderCreator {

    private SynchronizedStorage storage;

    public MessageHeaderCreator(SynchronizedStorage storage) {
        this.storage = storage;
    }

    public MessageHeader createTextMessageHeader(String content, byte encryptionMethod) {
        byte[] ivBytes = {};
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            IvParameterSpec iv = EncryptionUtils.generateIv();
            ivBytes = iv.getIV();
        }
        return new MessageHeader(
                Constants.MESSAGE_TYPE_TEXT,
                encryptionMethod,
                ivBytes,
                content.length(),
                ""
        );
    }

    public MessageHeader createFileMessageHeader(String filename, String fileDir, byte encryptionMethod) {
        storage.changeFilesToUploadDir(fileDir);
        byte[] ivBytes = {};
        if (encryptionMethod == Constants.ENCRYPTION_TYPE_CBC) {
            IvParameterSpec iv = EncryptionUtils.generateIv();
            ivBytes = iv.getIV();
        }
        return new MessageHeader(
                Constants.MESSAGE_TYPE_FILE,
                encryptionMethod,
                ivBytes,
                filename.length(),//hmmmm
                filename
        );
    }

}
