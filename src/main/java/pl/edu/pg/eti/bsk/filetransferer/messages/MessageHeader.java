package pl.edu.pg.eti.bsk.filetransferer.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageHeader {

    /**
     * Type of the message: file, text
     */
    private byte messageType;

    /**
     * Encryption method: ecb, cbc
     */
    private byte encryptionMethod;

    /**
     * Size of the message in bytes (ignored in text messages)
     */
    private long messageSize;

    /**
     * Only for file messages, otherwise empty
     */
    private String fileExtension;

    /**
     * Only for file messages, otherwise empty
     */
    private String filename;

}
