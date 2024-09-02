package Communication;

import Color.Color;
import Entities.Room;
import Entities.User;
import Message.*;
import UI.PeerCLI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static Message.MessageType.*;

public class NetworkDiscovery {
    private final User user;
    private ServerSocket serverSocket;

    public NetworkDiscovery(User user) {
        this.user = user;
    }

    /**
     * Handles incoming connections from peers in the network.
     * This method is responsible for processing different types of messages received from peers.
     * It performs actions based on the type of the message such as adding a new peer, reconnecting a peer,
     * updating a heartbeat, initializing a room, adding a message to a room, changing a username, and deleting a room.
     *
     * @param socket The socket of the peer that the connection is established with.
     * @param latch A CountDownLatch used for synchronization, ensuring certain operations complete before others proceed.
     * @throws IOException If an input or output exception occurred.
     */
    private void handleIncomingConnection(Socket socket, CountDownLatch latch) throws IOException {
        User handlingUser = null;
        try {
            while (true) {
                if(handlingUser != null && !handlingUser.checkHeartbeat()) {
                    System.out.println(Color.RED + "Peer " + handlingUser.getUsername() + " is not responding, removing from peers" + Color.RESET);
                    user.removePeer(handlingUser.getUserId());
                    break;
                }
                Message responseMessage = getMessage(socket);
                if(responseMessage != null) {
                    switch (responseMessage.getType()) {
                        case RESPONSE_RECONNECT:
                            user.setUserId(((ResponseReconnectMessage) responseMessage).getNewUserId());
                            user.setDisconnectedUser(((ResponseReconnectMessage) responseMessage).getDisconnectedUser());
                        case RESPONSE_DISCOVERY:
                            latch.countDown();
                        case DISCOVERY:
                        case PEER:
                            System.out.println(responseMessage.getType());
                            String peerUsername = responseMessage.getSender();
                            UUID peerId = responseMessage.getSenderId();
                            int peerPort = ((ConnectMessage) responseMessage).getPort();
                            String peerAddress = ((ConnectMessage) responseMessage).getIp();
                            if(!userExists(peerId) && !responseMessage.getType().equals(PEER) ) {
                                if(!user.inDisconnected(peerUsername) && !user.getUsername().equals(peerUsername)){
                                    handlingUser = user.addPeer(peerUsername, peerId, peerPort, socket);
                                    if(handlingUser == null) {
                                        sendChangeUsernameMessage(socket);
                                        break;
                                    }
                                }else{
                                    sendChangeUsernameOrReconnectMessage(socket);
                                    break;
                                }

                                if (responseMessage.getType().equals(DISCOVERY)) {
                                    sendResponseDiscoveryMessage(socket);
                                }

                                for (User peerUser : user.listPeers()) {
                                    if (!peerUser.getUserId().equals(peerId) && !peerUser.getUsername().equals(peerUsername)) {
                                        sendPeerMessage(peerUser.getUserId(), new ConnectMessage(peerPort, peerAddress, peerUsername, peerId));
                                    }
                                }
                            }else if(responseMessage.getType().equals(PEER)){
                                boolean flag = true;
                                for(User person : user.listPeers()){
                                    if(person.getUsername().equals(peerUsername)){
                                        flag = false;
                                        break;
                                    }

                                }
                                if(flag) user.startConnection(peerAddress, peerPort);
                            }
                            break;
                        case RECONNECT:
                            latch.countDown();
                            String peerUsernameRe = responseMessage.getSender();
                            int peerPortRe = ((ConnectMessage) responseMessage).getPort();
                            String peerAddressRe = ((ConnectMessage) responseMessage).getIp();
                            handlingUser = user.reconnectPeer(peerUsernameRe, peerPortRe, socket);

                            sendResponseReconnectMessage(socket, handlingUser.getUserId());
                            for (User peerUser : user.listPeers()) {
                                if (!peerUser.getUserId().equals(handlingUser.getUserId())) {
                                    sendPeerReconnectMessage(peerUser.getUserId(), new ConnectMessage(peerPortRe, peerAddressRe, peerUsernameRe, handlingUser.getUserId()));
                                }
                            }
                            Map<UUID, Room> rooms = user.getRooms();
                            for(UUID commonRoomId : user.commonRooms(handlingUser.getUserId()).keySet()){
                                    sendRoom(rooms.get(commonRoomId), handlingUser.getListeningSocket());
                            }
                            break;
                        case RECONNECT_PEER:
                            latch.countDown();
                            String peerUsernameReP = responseMessage.getSender();
                            int peerPortReP = ((ConnectMessage) responseMessage).getPort();
                            String peerAddressReP = ((ConnectMessage) responseMessage).getIp();
                            user.reconnectPeerWithID(peerUsernameReP);
                            user.startConnection(peerAddressReP, peerPortReP);
                            break;
                        case HEARTBEAT:
                            if (handlingUser != null) {
                                handlingUser.updateHeartbeat();
                            }
                            break;
                        case ROOM_INIT:
                            Room room = ((RoomInitMessage) responseMessage).getRoom();
                            user.addRoom(room);
                            break;
                        case ROOM_MESSAGE:
                            RoomMessage roomMessage = (RoomMessage) responseMessage;
                            user.addMessageToRoom(roomMessage);
                            break;
                        case CHANGE_USERNAME_OR_RECONNECT:
                            int choice = PeerCLI.ReconnectOrChangeUsername();
                            if(choice == 2){
                                changeUsername(socket);
                            }else{
                                reconnect(socket);
                            }
                            break;
                        case CHANGE_USERNAME:
                            PeerCLI.ChangeUsername();
                            changeUsername(socket);
                            break;
                        case DELETE_ROOM:
                            user.deleteRoom(((DeleteRoomMessage) responseMessage).getRoomId());
                            break;
                        default:
                            System.out.println(Color.RED + "Received unknown message type" + Color.RESET);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempts to reconnect to a peer in the network.
     * This method sends a reconnect message to the specified socket.
     *
     * @param socket The socket of the peer that the reconnection is attempted with.
     * @throws RuntimeException If an input or output exception occurred during the reconnection attempt.
     */
    private void reconnect(Socket socket) {
        try {
            sendReconnectMessage(socket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the username of the user in the network.
     * This method sends a discovery message to the specified socket after changing the username.
     *
     * @param socket The socket of the peer that the username change is communicated with.
     * @throws RuntimeException If an input or output exception occurred during the username change.
     */
    private void changeUsername(Socket socket) {
        try {
            sendDiscoveryMessage(socket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a room from the network.
     * This method sends a delete room message to the specified socket.
     *
     * @param roomID The ID of the room to be deleted.
     * @param socket The socket of the peer that the room deletion is communicated with.
     */
    public void deleteRoom(UUID roomID, Socket socket){
        try{
            DeleteRoomMessage roomDeleteMessage = new DeleteRoomMessage( user.getUsername(), user.getUserId(), roomID);
            sendMessage(socket, roomDeleteMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if a user exists in the network.
     * This method iterates through the list of peers to check if the user exists.
     *
     * @param peerId The ID of the peer to be checked.
     * @return true if the user exists, false otherwise.
     */
    private boolean userExists(UUID peerId) {
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(peerId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a peer message to a specified user in the network.
     * This method iterates through the list of peers to find the user and sends the message.
     *
     * @param userId The ID of the user to send the message to.
     * @param message The message to be sent.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendPeerMessage(UUID userId, ConnectMessage message) throws IOException {
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(userId)) {
                message.setType(PEER);
                sendMessage(user.getListeningSocket(), message);
                return;
            }
        }
        System.out.println("User not found" );
    }

    /**
     * Sends a peer reconnect message to a specified user in the network.
     * This method iterates through the list of peers to find the user and sends the message.
     *
     * @param userId The ID of the user to send the message to.
     * @param message The message to be sent.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendPeerReconnectMessage(UUID userId, ConnectMessage message) throws IOException{
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(userId)) {
                message.setType(RECONNECT_PEER);
                sendMessage(user.getListeningSocket(), message);
                return;
            }
        }
        System.out.println("User not found" );
    }

    /**
     * Sends a change username or reconnect message to a specified socket.
     * This method sends a change username or reconnect message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendChangeUsernameOrReconnectMessage(Socket socket) throws IOException {
        ConnectMessage changeUsernameOrReconnectMessage = new ConnectMessage(serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());
        changeUsernameOrReconnectMessage.setType(CHANGE_USERNAME_OR_RECONNECT);
        sendMessage(socket, changeUsernameOrReconnectMessage);
    }

    /**
     * Sends a reconnect message to a specified socket.
     * This method sends a reconnect message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendReconnectMessage(Socket socket) throws IOException{
        ConnectMessage reconnectMessage = new ConnectMessage(serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());
        reconnectMessage.setType(RECONNECT);
        sendMessage(socket, reconnectMessage);
    }

    /**
     * Sends a response reconnect message to a specified socket.
     * This method sends a response reconnect message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @param newId The new ID of the user.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendResponseReconnectMessage(Socket socket, UUID newId) throws IOException{
        ConnectMessage responseReconnectMessage = new ResponseReconnectMessage(serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId(), newId, user.getDisconnectedUser());
        sendMessage(socket, responseReconnectMessage);
    }

    /**
     * Sends a discovery message to a specified socket.
     * This method sends a discovery message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendDiscoveryMessage(Socket socket) throws IOException {
        ConnectMessage discoveryMessage = new ConnectMessage(serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());
        discoveryMessage.setType(DISCOVERY);
        sendMessage(socket, discoveryMessage);
    }

    /**
     * Sends a response discovery message to a specified socket.
     * This method sends a response discovery message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendResponseDiscoveryMessage(Socket socket) throws IOException {
        ConnectMessage responseDiscoveryMessage = new ConnectMessage(serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());
        responseDiscoveryMessage.setType(RESPONSE_DISCOVERY);
        sendMessage(socket, responseDiscoveryMessage);
    }

    /**
     * Send a change username message to a specified socket.
     * This method sends a change username message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    public void sendChangeUsernameMessage(Socket socket) throws IOException {
        ConnectMessage changeUsernameMessage = new ConnectMessage(serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());
        changeUsernameMessage.setType(CHANGE_USERNAME);
        sendMessage(socket, changeUsernameMessage);
    }

    /**
     * Sends a room initialization message to a specified socket.
     * This method sends a room initialization message to the specified socket.
     *
     * @param room The room to be initialized.
     * @param socket The socket of the peer that the message is sent to.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    public void sendRoom(Room room, Socket socket) throws IOException {
        RoomInitMessage roomMessage = new RoomInitMessage(user.getUsername(), user.getUserId(), room);
        sendMessage(socket, roomMessage);
    }

    /**
     * Sends a room message to a specified socket.
     * This method sends a room message to the specified socket.
     *
     * @param message The room message to be sent.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    public void sendRoomMessage(RoomMessage message) throws IOException {

        // Debugger mode per verificare che i vector clock funzionino bene anche empiricamente
        Scanner scanner = new Scanner(System.in);
        String debug;
        Map<UUID, Integer> userTime = new HashMap<>();
        for (UUID userId : user.getRoom().getParticipants()){
            if(!userId.equals(user.getUserId())){
                userTime.put(userId, 0);
            }
        }
        if(message.getContent().contains("DEBUG") || message.getContent().contains("debug")){
            System.out.println(Color.GRAY + "Wanna enter debug mode? (y/n)" + Color.RESET);
            debug = scanner.nextLine();
            if(debug.equals("y") || debug.equals("Y")){
                int delay;
                for (UUID userId : user.getRoom().getParticipants()){
                    if(!userId.equals(user.getUserId())){
                        System.out.println(Color.GRAY + "Select the delay time for " + userId + " (in milliseconds)" + Color.RESET);
                        delay = scanner.nextInt();
                        userTime.put(userId, delay);
                    }
                }
            }
        }

        for (UUID userId : user.getRoom().getParticipants()){
            if(!userId.equals(user.getUserId())){
                try {
                    Thread.sleep(userTime.get(userId));
                } catch (InterruptedException e) {
                    System.err.println("Sleep was interrupted.");
                }
                sendMessage(user.findPeerByUUID(userId).getListeningSocket(), message);
            }
        }
    }

    /**
     * Receives a message from a specified socket.
     * This method receives a message from the specified socket.
     *
     * @param socket The socket of the peer that the message is received from.
     * @return The message received from the socket.
     */
    private Message getMessage(Socket socket){
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            return (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Sends a message to a specified socket.
     * This method sends a message to the specified socket.
     *
     * @param socket The socket of the peer that the message is sent to.
     * @param message The message to be sent.
     * @throws IOException If an input or output exception occurred during the message sending.
     */
    private void sendMessage(Socket socket, Message message) throws IOException{
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(message);
        out.flush();
        //out.reset();
    }

    /**
     * Connects to a peer in the network.
     * This method attempts to connect to a peer in the network by sending a discovery message.
     * It retries the connection up to 5 times before closing the client.
     *
     * @param ipPeer The IP address of the peer to connect to.
     * @param portPeer The port of the peer to connect to.
     * @param sender The user that is connecting to the peer.
     */
    public void connectToPeer(String ipPeer, int portPeer, User sender) {
        int MAX_RETRIES = 5;
        for(int i = 0; i < MAX_RETRIES; i++) {
            try {
                Socket socket = new Socket(ipPeer, portPeer);
                CountDownLatch latch = new CountDownLatch(1);
                new Thread(() -> {
                    try {
                        handleIncomingConnection(socket,latch);
                    } catch (IOException e) {
                        System.out.println(Color.RED + "Connection crashed" + Color.RESET);
                    }
                }, "handleIncomingConnectionClientSide_").start();
                try {
                    Thread.sleep(1000);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
                sendDiscoveryMessage(socket);
                Map<UUID, Room> rooms = sender.getRooms();
                for(UUID roomId : rooms.keySet()){
                    sendRoom(rooms.get(roomId), socket);
                }
                try{
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            } catch (IOException e) {
                System.out.println(Color.RED + "Connection crashed, retrying discovery..." + Color.RESET);
                try {
                    int RETRY_WAIT_TIME = 1000;
                    Thread.sleep(RETRY_WAIT_TIME); // introduce delay before next retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                if (i == MAX_RETRIES - 1) {
                    System.out.println(Color.RED + "Failed to reconnect after " + MAX_RETRIES + " attempts, closing client." + Color.RESET);
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Starts listening for incoming connections.
     * This method starts listening for incoming connections on the user's port.
     */
    public void startListening() {
        try {
            serverSocket = new ServerSocket(user.getPort());
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Ignore loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                // Get the addresses for this interface
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // Print only IPv4 addresses (IPv6 can be handled similarly if needed)
                    if (addr instanceof Inet4Address) {
                        System.out.println(Color.GREEN + "Your IP is: " + addr.getHostAddress() +
                                " Listening for incoming connections on port " + user.getPort() + Color.RESET);
                    }
                }
            }

            // Start a new thread that handles incoming connections
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        CountDownLatch latch = new CountDownLatch(0);
                        new Thread(() -> {
                            try {
                                handleIncomingConnection(socket, latch);
                            } catch (IOException e) {
                                System.out.println(Color.RED + "Connection crashed" + Color.RESET);
                            }
                        }, "handleIncomingConnectionServerSide_").start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(Color.RED + "Error in establishing the connection" + Color.RESET);
                    }
                }
        } catch (IOException e) {
            System.out.println(Color.RED + "Failed to start listening on port " + user.getPort() +Color.RESET);
            System.exit(1);
        }
    }

    /**
     * Starts the heartbeat mechanism.
     * This method starts the heartbeat mechanism by sending a heartbeat message every second.
     *
     * @param peerSocket The socket of the peer to send the heartbeat message to.
     */
    public void startHeartbeat(Socket peerSocket) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    sendMessage(peerSocket, new HeartbeatMessage(user.getUsername(), user.getUserId()));
                } catch (IOException | InterruptedException e) {
                    break;
                }
            }
        }, "heartbeat_").start();
    }



}