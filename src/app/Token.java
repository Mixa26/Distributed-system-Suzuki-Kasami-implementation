package app;

import java.io.Serializable;
import java.util.*;

public class Token implements Serializable {
    public Map<Integer, Integer> tokenRequests;

    public Queue<Integer> queue;

    public Token(Token token) {
        this.tokenRequests = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : token.tokenRequests.entrySet()) {
            this.tokenRequests.put(entry.getKey(), entry.getValue());
        }

        this.queue = new LinkedList<>();
        for (Integer element : token.queue) {
            this.queue.add(element);
        }
    }

    public Token() {
        this.tokenRequests = new HashMap<>();
        for (int i = 0; i < AppConfig.SERVENT_COUNT; i++){
            tokenRequests.put(i, 0);
        }
        this.queue = new PriorityQueue<>();
    }
}
