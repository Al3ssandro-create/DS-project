package Message;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import Message.*;
public abstract class Message implements Serializable {
    private final UUID messageId;
    private final String senderUsername;
    private final UUID senderId;
    private String roomName;
    private Set<String> users;
    private String type; //enum to see what type of message is sent

    public Message(String senderUsername, UUID  senderId) {
        this.messageId = UUID.randomUUID();
        this.senderUsername = senderUsername;
        this.senderId = senderId;
    }
    public void setType(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
    public String getSender(){
        return senderUsername;
    }
    public UUID getSenderId(){
        return senderId;
    }
}
