package Message;

import Entities.User;

import java.util.UUID;

public class HeartbeatMessage extends Message {
    private final UUID userId;
    public HeartbeatMessage(String sender, UUID userId) {
        super(sender,userId);
        setType(MessageType.HEARTBEAT);
        this.userId = userId;
    }
}
