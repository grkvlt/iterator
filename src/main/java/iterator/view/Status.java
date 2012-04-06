/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.view;

import iterator.Explorer;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Status bar.
 */
public class Status extends JLabel {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    public static final Logger LOG = LoggerFactory.getLogger(Status.class);

    private final EventBus bus;
    private final Explorer controller;

    public Status(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;
        
        setFont(new Font("Calibri", Font.PLAIN, 10));
        setForeground(Color.BLACK);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        status("");

        bus.register(this);
    }
    
    @Subscribe
    public void status(String text) {
        setText(Strings.isNullOrEmpty(text) ? "-" : text);
        repaint();
    }
}
