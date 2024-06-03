package servent.message;

public class AllUpdatesDoneMessage extends BasicMessage{

    public AllUpdatesDoneMessage(int senderPort, int receiverPort) {
        super(MessageType.ALL_UPDATES_DONE, senderPort, receiverPort);
    }
}
