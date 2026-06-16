package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ControlWindow {

    private static final String USERNAME = "mangos";
    private static final String PASSWORD = "mangos";
    private static final String HOST     = "127.0.0.1";
    private static final String PORT     = "3306";
    private static final String DATABASE = "characters";
    private final JTextField      userInput;
    private final JPasswordField  passInput;
    private final JTextField hostInput;
    private final JTextField portInput;
    private final JTextField databaseInput;
    private final JButton runButton;
    private final JTextArea outputText;

    public ControlWindow(){
        userInput = new JTextField(USERNAME);
        passInput = new JPasswordField(PASSWORD);
        hostInput = new JTextField(HOST);
        portInput = new JTextField(PORT);
        databaseInput = new JTextField(DATABASE);
        runButton = new JButton("Run Cleaner");
        outputText = new JTextArea();

        Font inputFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        Font buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, 16);

        userInput.setFont(inputFont);
        passInput.setFont(inputFont);
        hostInput.setFont(inputFont);
        portInput.setFont(inputFont);
        databaseInput.setFont(inputFont);
        runButton.setFont(buttonFont);
        JFrame frame = new JFrame();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int windowWidth = screenSize.width / 2;
        int windowHeight = screenSize.height / 2;

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = createInputPanel();
        JPanel outputPanel = createOutputPanel();

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(outputPanel, BorderLayout.CENTER);

        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("VMangos Character DB Cleaner");

        frame.setSize(windowWidth, windowHeight);
        frame.setMinimumSize(new Dimension(500, 400));

        frame.setLocationRelativeTo(null);

        frame.setResizable(true);
        frame.setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Database Connection"));
        GridBagConstraints gbc = new GridBagConstraints();

        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(labelFont);
        inputPanel.add(usernameLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(userInput, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(labelFont);
        inputPanel.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(passInput, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setFont(labelFont);
        inputPanel.add(hostLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(hostInput, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(labelFont);
        inputPanel.add(portLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(portInput, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel databaseLabel = new JLabel("Database:");
        databaseLabel.setFont(labelFont);
        inputPanel.add(databaseLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        inputPanel.add(databaseInput, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0;
        runButton.setPreferredSize(new Dimension(190, 40));
        inputPanel.add(runButton, gbc);

        return inputPanel;
    }

    private JPanel createOutputPanel() {
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));

        outputText.setEditable(false);
        outputText.setLineWrap(true);
        outputText.setWrapStyleWord(true);
        outputText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        outputText.setBackground(Color.BLACK);
        outputText.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(outputText);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        outputPanel.add(scrollPane, BorderLayout.CENTER);
        return outputPanel;
    }

    public void addListener(IRunListener listener){
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = userInput.getText();
                String pass = new String(passInput.getPassword());
                String host = hostInput.getText();
                String port     = portInput.getText();
                String database = databaseInput.getText();
                String location = String.format("jdbc:mysql://%s:%s/%s", host, port, database);
                listener.run(user, pass, location);
            }
        });
    }

    public void appendResultText(String text){
        if (SwingUtilities.isEventDispatchThread()) {
            outputText.append(text);
            outputText.setCaretPosition(outputText.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                outputText.append(text);
                outputText.setCaretPosition(outputText.getDocument().getLength());
            });
        }
    }

    public void clearResultText(){
        if (SwingUtilities.isEventDispatchThread()) {
            outputText.setText("");
        } else {
            SwingUtilities.invokeLater(() -> outputText.setText(""));
        }
    }

    public void setRunButtonEnabled(boolean enabled) {
        if (SwingUtilities.isEventDispatchThread()) {
            runButton.setEnabled(enabled);
            runButton.setText(enabled ? "Run Cleaner" : "Running...");
        } else {
            SwingUtilities.invokeLater(() -> {
                runButton.setEnabled(enabled);
                runButton.setText(enabled ? "Run Cleaner" : "Running...");
            });
        }
    }
}
