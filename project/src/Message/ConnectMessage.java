package Message;

import Entities.User;

import java.util.UUID;

public class ConnectMessage extends Message {
    private final int port;
    private final String ip;
    public ConnectMessage(int sequenceNumber, int port, String ip, String username, UUID userId) {
        super(username, userId, sequenceNumber);
        this.port = port;
        this.ip = ip;


    }
    public int getPort() {
        return port;
    }
    public String getIp() {
        return ip;
    }
}
