/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Main display window
 */
public class Status extends JLabel implements Callable<Void> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    public static final Logger LOG = LoggerFactory.getLogger(Status.class);

    private final EventBus bus;
    private final Explorer controller;

    private IFS ifs;
    private boolean done = false;

    public Status(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;
        
        setFont(new Font("Calibri", Font.PLAIN, 16));
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
    
    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
        repaint();
    }

    /** @see java.util.concurrent.Callable#call() */
    @Override
    public Void call() throws Exception {
        return null;
    }
}
