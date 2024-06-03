package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.TokenMessage;

public class TokenHandler implements MessageHandler{

    private Message clientMessage;

    public TokenHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage instanceof TokenMessage) {
            System.out.println("TRYING TO RECEIVE THE TOKEN");
            synchronized (AppConfig.chordState.tokenRequestsLock) {
                System.out.println("I RECEIVED THE TOKEN");
                AppConfig.chordState.token = ((TokenMessage) clientMessage).getToken();
                AppConfig.chordState.tokenRequestsLock.notify();
            }
        } else {
            AppConfig.timestampedErrorPrint("TokenHandler got a message of different type than TokenMessage.");
        }
    }
}
