package servent.handler;

import app.AppConfig;
import servent.message.*;
import servent.message.util.MessageUtil;


public class FailureUpdatesHandler implements MessageHandler{

    private Message clientMessage;


    public FailureUpdatesHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.FAILURE_UPDATE) {
            if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
                synchronized(AppConfig.chordState.successorLock) {
                    AppConfig.chordState.removeNode(((FailureUpdatesMessage)clientMessage).getDeadChord());
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                    Message nextUpdate = new FailureUpdatesMessage(clientMessage.getSenderPort(), Integer.valueOf(AppConfig.chordState.getNextNodePort()), ((FailureUpdatesMessage)clientMessage).getDeadChord());
                    MessageUtil.sendMessage(nextUpdate);
                }
            } else {
                synchronized(AppConfig.chordState.successorLock) {
                    // Ask for predecessor backup
                    System.out.println("SEND ME YOUR BACKUP PORT " + AppConfig.chordState.getPredecessor().getListenerPort());
                    SendMeYouBackupMessage bm = new SendMeYouBackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.chordState.getPredecessor().getListenerPort());
                    MessageUtil.sendMessage(bm);
                }

                AppConfig.chordState.alreadySent.set(false);
                AppConfig.chordState.predecessorHealth.set(true);

                System.out.println("I SHOULD NOTIFY?");
                synchronized (AppConfig.chordState.updatesSync) {
                    AppConfig.chordState.updatesSync.notify();
                }
            }
        } else {
            AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
        }
    }
}
