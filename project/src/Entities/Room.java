package Entities;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import Message.*;
import Vector.VectorClock;
import Color.Color;
public class Room implements Serializable{
    private final UUID roomId;
    private final String name;
    private final Set<UUID> participants;
    private List<RoomMessage> messages;
    private Map<UUID, Integer> userSequenceNumbers;
    private VectorClock vectorClock;
    private Set<RoomMessage> messageQueue = new HashSet<>();
    
    public Room(String name, Set<UUID> participants) {
        this.roomId = UUID.randomUUID();
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>();
        this.userSequenceNumbers = new HashMap<>();
        this.vectorClock = new VectorClock(participants);       
    }

    public Room(String name, Set<UUID> participants, UUID roomId, List<RoomMessage> messages, VectorClock vectorClock){
        this.roomId = roomId;
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>(messages);
        this.userSequenceNumbers = new HashMap<>(); 
        this.vectorClock = vectorClock;
    }

    /**
     * Increase the clock of the user
     * @param user user to increase the clock
     */
    public void incrementClock(UUID user){
        vectorClock.incrementUser(user);
    }

    /**
     * Add a message to the room
     * @param message message to add
     */
    public void addOwnMessage(RoomMessage message){
        messages.add(message);
        UUID sender = message.getSenderId();
        int sequenceNumber = message.getSequenceNumber();
        userSequenceNumbers.put(sender, sequenceNumber);
        //vectorClock.incrementUser(sender);
        checkQueue();
    }

    /**
     * Add a message to the room
     * @param message message to add
     */
    public void addMessage(RoomMessage message) {       
        
        boolean valid = vectorClockCheck(message);
        //System.out.print(Color.GREEN + "Vector clock ricevuto msg:\n" + Color.RESET);
        //System.out.print(message.getVectorClock().toString());
        //System.out.println(Color.BLUE + "Valid: " + valid + Color.RESET);

        if(valid){
            messages.add(message);
            vectorClock.update(message.getVectorClock());
            //System.out.print(Color.GREEN + "Vector clock dopo msg (updated):\n" + Color.RESET);
            //System.out.print(vectorClock.toString());
            UUID sender = message.getSenderId();
            int sequenceNumber = message.getSequenceNumber();
            userSequenceNumbers.put(sender, sequenceNumber);
            checkQueue();
        }else{
            messageQueue.add(message);
            checkQueue();
        }
        for(RoomMessage msg: messageQueue){
            System.out.println(Color.RED + msg.getSender() + ": " + msg.getContent() + Color.RESET);
        }
    }

    /**
     * This function checks whether the vector clock of an incoming message is consistent with the vector
     * clock that the user receiving the message has in the room.
     * If it's not consistent, the message is placed in a queue of waiting messages.
     * If it's consistent, it's added to the list of messages in the room and checks if waiting messages
     * are now consistent.
     * @param message received
     * @return whether the received message is valid (consistent) or not
     */
    private boolean vectorClockCheck(RoomMessage message){
        /* il controllo sul vector clock si fa qua
         * la versione di vector clocks usata è quella in cui il clock è incrementato solo
         * quando un messaggio è inviato. Su ricesione solo merge, non incremento */
        Map<UUID, Integer> receivedVector = message.getVectorClock().getVector();
        Map<UUID, Integer> thisVector = vectorClock.getVector();
        //System.out.print(Color.GREEN + "Vector clock nella stanza:\n" + Color.RESET);
        //System.out.print(vectorClock.toString());
        boolean valid = true;
        //System.out.println(Color.BLUE + "Sender user " + message.getSenderId() + "\n" + Color.RESET); 
        /*controllo che il clock del messaggio sia:
            - minore o uguale in tutti gli altri peers
            - uguale al clock che si ha più 1 per il sender
         se non è così mettere il messagio in coda in attesa che queste condizioni siano vere*/
        for(UUID peer: receivedVector.keySet()){
            if((!message.getSenderId().equals(peer)) && (receivedVector.get(peer) > thisVector.get(peer))){
                valid = false;
                System.out.println(Color.RED + "Erorre clock utente " + peer + "\n" + Color.RESET);
            }
            else if((message.getSenderId().equals(peer)) && (receivedVector.get(peer) != (thisVector.get(peer) + 1))){
                valid = false;
                System.out.println(Color.RED + "Erorre clock sender " + peer + "\n" + Color.RESET);
            }
                
        }
        return valid;

    }

    /**
     * This function checks if there are messages in the queue that can be inserted into the room
     * after the arrival of other messages
     * If a message is inserted, the function is called recursively to check if other messages can be inserted
     *
     * @return void
     */
    private void checkQueue(){
        /* questo metodo controlla se dei messaggi nella coda possono essere inseriti nella stanza
         * dopo l'arrivo di altri messaggi */
        boolean update = false;
        for(RoomMessage message: messageQueue){
            if(vectorClockCheck(message)){
                messages.add(message);
                vectorClock.update(message.getVectorClock());
                messageQueue.remove(message);
                update = true;
                break;
            }
        }
        if(update)checkQueue();
    }

    public String getRoomName() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public List<RoomMessage> getMessages() {
        return messages;
    }

    public Map<UUID, Integer> getUserSequenceNumbers() {
        return userSequenceNumbers;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public VectorClock getVectorClock(){
        return vectorClock.copy();
    }

    /**
     * This function checks if a user is in the room
     * @param userId user to check
     * @return boolean
     */
    public boolean contains(UUID userId){
        if(participants.contains(userId))
            return true;
        else   
            return false;
    }
}
