package servent.handler;

import app.AppConfig;
import app.MyFile;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PutMessage;

public class PutHandler implements MessageHandler {

	private Message clientMessage;
	
	public PutHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {

		if (clientMessage.getMessageType() == MessageType.PUT) {
			AppConfig.chordState.putValue(((PutMessage)clientMessage).getKey(), ((PutMessage)clientMessage).getValue());
		} else {
			AppConfig.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
