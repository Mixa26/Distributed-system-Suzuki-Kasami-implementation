package servent.handler;

import java.util.ArrayList;
import java.util.List;

import app.AppConfig;
import app.ServentInfo;
import servent.message.AllUpdatesDoneMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
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

				AppConfig.chordState.addNodes(newNodes);
				String newMessageText = "";
				if (clientMessage.getMessageText().equals("")) {
					newMessageText = String.valueOf(AppConfig.myServentInfo.getListenerPort());
				} else {
					newMessageText = clientMessage.getMessageText() + "," + AppConfig.myServentInfo.getListenerPort();
				}
				Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(), AppConfig.chordState.getNextNodePort(),
						newMessageText);
				MessageUtil.sendMessage(nextUpdate);
			} else {
				String messageText = clientMessage.getMessageText();
				String[] ports = messageText.split(",");

				List<ServentInfo> allNodes = new ArrayList<>();
				for (String port : ports) {
					allNodes.add(new ServentInfo( AppConfig.myServentInfo.getIpAddress(), Integer.parseInt(port), AppConfig.getIdByPort(Integer.parseInt(port))));
				}
				AppConfig.chordState.addNodes(allNodes);
				AppConfig.chordState.updatesCollected.incrementAndGet();

				System.out.println("I SHOULD NOTIFY?");
				AllUpdatesDoneMessage allUpdatesDoneMessage = new AllUpdatesDoneMessage(AppConfig.myServentInfo.getListenerPort(), Integer.parseInt(ports[0]));
				MessageUtil.sendMessage(allUpdatesDoneMessage);
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
