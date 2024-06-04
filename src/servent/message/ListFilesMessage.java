package servent.message;

import app.MyFile;

import java.util.List;
import java.util.Map;

public class ListFilesMessage extends BasicMessage{

    private List<MyFile> files;

    public ListFilesMessage(int senderPort, int receiverPort, List<MyFile> files) {
        super(MessageType.ASK_GET, senderPort, receiverPort);
        this.files = files;
    }

    public List<MyFile> getFiles() {
        return files;
    }
}
