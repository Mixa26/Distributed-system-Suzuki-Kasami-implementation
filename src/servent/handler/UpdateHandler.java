package servent.handler;

import java.util.ArrayList;
import java.util.List;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private Message clientMessage;

	public UpdateHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				ServentInfo newNodInfo = new ServentInfo(AppConfig.myServentInfo.getIpAddress(), clientMessage.getSenderPort(), AppConfig.getIdByPort(clientMessage.getSenderPort()));
				List<ServentInfo> newNodes = new ArrayList<>();
				newNodes.add(newNodInfo);

				synchronized(AppConfig.chordState.successorLock) {
					AppConfig.chordState.addNodes(newNodes);

					String newMessageText = "";
					if (clientMessage.getMessageText().equals("")) {
						newMessageText = String.valueOf(AppConfig.myServentInfo.getListenerPort());
					} else {
						newMessageText = clientMessage.getMessageText() + "," + AppConfig.myServentInfo.getListenerPort();
					}
					Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(), Integer.valueOf(AppConfig.chordState.getNextNodePort()),
							newMessageText);
					MessageUtil.sendMessage(nextUpdate);
				}
			} else {
				String messageText = clientMessage.getMessageText();
				String[] ports = messageText.split(",");

				List<ServentInfo> allNodes = new ArrayList<>();
				for (String port : ports) {
					allNodes.add(new ServentInfo( AppConfig.myServentInfo.getIpAddress(), Integer.parseInt(port), AppConfig.getIdByPort(Integer.parseInt(port))));
				}
				synchronized(AppConfig.chordState.successorLock) {
					AppConfig.chordState.addNodes(allNodes);

					// Ask for successor backup
					System.out.println("SEND ME YOUR BACKUP PORT " + AppConfig.chordState.getNextNodePort());
					SendMeYouBackupMessage b = new SendMeYouBackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.chordState.getNextNodePort());
					MessageUtil.sendMessage(b);
					// Ask for predecessor backup
					System.out.println("SEND ME YOUR BACKUP PORT " + AppConfig.chordState.getPredecessor().getListenerPort());
					SendMeYouBackupMessage bm = new SendMeYouBackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.chordState.getPredecessor().getListenerPort());
					MessageUtil.sendMessage(bm);

					AppConfig.chordState.healthPort.set(AppConfig.chordState.getPredecessor().getListenerPort());

					AppConfig.chordState.added.compareAndSet(false, true);
					AppConfig.chordState.successorLock.notify();
				}

				System.out.println("I SHOULD NOTIFY?");
				AllUpdatesDoneMessage allUpdatesDoneMessage = new AllUpdatesDoneMessage(AppConfig.myServentInfo.getListenerPort(), Integer.parseInt(ports[0]));
				MessageUtil.sendMessage(allUpdatesDoneMessage);
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
