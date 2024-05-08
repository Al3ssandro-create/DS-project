package Message;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import Message.*;
public abstract class Message implements Serializable {
    private final UUID messageId;
    private final String senderUsername;
    private UUID senderId;
    private int sequenceNumber; // New field for sequence number
    private String roomName;
    private Set<String> users;
    private String type; //enum to see what type of message is sent

    public Message(String senderUsername, UUID  senderId, int sequenceNumber) {
        this.messageId = UUID.randomUUID();
        this.senderUsername = senderUsername;
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
    }
    public void setType(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
    public UUID getMessageId() {
        return messageId;
    }
    public String getSender(){
        return senderUsername;
    }
    public UUID getSenderId(){
        return senderId;
    }
    public String getRoomName(){
        return roomName;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Set<String> getUsers(){
        return users;
    }
}
