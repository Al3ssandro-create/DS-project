import java.util.*;

public class Room {
    private final UUID roomId;
    private final String name;
    private final Set<User> participants;
    private List<Message> messages;
    private Map<User, Integer> userSequenceNumbers;
    
    public Room(String name, Set<User> participants) {
        this.roomId = UUID.randomUUID();
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>();
        this.userSequenceNumbers = new HashMap<>();
        for (User participant : participants) {
            userSequenceNumbers.put(participant, 0);
        }
    }
    public String getRoomName() {
        return name;
    }
    public String getName() {
        return name;
    }
    public Set<User> getParticipants() {
        return participants;
    }
    public List<Message> getMessages() {
        return messages;
    }
    public Map<User, Integer> getUserSequenceNumbers() {
        return userSequenceNumbers;
    }
    public void addMessage(Message message) {
        messages.add(message);
        User sender = message.getSender();
        int sequenceNumber = message.getSequenceNumber();
        userSequenceNumbers.put(sender, sequenceNumber);
    }
}
