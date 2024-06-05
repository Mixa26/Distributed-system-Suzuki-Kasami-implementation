package servent.handler;

import app.AppConfig;
import servent.message.BackupMessage;
import servent.message.Message;

public class BackupHandler implements MessageHandler{

    private Message clientMessage;

    public BackupHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage instanceof BackupMessage) {
            if (AppConfig.chordState.backup.get(((BackupMessage)clientMessage).getBackupData().getSequenceNumber()) == null || ((BackupMessage)clientMessage).getBackupData().getSequenceNumber() >  AppConfig.chordState.backup.get(((BackupMessage)clientMessage).getBackupData().getSequenceNumber()).getSequenceNumber()) {
                AppConfig.chordState.backup.put(((BackupMessage) clientMessage).getBackupData().getPort(), ((BackupMessage) clientMessage).getBackupData());
            }
        } else {
            AppConfig.timestampedErrorPrint("BackupHandler got message of different type than BackupMessage!");
        }
    }
}
