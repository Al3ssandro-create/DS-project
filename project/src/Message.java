import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private UUID messageId;
    private User sender;
    private String content;
    private int sequenceNumber; // New field for sequence number

    public Message(User sender, String content, int sequenceNumber) {
        this.messageId = UUID.randomUUID();
        this.sender = sender;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
    }
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    public UUID getMessageId() {
        return messageId;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public void setSequenceNumber(int sequenceNumber) {

    }
}
