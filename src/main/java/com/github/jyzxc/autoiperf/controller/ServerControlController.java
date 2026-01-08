package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.service.ServerManagerService;
import com.github.jyzxc.autoiperf.ui.ServerControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the ServerControlPanel.
 * Manages the logic for starting, stopping, and monitoring iperf3 server instances.
 */
public class ServerControlController {

    private static final Logger log = LoggerFactory.getLogger(ServerControlController.class);

    private final ServerControlPanel view;
    private final ServerManagerService service;

    public ServerControlController(ServerControlPanel view, ServerManagerService service) {
        this.view = view;
        this.service = service;
        addListeners();
    }

    private void addListeners() {
        view.getStartServerButton().addActionListener(e -> handleStartServer());
    }

    private void handleStartServer() {
        log.info("'Start New Server' button clicked. Logic to be implemented.");
        // In the next step, we will get data from the view,
        // call the service to start the iperf3 server,
        // and update the JTable with the result.
    }
}
