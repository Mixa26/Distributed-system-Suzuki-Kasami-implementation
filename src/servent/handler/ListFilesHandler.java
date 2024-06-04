package servent.handler;

import app.AppConfig;
import app.ChordState;
import app.FileType;
import app.MyFile;
import servent.message.*;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;


public class ListFilesHandler implements MessageHandler{

    private Message clientMessage;

    public ListFilesHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage instanceof ListFilesMessage) {
            if (((ListFilesMessage) clientMessage).getFiles() == null) {
                List<MyFile> allFiles = new ArrayList<>();

                synchronized(AppConfig.chordState.successorLock) {
                    System.out.println("MY FILE LIST " + AppConfig.chordState.myFilesInSystem);

                    for (MyFile file : AppConfig.chordState.myFilesInSystem) {
                        if (file.getAccess().equals(FileType.PUBLIC) || (file.getAccess().equals(FileType.PRIVATE) && AppConfig.chordState.friendChords.contains(ChordState.chordHash(clientMessage.getSenderPort())))) {
                            System.out.println("ADDING FILE TO LIST TO SEND " + file);
                            allFiles.add(file);
                        }
                    }
                }

                System.out.println("SENDING MY FILE LIST TO " + clientMessage.getSenderPort());
                Message allFilesMessage = new ListFilesMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(), allFiles);
                MessageUtil.sendMessage(allFilesMessage);
            }
            else {
                AppConfig.timestampedStandardPrint("Files from " + clientMessage.getSenderPort() + " are:");
                for (MyFile file : ((ListFilesMessage) clientMessage).getFiles()) {
                    AppConfig.timestampedStandardPrint("* " + file.getFile() + " | " + file.getAccess());
                }
            }
        } else {
            AppConfig.timestampedErrorPrint("ListFilesHandler got message of type different from ListFilesMessage!");
        }
    }
}
