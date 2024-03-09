import java.io.IOException;
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
        Room room = new Room(name, new HashSet<>(List.of(this)));
        rooms.put(room.getRoomName(), room);
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
}