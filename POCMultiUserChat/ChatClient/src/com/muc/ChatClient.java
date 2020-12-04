package com.muc;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ChatClient {
    /** The server name */
    private final String serverName;

    /** The server port */
    private final int serverPort;

    /** The server socket */
    private Socket socket;

    /** The output stream of the socket */
    private OutputStream serverOut;

    /** The input stream of the socket */
    private InputStream serverIn;

    /** Buffer that is used to read the server output stream */
    private BufferedReader bufferedIn;

    /** Stores the user listeners of this chat client */
    private ArrayList<UserStatusListener> userStatusListeners = new ArrayList<>();

    /** Constructor of the server client class */
    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 8818);

        // Register listeners
        client.addUserStatusListener(new UserStatusListener() {
            @Override
            public void online(String login) {
                System.out.println("ONLINE: " + login);
            }
            @Override
            public void offline(String login) {
                System.out.println("OFFLINE: " + login);
            }
        });

        // Connect client instance to the server
        if (!client.connect()) {
            System.err.println("Connection failed");
        } else {
            System.out.println("Connection successful");
            // Connect user
            if (client.login("guest", "guest")) {
                System.out.println("Login successful");
            } else {
                System.err.println("Login failed");
            }
            
            client.logoff();
        }
    }

    /** Logs the user by sending login command to the server */
    private boolean login(String login, String password) throws IOException {
        // Send login command to server
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());
        // Get response line
        String response = bufferedIn.readLine();
        System.out.println("Response line: " + response);
        // Check if login was successful
        if ("ok login".equalsIgnoreCase(response)) {
            // Start reading responses from the server
            startMessageReader();
            return true;
        }
        return false;
    }

    /** Logs the user off by sending login command to the server */
    private void logoff() throws IOException {
        // Send logoff command to server
        String cmd = "logoff\n";
        serverOut.write(cmd.getBytes());
    }

    /** Reads responses from the server */
    private void startMessageReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    readMessageLoop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /** Infinite loop that reads line by line from the server output, which is going to be our client input */
    private void readMessageLoop() throws IOException {
        try {
            String line;
            while ( (line = bufferedIn.readLine()) != null) {
                // Split line into individual tokens based on whitespace character
                String[] tokens = StringUtils.split(line);
                // Check if the tokens list is valid
                if (tokens != null && tokens.length > 0) {
                    // Get first token of line
                    String cmd = tokens[0];
                    // Online presence message
                    if ("online".equalsIgnoreCase(cmd)) {
                        handleOnline(tokens);
                    // Offline presence message
                    } else if ("offline".equalsIgnoreCase(cmd)) {
                        handleOffline(tokens);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Close the socket
            try {
                socket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /** Handles the offline presence message
     *  format: "offline" login */
    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        // Call the offline method of all the registered user status listeners
        for (UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }

    /** Handles the online presence message
     * format: "online" login */
    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        // Call the online method of all the registered user status listeners
        for (UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    /** Establishes the connection to the server */
    private boolean connect() {
        try {
            // Create socket
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            // Initialize server output and input streams
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            // Initialize buffer used to read the server output stream line by line
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Registers a listener to this chat client */
    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
    }

    /** Removes a listener from this chat client */
    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }
}
