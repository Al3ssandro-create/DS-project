package Message;

import Entities.User;

import java.util.UUID;

public class HeartbeatMessage extends Message {
    private final UUID userId;
    public HeartbeatMessage(String sender, int sequenceNumber, UUID userId) {
        super(sender,userId, sequenceNumber);
        setType(MessageType.HEARTBEAT);
        this.userId = userId;
    }
}
