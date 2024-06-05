package servent.message;

public class PingMessage extends BasicMessage{

    public PingMessage(int senderPort, int receiverPort) {
        super(MessageType.PING, senderPort, receiverPort);
    }
}
