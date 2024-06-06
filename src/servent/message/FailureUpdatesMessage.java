package servent.message;

import app.ServentInfo;

public class FailureUpdatesMessage extends BasicMessage{

    private ServentInfo deadChord;

    public FailureUpdatesMessage(int senderPort, int receiverPort, ServentInfo deadChord) {
        super(MessageType.FAILURE_UPDATE, senderPort, receiverPort);
        this.deadChord = deadChord;
    }

    public ServentInfo getDeadChord() {
        return deadChord;
    }
}
