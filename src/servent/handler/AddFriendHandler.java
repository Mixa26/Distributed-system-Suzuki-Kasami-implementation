package servent.handler;

import app.AppConfig;
import servent.message.AddFriendMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class AddFriendHandler implements MessageHandler {

    private Message clientMessage;

    public AddFriendHandler() {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage instanceof AddFriendMessage) {
            AppConfig.chordState.friendChords.add(((AddFriendMessage)clientMessage).getChordID());
            AppConfig.timestampedStandardPrint("Added friend chordID " + ((AddFriendMessage)clientMessage).getChordID() + " on port " + clientMessage.getSenderPort());
            if (!((AddFriendMessage)clientMessage).isiAlreadyAddedYou()){
                AddFriendMessage addFriendMessage = new AddFriendMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(), true);
                MessageUtil.sendMessage(addFriendMessage);
            }
        } else {
            AppConfig.timestampedErrorPrint("AddFriendHandler got message of different type from AddFriendMessage");
        }
    }
}
