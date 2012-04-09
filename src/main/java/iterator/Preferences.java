/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import iterator.model.IFS;

import javax.swing.JDialog;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * IFS Editor.
 */
public class Preferences extends JDialog {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private final EventBus bus;
    private final Explorer controller;

    private IFS ifs;

    public Preferences(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;

        bus.register(this);
    }
    
    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
    }
}
