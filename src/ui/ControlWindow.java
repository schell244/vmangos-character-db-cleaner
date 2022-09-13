package ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ControlWindow {

    private static final String USERNAME = "mangos";
    private static final String PASSWORD = "mangos";
    private static final String HOST     = "127.0.0.1";
    private static final String PORT     = "3306";
    private final JTextField userInput;
    private final JTextField passInput;
    private final JTextField hostInput;
    private final JTextField portInput;
    private final JButton runButton;
    private final JTextArea outputText;

    public ControlWindow(){
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel userLabel = new JLabel("Username: ");
        userLabel.setBounds(120,20,80,25);
        userInput = new JTextField();
        userInput.setBounds(210,20,200,25);
        userInput.setText(USERNAME);

        JLabel passLabel = new JLabel("Password: ");
        passLabel.setBounds(120,50,80,25);
        passInput = new JTextField();
        passInput.setBounds(210,50,200,25);
        passInput.setText(PASSWORD);

        JLabel hostLabel = new JLabel("Host: ");
        hostLabel.setBounds(120,80,80,25);
        hostInput = new JTextField();
        hostInput.setBounds(210,80,200,25);
        hostInput.setText(HOST);

        JLabel portLabel = new JLabel("Port: ");
        portLabel.setBounds(120,110,80,25);
        portInput = new JTextField();
        portInput.setBounds(210,110,200,25);
        portInput.setText(PORT);

        runButton = new JButton("Run Cleaner");
        runButton.setBounds(120,160,140,25);

        outputText = new JTextArea ();
        outputText.setEditable(false);
        outputText.setLineWrap(true);
        outputText.setBounds(80,200,430,400);

        panel.add(userLabel);
        panel.add(userInput);
        panel.add(passLabel);
        panel.add(passInput);
        panel.add(hostLabel);
        panel.add(hostInput);
        panel.add(portLabel);
        panel.add(portInput);
        panel.add(runButton);
        panel.add(outputText);

        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("vmangos-character-db-cleaner");
        frame.setSize(600,700);
        frame.setLocationRelativeTo(null); // center
        frame.setVisible(true);
    }

    public void addListener(IRunListener listener){
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = userInput.getText();
                String pass = passInput.getText();
                String host = hostInput.getText();
                String port = portInput.getText();
                String location = String.format("jdbc:mysql://%s:%s/characters", host, port);
                listener.run(user,pass,location);
            }
        });
    }

    public void showResultText(String result){
        outputText.setText(result);
    }
}
