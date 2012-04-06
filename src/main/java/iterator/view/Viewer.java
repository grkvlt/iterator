/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;

import java.awt.Graphics;
import java.util.concurrent.Callable;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Main display window
 */
public class Viewer extends JPanel implements Callable<Void> {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    public static final Logger LOG = LoggerFactory.getLogger(Viewer.class);

    private final EventBus bus;
    private final Explorer controller;

    private IFS ifs;
    private boolean done = false;

    public Viewer(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;

        bus.register(this);
    }
    
    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
    }

    /** @see java.util.concurrent.Callable#call() */
    @Override
    public Void call() throws Exception {
        return null;
    }

    public void render() {
        while (!done) {
            Graphics myGraphics = getGraphics();
            // TODO draw
            myGraphics.dispose();
        }
    }
}
