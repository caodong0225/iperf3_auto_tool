package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.model.SshProfile;
import com.github.jyzxc.autoiperf.service.EnvironmentService;
import com.github.jyzxc.autoiperf.service.NetworkService;
import com.github.jyzxc.autoiperf.service.ProfileService;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class RemoteMachinePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(RemoteMachinePanel.class);
    private static final String NEW_CONNECTION_ITEM = "新建连接...";

    // Services
    private final SshService sshService;
    private final ProfileService profileService;
    private final NetworkService networkService;
    private final EnvironmentService environmentService;

    // UI Components
    private JComboBox<Object> profileComboBox;
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton saveButton;
    private JButton deleteButton;
    private JComboBox<String> nicComboBox;

    private boolean isConnected = false;

    public RemoteMachinePanel(String title) {
        this.sshService = new SshService();
        this.profileService = new ProfileService();
        this.networkService = new NetworkService(sshService);
        this.environmentService = new EnvironmentService(sshService);

        setBorder(new TitledBorder(title));
        setLayout(new GridBagLayout());
        initComponents();
        addListeners();
        loadProfilesIntoComboBox();
    }

    private void initComponents() {
        // ... (same as before)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Saved Profiles
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("历史配置:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; gbc.gridwidth = 2;
        profileComboBox = new JComboBox<>();
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
        connectButton.addActionListener(e -> {
            if (isConnected) {
                handleDisconnect();
            } else {
                handleConnection();
            }
        });

        saveButton.addActionListener(e -> handleSaveProfile());
        deleteButton.addActionListener(e -> handleDeleteProfile());
        profileComboBox.addActionListener(e -> handleProfileSelection());
    }

    private void loadProfilesIntoComboBox() {
        profileComboBox.removeAllItems();
        profileComboBox.addItem(NEW_CONNECTION_ITEM);
        profileService.loadProfiles().forEach(profileComboBox::addItem);
    }

    private void handleProfileSelection() {
        Object selected = profileComboBox.getSelectedItem();
        if (selected instanceof SshProfile) {
            SshProfile profile = (SshProfile) selected;
            hostField.setText(profile.getHost());
            usernameField.setText(profile.getUsername());
            passwordField.setText(profile.getPassword());
        } else {
            // "新建连接..." selected
            hostField.setText("");
            usernameField.setText("root");
            passwordField.setText("");
        }
    }

    private void handleSaveProfile() {
        String profileName = JOptionPane.showInputDialog(this, "请输入配置名称:", "保存配置", JOptionPane.PLAIN_MESSAGE);
        if (profileName != null && !profileName.trim().isEmpty()) {
            SshProfile profile = new SshProfile(profileName.trim(), hostField.getText(), usernameField.getText(), new String(passwordField.getPassword()));
            profileService.saveProfile(profile);
            loadProfilesIntoComboBox();
            // This is tricky, so we just reload. A better implementation would use a custom model.
            JOptionPane.showMessageDialog(this, "配置已保存！");
        }
    }

    private void handleDeleteProfile() {
        Object selected = profileComboBox.getSelectedItem();
        if (selected instanceof SshProfile) {
            SshProfile profile = (SshProfile) selected;
            int choice = JOptionPane.showConfirmDialog(this, "确定要删除配置 '" + profile.getProfileName() + "' 吗?", "删除确认", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                profileService.deleteProfile(profile.getProfileName());
                loadProfilesIntoComboBox();
            }
        } else {
            JOptionPane.showMessageDialog(this, "请先选择一个要删除的配置。", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleConnection() {
        String host = hostField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        log.info("Connect button clicked for host: {}", host);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                log.debug("SwingWorker started for SSH connection.");
                // 1. Connect
                sshService.connect(username, password, host, 22);

                // 2. Check for iperf3
                if (!environmentService.checkIperf3Exists(password)) {
                    sshService.disconnect(); // Clean up connection
                    throw new RuntimeException("iPerf3 not found on the remote host.\nPlease ensure 'iperf3' is installed and in the user's PATH, or that the user has sudo rights.");
                }

                // 3. Get IPs if check is successful
                return networkService.getRemoteIpAddresses();
            }

            @Override
            protected void done() {
                try {
                    List<String> ips = get();
                    log.info("Successfully connected and found IPs: {}", ips);
                    nicComboBox.removeAllItems();
                    if (ips.isEmpty()) { nicComboBox.addItem("未找到可用IP"); } else { ips.forEach(nicComboBox::addItem); }
                    nicComboBox.setEnabled(true);
                    setInputsEnabled(false);
                    // No need for a popup on success, the UI state change is enough feedback.
                } catch (Exception e) {
                    log.error("Failed to connect or complete setup for host: {}", host, e);
                    nicComboBox.removeAllItems(); nicComboBox.addItem("连接失败"); nicComboBox.setEnabled(false);
                    sshService.disconnect(); // Ensure disconnection on failure
                    showDetailedErrorDialog(e);
                }
            }
        }.execute();
    }
    
    private void handleDisconnect() {
        log.info("Disconnecting from host: {}", hostField.getText());
        sshService.disconnect();
        setInputsEnabled(true);
    }

    private void setInputsEnabled(boolean enabled) {
        isConnected = !enabled;
        connectButton.setText(enabled ? "连接" : "断开");
        hostField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        profileComboBox.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        
        if (enabled) { // If disconnecting
            nicComboBox.setEnabled(false);
            nicComboBox.removeAllItems();
            nicComboBox.addItem("待连接...");
        }
    }

    private void showDetailedErrorDialog(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        JTextArea textArea = new JTextArea(exceptionAsString);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "操作失败", JOptionPane.ERROR_MESSAGE);
    }
}