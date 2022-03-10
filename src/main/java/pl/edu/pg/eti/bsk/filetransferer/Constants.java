package pl.edu.pg.eti.bsk.filetransferer;

public class Constants {

    /**
     * Buffer size in bytes
     */
    public static final int BYTE_BUFFER_SIZE = 1024;

    /**
     * Max file size that does not need to be divided
     */
    public static final int MAX_FILE_SIZE_WITHOUT_BUFFER = 1024;

    public static final byte MESSAGE_TYPE_FILE = 21;
    public static final byte MESSAGE_TYPE_TEXT = 37;

    public static final byte ENCRYPTION_TYPE_ECB = 1;
    public static final byte ENCRYPTION_TYPE_CBC = 2;

    public static final String DEFAULT_UPLOAD_FILES_DIR = "C:\\Users\\theKonfyrm\\Desktop\\bsk-files-to-send\\";
    public static final String DEFAULT_RECEIVED_FILES_DIR = "C:\\Users\\theKonfyrm\\Desktop\\bsk-received-files\\";

}
