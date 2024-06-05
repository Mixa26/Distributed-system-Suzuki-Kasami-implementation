package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.PutMessage;
import servent.message.RemoveFileMessage;

public class RemoveFileHandler implements MessageHandler{

    private Message clientMessage;

    public RemoveFileHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage instanceof RemoveFileMessage){
            synchronized(AppConfig.chordState.successorLock) {
                AppConfig.chordState.removeValue(((RemoveFileMessage) clientMessage).getKey());
            }
        } else {
            AppConfig.timestampedErrorPrint("RemoveFileHandler got message of different type than RemoveFileMessage!");
        }
    }
}
