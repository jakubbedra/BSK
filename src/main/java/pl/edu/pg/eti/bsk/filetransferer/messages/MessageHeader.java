package pl.edu.pg.eti.bsk.filetransferer.messages;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
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
     * Initialization Vector
     */
    private byte[] iv;

    /**
     * Size of the message in bytes
     */
    private long messageSize;

    /**
     * Contains the filename if message type is a file, otherwise empty
     */
    private String filename;

}
