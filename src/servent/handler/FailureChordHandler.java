package servent.handler;

import app.*;
import servent.message.*;
import servent.message.util.MessageUtil;
import java.util.HashMap;
import java.util.Map;

public class FailureChordHandler {

    public static void restructure(BackupData backupData, ServentInfo deadChord) {

        synchronized (AppConfig.chordState.restructureSync) {
            synchronized (AppConfig.chordState.chordSync) {

                // Request token for critical section
                int serventNum = AppConfig.myServentInfo.getId();
                synchronized (AppConfig.chordState.tokenRequestsLock) {
                    AppConfig.chordState.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum) + 1);
                    if ((AppConfig.chordState.token == null)) {
                        for (int i = 0; i < AppConfig.SERVENT_COUNT; i++) {
                            if (i == serventNum) continue;
                            System.out.println("RESTRUCTURE SENDING TOKEN REQUEST TO " + AppConfig.getInfoById(i).getListenerPort() + " NUMBER " + AppConfig.chordState.tokenRequests.get(serventNum) + " AND I AM ID " + serventNum);
                            TokenRequestMessage tokenRequestMessage = new TokenRequestMessage(MessageType.TOKEN_REQUEST, AppConfig.myServentInfo.getListenerPort(), AppConfig.getInfoById(i).getListenerPort(), serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
                            MessageUtil.sendMessage(tokenRequestMessage);
                        }
                    }
                }
                // Wait for the token
                System.out.println("RESTRUCTURE GETTING TOKEN");
                synchronized (AppConfig.chordState.tokenRequestsLock) {
                    while (AppConfig.chordState.token == null) {
                        try {
                            System.out.println("RESTRUCTURE WAITING FOR TOKEN");
                            AppConfig.chordState.tokenRequestsLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // Token received!
                System.out.println("RESTRUCTURE GOT TOKEN");

                synchronized (AppConfig.chordState.successorLock) {

                    AppConfig.chordState.setPredecessor(backupData.getPredecessorInfo());
                    AppConfig.chordState.healthPort.set(backupData.getPredecessorInfo().getListenerPort());

                    AppConfig.chordState.removeNode(deadChord);

                    Map<Integer, MyFile> myValues = AppConfig.chordState.getValueMap();

                    myValues.putAll(backupData.getValueMap());

                    AppConfig.chordState.setValueMap(myValues);
                }

                // Updates
                MessageUtil.sendMessage(new FailureUpdatesMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.chordState.getNextNodePort(), deadChord));

                // Wait for all the update messages to finish
                synchronized (AppConfig.chordState.updatesSync) {
                    try {
                        System.out.println("RESTRUCTURE WAITING FOR UPDATES");
                        AppConfig.chordState.updatesSync.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                // All updates finished, safe to give token to someone else
                System.out.println("RESTRUCTURE GOT ALL UPDATES");
                // Mark in the token that my critical section is over
                synchronized (AppConfig.chordState.tokenRequestsLock) {
                    AppConfig.chordState.token.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
                    // Append requests in order
                    for (Map.Entry<Integer, Integer> tokenRequest : AppConfig.chordState.tokenRequests.entrySet()) {
                        if (tokenRequest.getValue() == AppConfig.chordState.token.tokenRequests.get(tokenRequest.getKey()) + 1
                                && !AppConfig.chordState.token.queue.contains(tokenRequest.getKey())) {
                            AppConfig.chordState.token.queue.add(tokenRequest.getKey());
                        }
                    }


                    // Give token to the first chord in queue
                    if (!AppConfig.chordState.token.queue.isEmpty()) {
                        int sendTokenTo = AppConfig.chordState.token.queue.remove();
                        System.out.println("RESTRUCTURE GIVING TOKEN TO " + AppConfig.getInfoById(sendTokenTo).getListenerPort());
                        TokenMessage tokenMessage = new TokenMessage(MessageType.TOKEN, AppConfig.myServentInfo.getListenerPort(), AppConfig.getInfoById(sendTokenTo).getListenerPort(), new Token(AppConfig.chordState.token));
                        MessageUtil.sendMessage(tokenMessage);
                        // I sent the token and don't have it anymore
                        AppConfig.chordState.token = null;
                    }
                }
            }
        }
    }
}
