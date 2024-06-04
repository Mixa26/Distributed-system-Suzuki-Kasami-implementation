package servent.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import app.AppConfig;
import app.MyFile;
import app.ServentInfo;
import app.Token;
import servent.message.*;
import servent.message.util.MessageUtil;

public class NewNodeHandler implements MessageHandler {

    private Message clientMessage;

    public NewNodeHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.NEW_NODE) {
            synchronized (AppConfig.chordState.chordSync) {

                // Request token for critical section
                int serventNum = AppConfig.myServentInfo.getId();
                synchronized (AppConfig.chordState.tokenRequestsLock) {
                    AppConfig.chordState.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum) + 1);
                    if ((AppConfig.chordState.token == null)) {
                        for (int i = 0; i < AppConfig.SERVENT_COUNT; i++) {
                            if (i == serventNum)continue;
                            System.out.println("SENDING TOKEN REQUEST TO " + AppConfig.getInfoById(i).getListenerPort() + " NUMBER " + AppConfig.chordState.tokenRequests.get(serventNum) + " AND I AM ID " + serventNum);
                            TokenRequestMessage tokenRequestMessage = new TokenRequestMessage(MessageType.TOKEN_REQUEST, AppConfig.myServentInfo.getListenerPort(), AppConfig.getInfoById(i).getListenerPort(), serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
                            MessageUtil.sendMessage(tokenRequestMessage);
                        }
                    }
                }
                // Wait for the token
                System.out.println("GETTING TOKEN");
                synchronized (AppConfig.chordState.tokenRequestsLock) {
                    while (AppConfig.chordState.token == null) {
                        try {
                            System.out.println("WAITING FOR TOKEN");
                            AppConfig.chordState.tokenRequestsLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // Token received!
                System.out.println("GOT TOKEN");

                int newNodePort = clientMessage.getSenderPort();
                ServentInfo newNodeInfo = new ServentInfo(AppConfig.myServentInfo.getIpAddress(), newNodePort, AppConfig.getIdByPort(newNodePort));

                //check if the new node collides with another existing node.
                if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
                    System.out.println("COLLISION :(");
                    Message sry = new SorryMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort());
                    MessageUtil.sendMessage(sry);
                    // Inform bootstrap he can continue adding chords
                    try {
                        Socket bsSocket = new Socket(AppConfig.myServentInfo.getIpAddress(), AppConfig.BOOTSTRAP_PORT);

                        PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
                        bsWriter.write("Sorry\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
                        bsWriter.flush();

                        bsSocket.close();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    synchronized (AppConfig.chordState.tokenRequestsLock) {
                        AppConfig.chordState.token.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
                        // Append requests in order
                        for (Entry<Integer, Integer> tokenRequest : AppConfig.chordState.tokenRequests.entrySet()) {
                            if (tokenRequest.getValue() == AppConfig.chordState.token.tokenRequests.get(tokenRequest.getKey()) + 1
                                    && !AppConfig.chordState.token.queue.contains(tokenRequest.getKey())) {
                                AppConfig.chordState.token.queue.add(tokenRequest.getKey());
                            }
                        }


                        // Give token to the first chord in queue
                        if (!AppConfig.chordState.token.queue.isEmpty()) {
                            int sendTokenTo = AppConfig.chordState.token.queue.remove();
                            System.out.println("GIVING TOKEN TO " + AppConfig.getInfoById(sendTokenTo).getListenerPort());
                            TokenMessage tokenMessage = new TokenMessage(MessageType.TOKEN, AppConfig.myServentInfo.getListenerPort(), AppConfig.getInfoById(sendTokenTo).getListenerPort(), AppConfig.chordState.token);
                            MessageUtil.sendMessage(tokenMessage);
                            // I sent the token and don't have it anymore
                            AppConfig.chordState.token = null;
                        }
                    }

                    return;
                }

                //check if he is my predecessor
                boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
                    if (isMyPred) { //if yes, prepare and send welcome message
                        Map<Integer, MyFile> hisValues = new HashMap<>();

                        synchronized(AppConfig.chordState.successorLock) {
                            ServentInfo hisPred = AppConfig.chordState.getPredecessor();
                            if (hisPred == null) {
                                hisPred = AppConfig.myServentInfo;
                            }

                            AppConfig.chordState.setPredecessor(newNodeInfo);

                            Map<Integer, MyFile> myValues = AppConfig.chordState.getValueMap();

                            int myId = AppConfig.myServentInfo.getChordId();
                            int hisPredId = hisPred.getChordId();
                            int newNodeId = newNodeInfo.getChordId();

                            for (Entry<Integer, MyFile> valueEntry : myValues.entrySet()) {
                                if (hisPredId == myId) { //i am first and he is second
                                    if (myId < newNodeId) {
                                        if (valueEntry.getKey() <= newNodeId && valueEntry.getKey() > myId) {
                                            hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                                        }
                                    } else {
                                        if (valueEntry.getKey() <= newNodeId || valueEntry.getKey() > myId) {
                                            hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                                        }
                                    }
                                }
                                if (hisPredId < myId) { //my old predecesor was before me
                                    if (valueEntry.getKey() <= newNodeId) {
                                        hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                                    }
                                } else { //my old predecesor was after me
                                    if (hisPredId > newNodeId) { //new node overflow
                                        if (valueEntry.getKey() <= newNodeId || valueEntry.getKey() > hisPredId) {
                                            hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                                        }
                                    } else { //no new node overflow
                                        if (valueEntry.getKey() <= newNodeId && valueEntry.getKey() > hisPredId) {
                                            hisValues.put(valueEntry.getKey(), valueEntry.getValue());
                                        }
                                    }
                                }
                            }
                            for (Integer key : hisValues.keySet()) { //remove his values from my map
                                myValues.remove(key);
                            }
                            AppConfig.chordState.setValueMap(myValues);
                        }

                        System.out.println("SENDING WELCOME TO " + newNodePort);
                        WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo.getListenerPort(), newNodePort, hisValues);
                        MessageUtil.sendMessage(wm);
                        // Wait for all the update messages to finish
                        synchronized (AppConfig.chordState.updatesSync){
                            try {
                                System.out.println("WAITING FOR UPDATES");
                                AppConfig.chordState.updatesSync.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        // All updates finished, safe to give token to someone else
                        System.out.println("GOT ALL UPDATES");
                        // Mark in the token that my critical section is over
                        synchronized (AppConfig.chordState.tokenRequestsLock) {
                            AppConfig.chordState.token.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
                            // Append requests in order
                            for (Entry<Integer, Integer> tokenRequest : AppConfig.chordState.tokenRequests.entrySet()) {
                                if (tokenRequest.getValue() == AppConfig.chordState.token.tokenRequests.get(tokenRequest.getKey()) + 1
                                        && !AppConfig.chordState.token.queue.contains(tokenRequest.getKey())) {
                                    AppConfig.chordState.token.queue.add(tokenRequest.getKey());
                                }
                            }


                            // Give token to the first chord in queue
                            if (!AppConfig.chordState.token.queue.isEmpty()) {
                                int sendTokenTo = AppConfig.chordState.token.queue.remove();
                                System.out.println("GIVING TOKEN TO " + AppConfig.getInfoById(sendTokenTo).getListenerPort());
                                TokenMessage tokenMessage = new TokenMessage(MessageType.TOKEN, AppConfig.myServentInfo.getListenerPort(), AppConfig.getInfoById(sendTokenTo).getListenerPort(), new Token(AppConfig.chordState.token));
                                MessageUtil.sendMessage(tokenMessage);
                                // I sent the token and don't have it anymore
                                AppConfig.chordState.token = null;
                            }
                        }
                } else { //if he is not my predecessor, let someone else take care of it
                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
                    System.out.println("NOT MY NODE, SENDING TO " + nextNode.getListenerPort());
                    NewNodeMessage nnm = new NewNodeMessage(newNodePort, nextNode.getListenerPort());
                    MessageUtil.sendMessage(nnm);

                    synchronized (AppConfig.chordState.tokenRequestsLock) {
                        AppConfig.chordState.token.tokenRequests.put(serventNum, AppConfig.chordState.tokenRequests.get(serventNum));
                        // Append requests in order
                        for (Entry<Integer, Integer> tokenRequest : AppConfig.chordState.tokenRequests.entrySet()) {
                            if (tokenRequest.getValue() == AppConfig.chordState.token.tokenRequests.get(tokenRequest.getKey()) + 1
                                    && !AppConfig.chordState.token.queue.contains(tokenRequest.getKey())) {
                                AppConfig.chordState.token.queue.add(tokenRequest.getKey());
                            }
                        }


                        // Give token to the first chord in queue
                        if (!AppConfig.chordState.token.queue.isEmpty()) {
                            //int sendTokenTo = AppConfig.chordState.token.queue.remove();
                            System.out.println("GIVING TOKEN TO " + nextNode.getListenerPort());
                            TokenMessage tokenMessage = new TokenMessage(MessageType.TOKEN, AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), AppConfig.chordState.token);
                            MessageUtil.sendMessage(tokenMessage);
                            // I sent the token and don't have it anymore
                            AppConfig.chordState.token = null;
                        }
                    }
                }
            }
        } else{
            AppConfig.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
        }
    }

}
