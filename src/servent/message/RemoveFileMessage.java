package servent.message;

import app.ChordState;

public class RemoveFileMessage extends BasicMessage{
    private int key;

    public RemoveFileMessage(int senderPort, int receiverPort, int key) {
        super(MessageType.REMOVE_FILE, senderPort, receiverPort);
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
