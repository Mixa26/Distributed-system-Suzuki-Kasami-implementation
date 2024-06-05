package servent.message;

import app.BackupData;

public class BackupMessage extends BasicMessage{

    private BackupData backupData;

    public BackupMessage(int senderPort, int receiverPort, BackupData backupData) {
        super(MessageType.BACKUP, senderPort, receiverPort);
        this.backupData = backupData;
    }

    public BackupData getBackupData() {
        return backupData;
    }
}
