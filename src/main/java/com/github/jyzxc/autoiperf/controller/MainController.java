package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main controller, responsible for orchestrating UI and services.
 * NOTE: This class is being heavily refactored. The old logic has been
 * temporarily removed to allow the application to compile during the UI transition.
 * New, more specialized controllers will be created.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    public MainController(MainFrame mainFrame) {
        log.info("MainController initialized in a temporary state during UI refactoring.");
        // All old logic is removed to prevent compilation errors.
    }
}