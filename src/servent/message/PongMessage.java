package servent.message;

public class PongMessage extends BasicMessage{

    public PongMessage(int senderPort, int receiverPort) {
        super(MessageType.PONG, senderPort, receiverPort);
    }
}
