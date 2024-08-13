package UI;

import Color.Color;
import Entities.Room;
import Entities.User;

import java.util.*;
import java.util.regex.Pattern;

public class PeerCLI {
    private static User user;
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Main method
     * @param args arguments
     */
    public static void main(String[] args) {

        System.out.print(Color.BLUE + "Enter your username: " + Color.RESET);
        String username = scanner.nextLine();
        while (username.isEmpty()) {
            System.out.println(Color.RED + "Username cannot be empty, please insert a valid username" + Color.RESET);
            username = scanner.nextLine();
        }
        System.out.print(Color.BLUE + "Enter your port (must be between 1024 and 49151): " + Color.RESET);
        int PORT = scanner.nextInt();
        while(PORT < 1024 || PORT > 49151){
            System.out.println(Color.RED + "Port must be between 1024 and 49151" + Color.RESET);
            PORT = scanner.nextInt();
        }
        scanner.nextLine();
        user = new User(username, PORT);
        System.out.println(Color.BLUE + "Do you already know someone in the network? (y/n)" + Color.RESET);
        boolean loop = true;
        while(loop) {
            loop = false;
            String answer = scanner.nextLine();
            if (answer.equals("y") || answer.equals("Y") || answer.equals("yes") || answer.equals("Yes")) {
                System.out.println(Color.BLUE + "Enter the ip of a peer to connect to: " + Color.RESET);
                String ipPeer = scanner.nextLine();
                while(!validIp(ipPeer)){
                    System.out.println(Color.RED + "Peer's IP not valid" + Color.RESET);
                    ipPeer = scanner.nextLine();
                }
                System.out.println(Color.BLUE + "Enter the port of a peer to connect to: " + Color.RESET);
                int portPeer = scanner.nextInt();
                scanner.nextLine();
                while(portPeer < 1024 || portPeer > 49151 || portPeer == PORT){
                    System.out.println(Color.RED + "Peer's port must be between 1024 and 49151" + Color.RESET);
                    portPeer = scanner.nextInt();
                    scanner.nextLine();
                }
                user.startConnection(ipPeer, portPeer);
            }else if(!answer.equals("n") && !answer.equals("N") && !answer.equals("no") && !answer.equals("No")) {
                System.out.println(Color.RED + "Invalid choice. Please enter y or n." + Color.RESET);
                loop = true;
            }
        }
        while (true) {
            System.out.println(Color.BLUE + "1) Create a room \n2) See the list of all your rooms\n3) See the chat in a particular room\n4) List all peers\n5) Delete a Room\n6) Exit" + Color.RESET);
            int choice = 0;
            boolean validInput = false;
            while (!validInput) {
                System.out.print(Color.BLUE + "Enter your choice: " + Color.RESET);
                if (scanner.hasNextInt()) {
                    choice = scanner.nextInt();
                    validInput = true;
                } else {
                    System.out.println(Color.RED + "Invalid input. Please enter a number." + Color.RESET);
                    scanner.nextLine(); // consume the invalid input
                }
            }
            scanner.nextLine();
            switch (choice) {
                case 1 -> {
                    System.out.print(Color.BLUE + "Creating room name: " + Color.RESET);
                    String roomName = scanner.nextLine();
                    if (roomName.isEmpty()) {
                        System.out.println(Color.RED + "Room name cannot be empty" + Color.RESET);
                        continue;
                    }
                    user.createRoom(roomName);
                }
                case 2 -> {
                    System.out.println(Color.GREEN);
                    user.listRooms();
                    System.out.println(Color.RESET);
                }
                case 3 -> {
                    System.out.print(Color.BLUE + "Enter room name: " + Color.RESET);
                    String roomToEnter = scanner.nextLine();
                    Room room = user.findRoom(roomToEnter);
                    if (room == null) {
                        System.out.println(Color.RED + "Room not found" + Color.RESET);
                        break;
                    } else {
                        user.setRoom(room);
                        user.viewChat(roomToEnter);
                    }
                    System.out.println(Color.RESET);
                    while (user.getRoom() != null) {
                        System.out.println(Color.BLUE + "1) Add a message to the room chat\n2) Refresh the chat\n3) Exit the room" + Color.RESET);
                        int roomChoice = 0;
                        validInput = false;
                        while (!validInput) {
                            System.out.print(Color.BLUE + "Enter your choice: " + Color.RESET);
                            if (scanner.hasNextInt()) {
                                roomChoice = scanner.nextInt();
                                validInput = true;
                            } else {
                                System.out.println(Color.RED + "Invalid input. Please enter a number." + Color.RESET);
                                scanner.nextLine(); // consume the invalid input
                            }
                        }
                        scanner.nextLine();
                        switch (roomChoice) {
                            case 1 -> {
                                System.out.print(Color.BLUE + "Enter your message: " + Color.RESET);
                                String message = scanner.nextLine();
                                user.addMessageToRoomAndSend(message);
                                user.viewChat(roomToEnter);
                            }
                            case 2 -> user.viewChat(roomToEnter);
                            case 3 -> {
                                System.out.println(Color.BLUE + "Exiting room..." + Color.RESET);
                                user.setRoom(null);
                            }
                            default ->
                                    System.out.println(Color.RED + "Invalid choice. Please enter a number between 1 and 3" + Color.RESET);
                        }
                    }
                }
                case 4 -> {
                    System.out.println(Color.GREEN);
                    if (user.listPeers().isEmpty()) {
                        System.out.println(Color.RED + "No peers found" + Color.RESET);
                    }
                    for (User peer : user.listPeers()) {
                        System.out.println(Color.GREEN + peer.getUsername() + " on port: " + peer.getPort() + "\n" + Color.RESET);
                    }
                    System.out.println(Color.RESET);
                }
                case 5 -> {
                    System.out.print(Color.BLUE + "Enter the name of the room you want to delete: " + Color.RESET);
                    String roomToDeleteName = scanner.nextLine();
                    Room roomToDelete = user.findRoom(roomToDeleteName);
                    user.deleteRoomAndForward(roomToDelete);
                }
                case 6 -> {
                    System.out.println(Color.BLUE + "Exiting..." + Color.RESET);
                    scanner.close();
                    System.exit(0); // Exit the program
                }
                default ->
                        System.out.println(Color.RED + "Invalid choice. Please enter a number between 1 and 5." + Color.RESET);
            }
        }
    }
    /**
     * Reconnect or change the username
     * @return 1 if the user wants to reconnect, 2 if the user wants to change the username
     */
    public static int ReconnectOrChangeUsername(){
        System.out.println(Color.RED + "Username already in use, where you already connected? (y/n)" + Color.RESET);
        while(true) {
            String answer = scanner.nextLine();
            if (answer.equals("y") || answer.equals("Y") || answer.equals("yes") || answer.equals("Yes")) {
                return 1;
            }else if(answer.equals("n") || answer.equals("N") || answer.equals("no") || answer.equals("No")) {
                System.out.print(Color.BLUE + "Enter new username: " + Color.RESET);
                String newUsername = scanner.nextLine();
                user.setUsername(newUsername);
                System.out.println(Color.GREEN + "Username changed to: " + newUsername + Color.RESET);
                return 2;
            }else{
                System.out.println(Color.RED + "Invalid choice. Please enter y or n." + Color.RESET);
            }
        }
    }

    /**
     * Change the username
     */
    public static void ChangeUsername() {
        System.out.println(Color.RED + "Username already in use, change username: " + Color.RESET);
        String newUsername = scanner.nextLine();
        user.setUsername(newUsername);
        System.out.println(Color.GREEN + "Username changed to: " + newUsername + Color.RESET);
    }

    /**
     * Check if the ip is valid
     * @param ip ip to check
     * @return true if the ip is valid, false otherwise
     */
    private static boolean validIp(final String ip) {
        Pattern PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        return PATTERN.matcher(ip).matches();
    }
}