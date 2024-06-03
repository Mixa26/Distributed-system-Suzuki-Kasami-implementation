package servent.handler;

import app.AppConfig;
import servent.message.AllUpdatesDoneMessage;
import servent.message.Message;

public class AllUpdatesDoneHandler implements MessageHandler{

    private Message clientMessage;

    public AllUpdatesDoneHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }
    @Override
    public void run() {
        if (clientMessage instanceof AllUpdatesDoneMessage) {
            synchronized (AppConfig.chordState.updatesSync) {
                AppConfig.chordState.updatesSync.notify();
            }
        } else {
            AppConfig.timestampedErrorPrint("AllUpdatesDoneHandler got message of different type than AllUpdatesDoneMessage!");
        }
    }
}
