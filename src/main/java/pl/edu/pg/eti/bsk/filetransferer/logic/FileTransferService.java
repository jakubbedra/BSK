package pl.edu.pg.eti.bsk.filetransferer.logic;

import pl.edu.pg.eti.bsk.filetransferer.data.FileRepository;
import pl.edu.pg.eti.bsk.filetransferer.data.DataTransferRepository;

public class FileTransferService {

    private final FileRepository fileRepository;
    private final DataTransferRepository dataTransferRepository;

    public FileTransferService() {
        this.fileRepository = new FileRepository();
        this.dataTransferRepository = new DataTransferRepository();
    }

    public void sendFile() {

    }

    public void receiveFile() {

    }

    public void sendMessage() {

    }

    public void ReceiveMessage() {

    }

}
