package org.example.chatroom.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    // roomCode -> list of clients
    private static final Map<String, List<ClientHandler>> rooms = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234); //Create a server
        System.out.println("Server started on port 1234...");

        while (true) { //always accept clients trying to join
            Socket socket = serverSocket.accept();
            new ClientHandler(socket).start(); //each client is its own thread
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader input;
        private PrintWriter output;

        private boolean joined = false;

        private String username;
        private String roomCode;

        public ClientHandler(Socket socket) { //constructor
            this.socket = socket; //store the connection
        }

        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream())); //get what the user says
                output = new PrintWriter(socket.getOutputStream(), true); //send what the user says
                //auto flush to send it instantly

                String joinMsg = input.readLine(); //The client must send "JOIN room1 Alice" for example
                String[] parts = joinMsg.split(" "); //Splits the joinMsg into diff parts
                //parts[0] = JOIN
                //parts[1] = room1
                //parts[2] = Alice

                // 1. Validate join format
                if (parts.length < 3 || !parts[0].equals("JOIN")) { //make sure they are trying to join
                    output.println("ERROR Invalid join format. Use: JOIN room username");
                    socket.close();
                    return;
                }

                roomCode = parts[1];
                username = parts[2];

                // 2. Add to room safely + check duplicates
                synchronized (rooms) {
                    rooms.putIfAbsent(roomCode, new ArrayList<>()); //if roomCode doesnt exist make it

                    List<ClientHandler> clients = rooms.get(roomCode); //get the clients in the room

                    // 3. Prevent duplicate usernames in same room
                    for (ClientHandler client : clients) {
                        if (client.username != null &&
                                client.username.equalsIgnoreCase(username)) {

                            output.println("ERROR Username already taken in this room");
                            socket.close();
                            return;
                        }
                    }

                    // 4. Add user
                    clients.add(this);
                    joined = true;
                }

                // 5. Log on server
                System.out.println(username + " joined room " + roomCode);
                output.println("JOIN_SUCCESS");

                // 6. Notify everyone
                broadcast("[SYSTEM] " + username + " joined the room");
                broadcastAll(buildRoomUpdate(roomCode));

                String msg;
                while ((msg = input.readLine()) != null) {

                    if (msg.equals("TYPING")) {

                        broadcast("[TYPING] " + username);

                        continue;
                    }

                    if (msg.equals("STOP_TYPING")) {

                        broadcast("[STOP_TYPING] " + username);

                        continue;
                    }

                    String fullMsg = username + ": " + msg;

                    // PRINT TO SERVER CONSOLE
                    System.out.println("[" + roomCode + "] " + fullMsg);

                    // SEND TO CLIENTS
                    broadcast(fullMsg);
                }

            } catch (IOException e) {
                System.out.println(username + " disconnected");
            } finally {
                cleanup(); //delete all empty rooms
            }
        }
        private void broadcastAll(String message) {

            List<ClientHandler> clients;

            synchronized (rooms) {

                clients = rooms.get(roomCode);

                if (clients == null) {
                    return;
                }
            }

            for (ClientHandler client : clients) {

                client.output.println(message);
            }
        }

        private void broadcast(String message) {

            List<ClientHandler> clients;

            synchronized (rooms) {

                clients = rooms.get(roomCode);

                if (clients == null) return;
            }

            for (ClientHandler client : clients) {

                // don't echo back to sender
                if (client != this) {

                    client.output.println(message);
                }
            }
        }
        private String buildRoomUpdate(String roomCode) {
            List<ClientHandler> clients = rooms.get(roomCode);
            if (clients == null) return "USERS 0";

            StringBuilder sb = new StringBuilder();
            sb.append("USERS ").append(clients.size()).append(" ");

            for (ClientHandler c : clients) {
                if (c.username != null) {
                    sb.append(c.username).append(",");
                }
            }

            return sb.toString();
        }

        private void cleanup() {
            try {
                if (joined && roomCode != null) {
                    synchronized (rooms) {
                        List<ClientHandler> clients = rooms.get(roomCode);
                        if (clients != null) {
                            clients.remove(this);
                            if (clients.isEmpty()) { //if there is no one in the room
                                rooms.remove(roomCode); //delete the room
                            }
                        }
                    }

                    System.out.println(username + " left room " + roomCode);
                    broadcast("[SYSTEM] " + username + " left the room");
                    broadcastAll(buildRoomUpdate(roomCode));
                }

                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}