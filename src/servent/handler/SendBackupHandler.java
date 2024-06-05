package servent.handler;

import app.AppConfig;
import app.BackupData;
import servent.message.BackupMessage;
import servent.message.Message;
import servent.message.SendMeYouBackupMessage;
import servent.message.util.MessageUtil;

public class SendBackupHandler implements  MessageHandler{

    private Message clientMessage;

    public SendBackupHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        synchronized (AppConfig.chordState.successorLock) {
            MessageUtil.sendMessage(new BackupMessage(AppConfig.myServentInfo.getListenerPort(), ((SendMeYouBackupMessage) clientMessage).getSenderPort(), new BackupData(
                    AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.backupSequence.get(),
                    AppConfig.chordState.getPredecessor(),
                    AppConfig.chordState.getValueMapCopy()
            )));
        }
    }
}
