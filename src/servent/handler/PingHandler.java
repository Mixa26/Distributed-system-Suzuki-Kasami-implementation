package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.PongMessage;
import servent.message.util.MessageUtil;

public class PingHandler implements MessageHandler {

    private Message clientMessage;

    public PingHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        MessageUtil.sendMessage(new PongMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort()));
    }
}
