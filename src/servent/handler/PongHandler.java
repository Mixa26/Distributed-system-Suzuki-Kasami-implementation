package servent.handler;

import app.AppConfig;
import servent.message.Message;

public class PongHandler implements MessageHandler{

    private Message clientMessage;

    public PongHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Predecessor " + clientMessage.getSenderPort() + " is healthy!");
        AppConfig.chordState.predecessorHealth.set(true);
    }
}
