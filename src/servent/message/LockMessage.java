package servent.message;

public class LockMessage extends BasicMessage{

    private boolean isLockConfirmation;

    public LockMessage(int senderPort, int receiverPort, boolean isLockConfirmation) {
        super(MessageType.LOCK, senderPort, receiverPort);
        this.isLockConfirmation = isLockConfirmation;
    }

    public boolean isLockConfirmation() {
        return isLockConfirmation;
    }
}
