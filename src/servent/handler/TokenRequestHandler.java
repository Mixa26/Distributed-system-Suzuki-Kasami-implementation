package servent.handler;

import app.AppConfig;
import app.Token;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TokenMessage;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;

public class TokenRequestHandler implements MessageHandler{

    private Message clientMessage;

    public TokenRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage instanceof TokenRequestMessage) {
            int serventNum = ((TokenRequestMessage)clientMessage).getServentID();
            if (serventNum == AppConfig.myServentInfo.getId())return;
            synchronized (AppConfig.chordState.tokenRequestsLock) {
                AppConfig.chordState.tokenRequests.put(serventNum, Integer.max(((TokenRequestMessage) clientMessage).getRequestNumber(), AppConfig.chordState.tokenRequests.get(serventNum)));
                System.out.println("My RN " + AppConfig.chordState.tokenRequests);
            }
            System.out.println("IN TOKEN REQUEST");
            // If the chord is idle (not using the token)
            synchronized (AppConfig.chordState.chordSync) {
                if (AppConfig.chordState.token == null){
                    System.out.println("I dont have the token!");
                    return;
                }
                // If I have the token, and this is not an outdated request, send the token
                System.out.println("TRYING TO SeND THE TOKEN TO " + clientMessage.getSenderPort());
                synchronized (AppConfig.chordState.tokenRequestsLock) {
                    System.out.println("My RN " + AppConfig.chordState.tokenRequests);
                    System.out.println("Token LN " + AppConfig.chordState.token.tokenRequests);
                    System.out.println("first " + AppConfig.chordState.token + " second " + (AppConfig.chordState.tokenRequests.get(serventNum) == AppConfig.chordState.token.tokenRequests.get(serventNum) + 1));
                    if (AppConfig.chordState.token != null && AppConfig.chordState.tokenRequests.get(serventNum) == AppConfig.chordState.token.tokenRequests.get(serventNum) + 1) {
                        System.out.println("IM SENDING THE TOKEN TO " + clientMessage.getSenderPort());
                        TokenMessage tokenMessage = new TokenMessage(MessageType.TOKEN, AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(), new Token(AppConfig.chordState.token));
                        MessageUtil.sendMessage(tokenMessage);
                        // I sent the token and don't have it anymore
                        AppConfig.chordState.token = null;
                    }
                }
            }
        } else {
            AppConfig.timestampedErrorPrint("TokenRequestHandler got a message of different type than TokenRequestMessage.");
        }
    }
}
