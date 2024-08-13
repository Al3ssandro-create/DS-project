package Message;

import Entities.UserTuple;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ResponseReconnectMessage extends ConnectMessage {
    private final UUID newUserId;
    private Set<UserTuple> disconnectedUser;
    public ResponseReconnectMessage(int port, String ip, String username, UUID userId, UUID newUserId, Set<UserTuple> disconnectedUser) {
        super(port, ip, username, userId);
        setType(MessageType.RESPONSE_RECONNECT);
        this.newUserId = newUserId;
        this.disconnectedUser = disconnectedUser;
    }
    public UUID getNewUserId() {
        return newUserId;
    }
    public Set<UserTuple> getDisconnectedUser() {
        return disconnectedUser;
    }
}
