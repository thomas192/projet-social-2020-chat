package poc;

import redis.clients.jedis.Jedis;

import poc.data.ClientDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class LoginWindow extends JFrame {

    /** Instance of the client API so that we can login */
    private final ChatClient client;

    /** ClientDAO instance */
    private ClientDAO clientDAO = new ClientDAO();

    /** Input field for login */
    JTextField loginField = new JTextField();

    /** Input field for password */
    JPasswordField passwordField = new JPasswordField();

    /** Login button */
    JButton loginButton = new JButton("Login");

    public LoginWindow() {
        super("Login");

        this.client = new ChatClient("localhost", 8818);
        // Connect to the server
        client.connect();

        // Set behavior on close
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    client.logoff();
                    dispose();
                    System.exit(0);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        // Create new layout
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        // Add fields and button to layout
        p.add(loginField);
        p.add(passwordField);
        p.add(loginButton);
        // Login button is clicked
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });
        getContentPane().add(p, BorderLayout.CENTER);
        // Resize window to fit all the different components
        pack();
        setVisible(true);
    }

    private void doLogin() {
        String login = loginField.getText();
        String password = passwordField.getText();

        try {
            if (client.login(login, password)) {
                // Bring up the user list window
                // Create user pane that takes the client
                MainPane mainPane = new MainPane(client);
                mainPane.setTopicsFollowed(clientDAO.fetchTopicsFollowed(login));

                Jedis jedis = new Jedis("localhost");
                // Get conversations history from redis
                Set<String> res = jedis.smembers(login);
                if (res.size() == 0) {
                    // Initialize conversations history in redis
                    for (String l : clientDAO.fetchConversationsHistory(login)) {
                        jedis.sadd(login, l);
                    }
                    res = jedis.smembers(login);
                }
                jedis.disconnect();
                mainPane.setConversationsHistory(new ArrayList<>(res));
                // userListPane.setConversationsHistory(clientDAO.fetchConversationsHistory(login));
                mainPane.display();
                // Create the user list window
                JFrame userListWindow = new JFrame("Main pane");
                userListWindow.setSize(400, 600);
                // Set behavior on close
                userListWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                userListWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        try {
                            client.logoff();
                            userListWindow.dispose();
                            System.exit(0);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                });
                // Add the user list pane as the main component
                userListWindow.getContentPane().add(mainPane, BorderLayout.CENTER);
                userListWindow.setVisible(true);
                // Remove the login window
                setVisible(false);
            } else {
                // Show error message
                JOptionPane.showMessageDialog(this, "Invalid login/password");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LoginWindow loginWindow = new LoginWindow();
        loginWindow.setVisible(true);
    }
}
