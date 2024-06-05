package servent.handler;

import app.AppConfig;
import servent.message.IsReallyDeadMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class IsReallyDeadHandler implements MessageHandler{

    private Message clientMessage;

    public IsReallyDeadHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        IsReallyDeadMessage msg = ((IsReallyDeadMessage)clientMessage);
        if (!msg.isChecker() && !msg.isPortInQuestion()) {
            MessageUtil.sendMessage(new IsReallyDeadMessage(AppConfig.myServentInfo.getListenerPort(), msg.getPortToCheck(), clientMessage.getSenderPort(), true, false));
        } else if (msg.isChecker() && !msg.isPortInQuestion()) {
            MessageUtil.sendMessage(new IsReallyDeadMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(), msg.getPortToCheck(), false, true));
        } else if (!msg.isChecker() && msg.isPortInQuestion()) {
            MessageUtil.sendMessage(new IsReallyDeadMessage(AppConfig.myServentInfo.getListenerPort(), msg.getPortToCheck(), clientMessage.getSenderPort(), true, true));
        } else {
            AppConfig.chordState.predecessorConfirmedDead.set(false);
        }
    }
}
