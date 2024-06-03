package servent.message;

import app.MyFile;

public class TellGetMessage extends BasicMessage {

	private static final long serialVersionUID = -6213394344524749872L;

	private int key;

	private MyFile value;

	public TellGetMessage(int senderPort, int receiverPort, int key, MyFile value) {
		super(MessageType.TELL_GET, senderPort, receiverPort);
		this.key = key;
		this.value = value;
	}

	public int getKey() {
		return key;
	}

	public MyFile getValue() {
		return value;
	}
}
