package servent.message;

public class IsReallyDeadMessage extends BasicMessage{

    private int portToCheck;

    private boolean checker;

    private boolean portInQuestion;

    public IsReallyDeadMessage(int senderPort, int receiverPort, int portToCheck, boolean checker, boolean portInQuestion) {
        super(MessageType.IS_REALLY_DEAD, senderPort, receiverPort);
        this.portToCheck = portToCheck;
        this.checker = checker;
        this.portInQuestion = portInQuestion;
    }

    public int getPortToCheck() {
        return portToCheck;
    }

    public boolean isChecker() {
        return checker;
    }

    public boolean isPortInQuestion() {
        return portInQuestion;
    }
}
