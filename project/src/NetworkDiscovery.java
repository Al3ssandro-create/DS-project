import java.io.*;
import java.net.*;

public class NetworkDiscovery {
    private User user;

    public NetworkDiscovery(User user) {
        this.user = user;
    }
    private ServerSocket serverSocket;
    public void discoverPeers() {
        try {
            // Create a server socket that listens for incoming connections on a specific port
            serverSocket = new ServerSocket(user.getPort());

            // Start a new thread that handles incoming connections
            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new Thread(() -> {
                            try {
                                handleIncomingConnection(socket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // Send a discovery message to all other ports
            for (int port = 1024; port <= 49151; port++) {
                if (port == user.getPort()) {
                    continue;
                }

                try {
                    Socket socket = new Socket("localhost", port);
                    sendDiscoveryMessage(socket);
                    socket.close();
                } catch (IOException e) {
                    // Ignore exceptions, as they are expected when a connection attempt fails
                }
            }
        } catch (IOException e) {
            // Handle exception
        }
    }

    private void handleIncomingConnection(Socket socket) throws IOException {
        String responseMessage = getResponseMessage(socket);
        if (responseMessage != null && (responseMessage.startsWith("DISCOVERY") || responseMessage.startsWith("RESPONSE_DISCOVERY"))) {
            String[] parts = responseMessage.split(" ");
            String peerUsername = parts[1];
            String peerAddress = parts[3];
            int peerPort = Integer.parseInt(parts[2]);
            User peer = new User(peerUsername, peerPort, new Socket(peerAddress, peerPort));
            this.user.listPeers().add(peer);
            if(responseMessage.startsWith("DISCOVERY")) {
                PrintWriter out = new PrintWriter(peer.getSocket().getOutputStream(), true);
                String responseBackMessage = "RESPONSE_DISCOVERY " + this.user.getUsername() + " " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress().getHostAddress();
                out.println(responseBackMessage);
            }
        }
    }
    public void sendMessageToUser(String username, String message) throws IOException {
        for (User user : this.user.listPeers()) {
            if (user.getUsername().equals(username)) {
                PrintWriter out = new PrintWriter(user.getSocket().getOutputStream(), true);
                out.println(message);
                return;
            }
        }
        System.out.println("User not found: " + username);
    }

    private void sendDiscoveryMessage(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String discoveryMessage = "DISCOVERY " + this.user.getUsername() + " " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress().getHostAddress();
        out.println(discoveryMessage);
    }

    private String getResponseMessage(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return in.readLine();
    }
}