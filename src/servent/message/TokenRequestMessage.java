package servent.message;

public class TokenRequestMessage extends BasicMessage{

    private int serventID;

    private int requestNumber;

    public TokenRequestMessage(MessageType type, int senderPort, int receiverPort, int serventID, int requestNumber) {
        super(type, senderPort, receiverPort);
        this.serventID = serventID;
        this.requestNumber = requestNumber;
    }

    public int getServentID() {
        return serventID;
    }

    public int getRequestNumber() {
        return requestNumber;
    }
}
