package servent.message;

import app.Token;

public class TokenMessage extends BasicMessage{
    private Token token;

    public TokenMessage(MessageType type, int senderPort, int receiverPort, Token token) {
        super(type, senderPort, receiverPort);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
