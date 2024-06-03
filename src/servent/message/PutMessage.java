package servent.message;

import app.MyFile;

public class PutMessage extends BasicMessage {

	private static final long serialVersionUID = 5163039209888734276L;

	private int key;

	private MyFile value;

	public PutMessage(int senderPort, int receiverPort, int key, MyFile value) {
		super(MessageType.PUT, senderPort, receiverPort);
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
