package servent.message;

import app.BackupData;

public class SendMeYouBackupMessage extends BasicMessage{

    public SendMeYouBackupMessage(int senderPort, int receiverPort) {
        super(MessageType.SEND_BACKUP, senderPort, receiverPort);
    }

}
