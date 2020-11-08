package ClientStuff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessagesController {
    private final Map<String, Integer> messagesStat = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> messages = Collections.synchronizedMap(new HashMap<>());

    public void addMessage(String uuid, String message) {
        messages.put(uuid, message);
        messagesStat.put(uuid, 0);
    }

    public void removeMessageIfZero(String uuid) {
        if (messagesStat.get(uuid) == 0) {
            messages.remove(uuid);
        }
    }

    public String getMessage(String uuid) {
        return messages.get(uuid);
    }

    public void increaseStat(String uuid) {
        messagesStat.put(uuid, messagesStat.get(uuid) + 1);
    }

    public void decreaseStat(String uuid) {
        if (messagesStat.containsKey(uuid)) {
            messagesStat.put(uuid, messagesStat.get(uuid) - 1);
        }
        if (messagesStat.get(uuid) == 0) {
            messagesStat.remove(uuid);
            messages.remove(uuid);
        }
    }
}
