package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.service.NetworkService;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * A reusable panel for configuring a single remote test machine (either client or server role).
 */
public class RemoteMachinePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(RemoteMachinePanel.class);

    private final SshService sshService;
    private final NetworkService networkService;

    // UI Components
    private JComboBox<String> profileComboBox;
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton saveButton;
    private JButton deleteButton;
    private JComboBox<String> nicComboBox;

    public RemoteMachinePanel(String title) {
        this.sshService = new SshService();
        this.networkService = new NetworkService(sshService);

        setBorder(new TitledBorder(title));
        setLayout(new GridBagLayout());
        initComponents();
        addListeners();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Saved Profiles
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("历史配置:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; gbc.gridwidth = 2;
        profileComboBox = new JComboBox<>(new String[]{"新建连接..."});
        add(profileComboBox, gbc);

        // Row 1: Host
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("主机地址:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        hostField = new JTextField("");
        add(hostField, gbc);

        // Row 2: Username
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        usernameField = new JTextField("root");
        add(usernameField, gbc);

        // Row 3: Password
        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("密码:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        passwordField = new JPasswordField("");
        add(passwordField, gbc);

        // Row 4: Action Buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        connectButton = new JButton("连接");
        saveButton = new JButton("保存");
        deleteButton = new JButton("删除");
        buttonPanel.add(connectButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, gbc);

        // Row 5: NIC Selection
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 5; add(new JLabel("网卡 IP:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2;
        nicComboBox = new JComboBox<>(new String[]{"待连接..."});
        nicComboBox.setEnabled(false);
        add(nicComboBox, gbc);
    }

    private void addListeners() {
        connectButton.addActionListener(e -> handleConnection());
    }

    private void handleConnection() {
        String host = hostField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        log.info("Connect button clicked for host: {}", host);

        // Use a background thread for network operations to avoid freezing the GUI
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                log.debug("SwingWorker started for SSH connection.");
                sshService.connect(username, password, host, 22);
                return networkService.getRemoteIpAddresses();
            }

            @Override
            protected void done() {
                try {
                    List<String> ips = get();
                    log.info("Successfully fetched IPs: {}", ips);
                    nicComboBox.removeAllItems();
                    if (ips.isEmpty()) {
                        nicComboBox.addItem("未找到可用IP");
                    } else {
                        ips.forEach(nicComboBox::addItem);
                    }
                    nicComboBox.setEnabled(true);
                    setInputsEnabled(false);
                    JOptionPane.showMessageDialog(RemoteMachinePanel.this, "连接成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to connect or fetch IPs for host: {}", host, e);
                    nicComboBox.removeAllItems();
                    nicComboBox.addItem("连接失败");
                    nicComboBox.setEnabled(false);
                    
                    // Create a detailed and scrollable error message dialog
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionAsString = sw.toString();

                    JTextArea textArea = new JTextArea(exceptionAsString);
                    textArea.setEditable(false);
                    textArea.setLineWrap(true);
                    textArea.setWrapStyleWord(true);

                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(600, 400)); // Make dialog larger

                    JOptionPane.showMessageDialog(RemoteMachinePanel.this, scrollPane, "连接失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setInputsEnabled(boolean enabled) {
        hostField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        connectButton.setEnabled(enabled);
        // Keep save/delete enabled for profile management
        // saveButton.setEnabled(enabled);
        // deleteButton.setEnabled(enabled);
        // profileComboBox.setEnabled(enabled);
    }
}
