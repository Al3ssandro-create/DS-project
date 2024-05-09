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
    public Room(String name, Set<UUID> participants, UUID roomId, List<RoomMessage> messages){
        this.roomId = roomId;
        this.name = name;
        this.participants = new HashSet<>(participants);
        this.messages = new ArrayList<>(messages);
        this.userSequenceNumbers = new HashMap<>(); 
        this.vectorClock = new VectorClock(participants); //TODO:???
    }

    public void addOwnMessage(RoomMessage message){
        messages.add(message);
        UUID sender = message.getSenderId();
        int sequenceNumber = message.getSequenceNumber();
        userSequenceNumbers.put(sender, sequenceNumber);
        checkQueue();
    }

    public void addMessage(RoomMessage message) {
        
        
        boolean valid = vectorClockCheck(message);
        System.out.print(Color.GREEN + "Vector clock ricevuto msg:\n" + Color.RESET);
        System.out.print(message.getVectorClock().toString());
        System.out.println(Color.BLUE + "Valid: " + valid + Color.RESET);

        if(valid){
            messages.add(message);
            vectorClock.update(message.getVectorClock());
            System.out.print(Color.GREEN + "Vector clock dopo msg (updated):\n" + Color.RESET);
            System.out.print(vectorClock.toString());
            UUID sender = message.getSenderId();
            int sequenceNumber = message.getSequenceNumber();
            userSequenceNumbers.put(sender, sequenceNumber);
            checkQueue();
        }else{
            messageQueue.add(message);
        }
        for(RoomMessage msg: messageQueue){
            System.out.println(Color.RED + msg.getSender() + ": " + msg.getContent() + Color.RESET);
        }
    }

    /**
     * Questa funzione controlla che il vector clock di un messaggio arrivato sia consistente con il vector
     * clock che l'utente ricevente il messaggio ha nella room.
     * Se non è consistente il messaggio viene inserito in una coda di messaggi in attesa.
     * Se è consistente viene aggiunto nella lista dei messaggi della stanza e si controlla se messaggi
     * in attesa adesso siano consistenti
     * @param message ricevuto
     * @return se il messaggio ricevuto sia valido (consistente) o no
     */
    private boolean vectorClockCheck(RoomMessage message){
        /* il controllo sul vector clock si fa qua
         * the version of vector clocks used is the one where the clock is incremented only
         * when a message is sent. On receive just merge, not increment*/
        Map<UUID, Integer> receivedVector = message.getVectorClock().getVector();
        Map<UUID, Integer> thisVector = vectorClock.getVector();
        System.out.print(Color.GREEN + "Vector clock nella stanza:\n" + Color.RESET);
        System.out.print(vectorClock.toString());
        boolean valid = true;
        System.out.println(Color.BLUE + "Sender user " + message.getSenderId() + "\n" + Color.RESET); 
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
        return vectorClock;
    }
}
