package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.model.SshProfile;
import com.github.jyzxc.autoiperf.service.EnvironmentService;
import com.github.jyzxc.autoiperf.service.NetworkService;
import com.github.jyzxc.autoiperf.service.NicNameService;
import com.github.jyzxc.autoiperf.service.ProfileService;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RemoteMachinePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(RemoteMachinePanel.class);
    private static final String NEW_CONNECTION_ITEM = "新建连接...";

    // Services
    @Getter
    private final SshService sshService;
    private final ProfileService profileService;
    private final NetworkService networkService;
    private final EnvironmentService environmentService;
    private final NicNameService nicNameService;

    // UI Components
    private JComboBox<Object> profileComboBox;
    @Getter
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton saveButton;
    private JButton deleteButton;
    private JComboBox<String> nicComboBox;

    private boolean isConnected = false;
    private final List<Consumer<Boolean>> connectionStateListeners = new ArrayList<>();


    public RemoteMachinePanel(String title) {
        this.sshService = new SshService();
        this.profileService = new ProfileService();
        this.networkService = new NetworkService(sshService);
        this.environmentService = new EnvironmentService(sshService);
        this.nicNameService = new NicNameService();

        setBorder(new TitledBorder(title));
        setLayout(new GridBagLayout());
        initComponents();
        addListeners();
        loadProfilesIntoComboBox();
    }

    // Getters for controller to access services and state
    public boolean isConnected() { return isConnected; }
    public String getSelectedNicIp() {
        Object selected = nicComboBox.getSelectedItem();
        if (selected == null) {
            return null;
        }
        // Extract IP from display text (which may include custom name)
        return nicNameService.extractIp(selected.toString());
    }
    public NetworkService getNetworkService() { return networkService; }

    public void addConnectionStateListener(Consumer<Boolean> listener) {
        connectionStateListeners.add(listener);
    }

    private void initComponents() {
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
        
        // Add right-click context menu for renaming NICs
        setupNicComboBoxContextMenu();
        
        add(nicComboBox, gbc);
    }
    
    /**
     * Setup right-click context menu for NIC combo box to allow renaming.
     */
    private void setupNicComboBoxContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("重命名网卡");
        renameItem.addActionListener(e -> handleRenameNic());
        popupMenu.add(renameItem);
        
        nicComboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && nicComboBox.isEnabled()) {
                    int index = nicComboBox.getSelectedIndex();
                    if (index >= 0) {
                        Object selected = nicComboBox.getSelectedItem();
                        if (selected != null && !selected.toString().equals("待连接...") 
                            && !selected.toString().equals("未找到可用IP") 
                            && !selected.toString().equals("连接失败")) {
                            popupMenu.show(nicComboBox, e.getX(), e.getY());
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Handle renaming a NIC IP address.
     */
    private void handleRenameNic() {
        Object selected = nicComboBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        
        String displayText = selected.toString();
        String ip = nicNameService.extractIp(displayText);
        if (ip == null) {
            return;
        }
        
        String currentName = nicNameService.getNicName(ip);
        String message = String.format("请输入网卡名称（留空则删除名称）:\nIP地址: %s", ip);
        String initialValue = currentName != null ? currentName : "";
        
        // Use JOptionPane with initial value
        JTextField textField = new JTextField(initialValue);
        Object[] messageArray = {message, textField};
        int option = JOptionPane.showConfirmDialog(
            this,
            messageArray,
            "重命名网卡",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (option == JOptionPane.OK_OPTION) {
            String input = textField.getText();
            // Set the name (empty string removes it)
            nicNameService.setNicName(ip, input.trim());
            
            // Refresh the combo box to show updated name
            refreshNicComboBox();
            
            log.info("Renamed NIC {} to '{}'", ip, input.trim().isEmpty() ? "(removed)" : input.trim());
        }
    }
    
    /**
     * Refresh the NIC combo box with updated names.
     */
    private void refreshNicComboBox() {
        if (!isConnected) {
            return;
        }
        
        Object currentSelection = nicComboBox.getSelectedItem();
        String currentIp = currentSelection != null ? nicNameService.extractIp(currentSelection.toString()) : null;
        
        // Get all IPs from the combo box
        List<String> ips = new ArrayList<>();
        for (int i = 0; i < nicComboBox.getItemCount(); i++) {
            Object item = nicComboBox.getItemAt(i);
            if (item != null) {
                String ip = nicNameService.extractIp(item.toString());
                if (ip != null && !ip.equals("待连接...") && !ip.equals("未找到可用IP") && !ip.equals("连接失败")) {
                    ips.add(ip);
                }
            }
        }
        
        // Rebuild combo box with formatted names
        nicComboBox.removeAllItems();
        for (String ip : ips) {
            nicComboBox.addItem(nicNameService.formatIpWithName(ip));
        }
        
        // Restore selection if possible
        if (currentIp != null) {
            for (int i = 0; i < nicComboBox.getItemCount(); i++) {
                Object item = nicComboBox.getItemAt(i);
                if (item != null && currentIp.equals(nicNameService.extractIp(item.toString()))) {
                    nicComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
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
        
    private void fireConnectionStateChanged() {
        connectionStateListeners.forEach(l -> l.accept(isConnected));
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
                sshService.connect(username, password, host, 22);
                if (!environmentService.checkIperf3Exists(password)) {
                    sshService.disconnect();
                    throw new RuntimeException("iPerf3 not found. Please install it on the remote machine.");
                }
                return networkService.getRemoteIpAddresses();
            }

            @Override
            protected void done() {
                try {
                    List<String> ips = get();
                    log.info("Successfully connected and found IPs: {}", ips);

                    // User story: If the SSH host IP is a VIP, it might not be in the list. Add it.
                    String sshHost = hostField.getText().trim();
                    if (!sshHost.isEmpty() && !ips.contains(sshHost)) {
                        log.warn("SSH host '{}' not found in interface list. Adding it as a potential VIP.", sshHost);
                        ips.add(0, sshHost); // Add to the top for visibility
                    }

                    nicComboBox.removeAllItems();
                    if (ips.isEmpty()) { 
                        nicComboBox.addItem("未找到可用IP"); 
                    } else { 
                        // Add IPs with custom names if available
                        ips.forEach(ip -> nicComboBox.addItem(nicNameService.formatIpWithName(ip)));
                    }
                    nicComboBox.setEnabled(true);
                    setInputsEnabled(false);
                } catch (Exception e) {
                    log.error("Failed to connect or complete setup for host: {}", hostField.getText(), e);
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

    public void setPanelEnabled(boolean enabled) {
        // Enable/disable all input fields except the connect button
        hostField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        profileComboBox.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);

        // Special handling for the connect button's state
        if (isConnected) {
            connectButton.setEnabled(true); // Always allow user to disconnect
            connectButton.setText("断开连接");
        } else {
            connectButton.setEnabled(enabled);
            connectButton.setText("连接");
        }
        
        // NIC combo box is only enabled when connected
        nicComboBox.setEnabled(isConnected);
    }
    
    private void setInputsEnabled(boolean enabled) {
        this.isConnected = !enabled;
        
        if (isConnected) { // Just connected
             // Logic to update UI is now primarily in setPanelEnabled
        } else { // Just disconnected
            nicComboBox.setEnabled(false);
            nicComboBox.removeAllItems();
            nicComboBox.addItem("待连接...");
        }

        setPanelEnabled(enabled); // Update all panel components based on the new state
        fireConnectionStateChanged();
    }

    public void showDetailedErrorDialog(Exception e) {
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
