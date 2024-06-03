package servent.message;

import app.AppConfig;
import app.ChordState;

public class AddFriendMessage extends BasicMessage {

    private int chordID;

    private boolean iAlreadyAddedYou;

    public AddFriendMessage(int senderPort, int receiverPort, boolean iAlreadyAddedYou) {
        super(MessageType.WELCOME, senderPort, receiverPort);
        this.chordID = ChordState.chordHash(receiverPort);
        this.iAlreadyAddedYou = iAlreadyAddedYou;
    }

    public int getChordID() {
        return chordID;
    }

    public boolean isiAlreadyAddedYou() {
        return iAlreadyAddedYou;
    }
}
