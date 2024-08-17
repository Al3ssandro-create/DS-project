package Message;

import java.util.UUID;

public class DisconnectMessage extends Message{

    public DisconnectMessage(String senderUsername, UUID senderId){
        super(senderUsername, senderId);
        setType(MessageType.DISCONNECTION);
    }
}
