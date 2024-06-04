package servent.handler;

import app.AppConfig;
import app.MyFile;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;

public class TellGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public TellGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TELL_GET) {

			try {
				int key = ((TellGetMessage)clientMessage).getKey();
				MyFile value = ((TellGetMessage)clientMessage).getValue();
				if (value == null) {
					AppConfig.timestampedStandardPrint("No such key: " + key);
				} else {
					AppConfig.timestampedStandardPrint("Got key " + key + " with file " + value.getFile());
				}
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Got TELL_GET message with bad text: " + clientMessage.getMessageText());
			}
		} else {
			AppConfig.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
