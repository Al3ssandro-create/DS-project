package Communication;

import Color.Color;
import Entities.Room;
import Entities.User;
import Message.*;
import UI.PeerCLI;

import java.io.*;
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
                    //System.out.println(responseMessage + responseMessage.getType());
                    switch (responseMessage.getType()) {
                        case RESPONSE_RECONNECT:
                            user.setUserId(((ResponseReconnectMessage) responseMessage).getNewUserId());
                            user.setDisconnectedUser(((ResponseReconnectMessage) responseMessage).getDisconnectedUser());
                        case RESPONSE_DISCOVERY:
                            latch.countDown();
                        case DISCOVERY:
                        case PEER:
                            String peerUsername = responseMessage.getSender();
                            UUID peerId = responseMessage.getSenderId();
                            int peerPort = ((ConnectMessage) responseMessage).getPort();
                            String peerAddress = ((ConnectMessage) responseMessage).getIp();
                            if(!userExists(peerId) && !responseMessage.getType().equals(PEER) ) {
                                if(!user.inDisconnected(peerUsername)){
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
                                    if (!peerUser.getUserId().equals(peerId)) {
                                        sendPeerMessage(peerUser.getUserId(), new ConnectMessage(0, peerPort, peerAddress, peerUsername, peerId));
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
                                    sendPeerReconnectMessage(peerUser.getUserId(), new ConnectMessage(0, peerPortRe, peerAddressRe, peerUsernameRe, handlingUser.getUserId()));
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
                            /*UUID reconnectedUUID = responseMessage.getSenderId();
                            User reconnected = user.findPeerByUUID(reconnectedUUID);
                            Map<UUID, Room> roomsPeer = user.getRooms();
                            if(!user.commonRooms(reconnected.getUserId()).isEmpty()){
                                for(UUID commonRoomId : user.commonRooms(reconnected.getUserId()).keySet()){
                                        sendRoom(roomsPeer.get(commonRoomId), reconnected.getListeningSocket());
                                }
                            } */
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

    private void reconnect(Socket socket) {
        try {
            sendReconnectMessage(socket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void changeUsername(Socket socket) {
        try {
            sendDiscoveryMessage(socket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void deleteRoom(UUID roomID, Socket socket){
        try{
            DeleteRoomMessage roomDeleteMessage = new DeleteRoomMessage( user.getUsername(), user.getUserId(), 0, roomID);
            sendMessage(socket, roomDeleteMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private boolean userExists(UUID peerId) {
        for (User user : this.user.listPeers()) {
            if (user.getUserId().equals(peerId)) {
                return true;
            }
        }
        return false;
    }

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

    private void sendChangeUsernameOrReconnectMessage(Socket socket) throws IOException {
        ConnectMessage changeUsernameOrReconnectMessage = new ConnectMessage(0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        changeUsernameOrReconnectMessage.setType(CHANGE_USERNAME_OR_RECONNECT);
        sendMessage(socket, changeUsernameOrReconnectMessage);
    }

    private void sendReconnectMessage(Socket socket) throws IOException{
        ConnectMessage reconnectMessage = new ConnectMessage(0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        reconnectMessage.setType(RECONNECT);
        sendMessage(socket, reconnectMessage);
    }

    private void sendResponseReconnectMessage(Socket socket, UUID newId) throws IOException{
        ConnectMessage responseReconnectMessage = new ResponseReconnectMessage(0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId(), newId, user.getDisconnectedUser());//TODO: sequence number
        sendMessage(socket, responseReconnectMessage);
    }

    private void sendDiscoveryMessage(Socket socket) throws IOException {
        ConnectMessage discoveryMessage = new ConnectMessage( 0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        discoveryMessage.setType(DISCOVERY);
        sendMessage(socket, discoveryMessage);
    }

    private void sendResponseDiscoveryMessage(Socket socket) throws IOException {
        ConnectMessage responseDiscoveryMessage = new ConnectMessage(0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        responseDiscoveryMessage.setType(RESPONSE_DISCOVERY);
        sendMessage(socket, responseDiscoveryMessage);
    }

    public void sendChangeUsernameMessage(Socket socket) throws IOException {
        ConnectMessage changeUsernameMessage = new ConnectMessage(0, serverSocket.getLocalPort(), serverSocket.getInetAddress().getHostAddress(), user.getUsername(), user.getUserId());//TODO: sequence number
        changeUsernameMessage.setType(CHANGE_USERNAME);
        sendMessage(socket, changeUsernameMessage);
    }

    public void sendRoom(Room room, Socket socket) throws IOException {
        RoomInitMessage roomMessage = new RoomInitMessage(0, user.getUsername(), user.getUserId(), room); // TODO: sequence number
        sendMessage(socket, roomMessage);
    }

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
                Integer delay;
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
                //System.out.println(Color.RED + "Vector proprio prima di mandare:" + message.getContent() + " a " + userId + Color.RESET);
                //System.out.println(message.getVectorClock().toString());
                sendMessage(user.findPeerByUUID(userId).getListeningSocket(), message);
            }
        }
    }

    private Message getMessage(Socket socket){
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(socket.getInputStream());
            return (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    private void sendMessage(Socket socket, Message message) throws IOException{
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(message);
        out.flush();
    }

    public void connectToPeer(String ipPeer, int portPeer, User sender) {
        int MAX_RETRIES = 5;
        for(int i = 0; i < MAX_RETRIES; i++) {
            try {
                Socket socket = new Socket(ipPeer, portPeer);
                sendDiscoveryMessage(socket);
                Map<UUID, Room> rooms = sender.getRooms();
                    for(UUID roomId : rooms.keySet()){
                        sendRoom(rooms.get(roomId), socket);
                    }
                CountDownLatch latch = new CountDownLatch(1);
                new Thread(() -> {
                    try {
                        handleIncomingConnection(socket,latch);
                    } catch (IOException e) {
                        System.out.println(Color.RED + "Connection crashed" + Color.RESET);
                    }
                }, "handleIncomingConnectionClientSide_").start();
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
                    }
                }
        } catch (IOException e) {
            System.out.println(Color.RED + "Failed to start listening on port " + user.getPort() +Color.RESET);
            System.exit(1);
        }
    }

    public void startHeartbeat(Socket peerSocket) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    sendMessage(peerSocket, new HeartbeatMessage(user.getUsername(), 0, user.getUserId())); // TODO: sequence number
                } catch (IOException | InterruptedException e) {
                    break;
                }
            }
        }, "heartbeat_").start();
    }

}