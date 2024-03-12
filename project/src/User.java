import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;
import Color.Color;
public class User implements Serializable {
    private UUID userId;
    private Set<User> peers;
    private String username;
    public Map<String, Room> rooms;
    private Room actualRoom = null;
    private int port;
    private Socket sendingSocket;
    private Socket listeningSocket;

    public User(String username, int port) throws IOException {
        this.userId = UUID.randomUUID();
        this.username = username;
        this.rooms = new HashMap<>();
        this.port = port;
        this.peers = new HashSet<>();
    }
    public User(String username,UUID userId, int port, Socket sendingSocket, Socket listeningSocket) {
        this.userId = userId;
        this.username = username;
        this.rooms = new HashMap<>();
        this.port = port;
        this.peers = new HashSet<>();
        this.sendingSocket = sendingSocket;
        this.listeningSocket = listeningSocket;
    }

    public void createRoom(String name){
        System.out.println(Color.BLUE + "Select the partecipants of the room:\n" + Color.RESET);
        Set<User> selected = new HashSet<>(selectPeers());
        Room room = new Room(name, new HashSet<>(selected));
        rooms.put(room.getRoomName(), room);
        //send creation room message to all partecipant
    }

    public void listRooms(){
        for (Room room : rooms.values()) {
            System.out.println(room.getName());
        }
    }

    public void viewChat(String roomName){
        Room room = rooms.get(roomName);
        if(room != null){
            if(room.getMessages().isEmpty()){
                System.out.println("No messages in this room");
                return;
            }
            System.out.println(Color.GREEN + "_______________________ " + room.getRoomName() + " _______________________" + Color.RESET);
            for (Message message : room.getMessages()) {
                System.out.println(message.getSender().getUsername() + ": " + message.getContent());
            }
        } else {
            System.out.println("Room not found");
        }
    }
    public void startPeerDiscovery() {
        Thread discoveryThread = new Thread(() -> {
            NetworkDiscovery networkDiscovery = new NetworkDiscovery(this);
            networkDiscovery.discoverPeers();
        });
        discoveryThread.start();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Room getRoom() {
        return actualRoom;
    }

    public void setRoom(Room room){
        actualRoom = room;
    }

    public Set<User> listPeers() {
        return peers;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    public Socket getSendingSocket() {
        return sendingSocket;
    }
    public Socket getListeningSocket() {
        return listeningSocket;
    }

    public void removePeer(Socket socket) {
        User disconnectedUser = this.peers.stream()
                .filter(user -> user.getListeningSocket().equals(socket))
                .findFirst()
                .orElse(null);

        if (disconnectedUser != null) {
            System.out.println("\n" + Color.RESET + disconnectedUser.getUsername() + Color.RED + " DISCONNECT FROM THE NETWORK" + Color.RESET);
            this.peers.remove(disconnectedUser);
        }
    }

    public void addPeer(User peer) {
        System.out.println("\n" + Color.RESET + peer.getUsername() + Color.GREEN + " CONNECT TO THE NETWORK" + Color.RESET);
        this.peers.add(peer);
    }

    public void receiveMessages(){
        new Thread(()->{
            try{
                while(true){
                    ObjectInputStream in = new ObjectInputStream(listeningSocket.getInputStream());
                    Message msg = (Message) in.readObject();
                    if(msg.getRoom() != null)
                        addRoom(msg);
                    // else handling of messages of chat, maybe will do it directly in the room
                }
            }catch(IOException e){
                e.printStackTrace();
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }
        }).start();
    }

    //this method is called from the method that reads the received messages if !message.getRoom().isNull()
    public void addRoom(Message message){
        Room room = message.getRoom();
        rooms.put(room.getName(), room);
    }

    private Set<User> selectPeers(){
        Scanner scan = new Scanner(System.in);
        Set<User> selected = new HashSet<>();
        for(User peer:peers)
            System.out.println(Color.GREEN + peer.getUsername() + "\n" + Color.RESET);
        System.out.println(Color.BLUE + "Write the username(s) of the client(s) to add separated by a comma.\n" + Color.RESET);
        String users = scan.nextLine();
        while(users.isEmpty()){
            System.out.println(Color.BLUE + "The usernames list cannot be empty.\n" + Color.RESET);
            users = scan.nextLine();
        }
        Set<String> cleanUsers = new HashSet<>(spliceString(users));
        for(User peer:peers){
            if(cleanUsers.contains(peer.getUsername()))
                selected.add(peer);
        }
        return selected;
    }

    private Set<String> spliceString(String fullString){
        String[] wordsArray = fullString.split(",");
        Set<String> spliced = new HashSet<>();
        for(String word:wordsArray)
            spliced.add(word.trim());
        return spliced;
    }
}