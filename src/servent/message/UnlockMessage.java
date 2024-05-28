package servent.message;

public class UnlockMessage extends BasicMessage{

    private boolean isLockConfirmation;
    public UnlockMessage(int senderPort, int receiverPort, boolean isLockConfirmation) {
        super(MessageType.UNLOCK, senderPort, receiverPort);
        this.isLockConfirmation = isLockConfirmation;
    }

    public boolean isLockConfirmation() {
        return isLockConfirmation;
    }
}
