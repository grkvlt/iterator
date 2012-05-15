/*
 * Copyright 2012 by adk; All Rights Reserved
 */
package iterator;

import iterator.model.IFS;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitStrategy;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

/**
 * Apple OSX native support.
 */
public class AppleSupport implements OpenFilesHandler, AboutHandler, PreferencesHandler {
    public static final Logger LOG = LoggerFactory.getLogger(AppleSupport.class);

    private final EventBus bus;
    private final Explorer controller;

    public AppleSupport(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;
    }
    
    public void setup() {
        Application app = Application.getApplication();
        app.setOpenFileHandler(this);
        app.setAboutHandler(this);
        app.setPreferencesHandler(this);
        app.setQuitStrategy(QuitStrategy.SYSTEM_EXIT_0);
        app.setDockIconImage(controller.getIcon());
    }

    /** @see com.apple.eawt.OpenFilesHandler#openFiles(com.apple.eawt.AppEvent.OpenFilesEvent) */
    @Override
    public void openFiles(OpenFilesEvent e) {
        File open = Iterables.getFirst(e.getFiles(), null);
        IFS loaded = controller.load(open);
        loaded.setSize(controller.getSize());
        bus.post(loaded);
    }

    /** @see com.apple.eawt.PreferencesHandler#handlePreferences(com.apple.eawt.AppEvent.PreferencesEvent) */
    @Override
    public void handlePreferences(PreferencesEvent e) {
    }

    /** @see com.apple.eawt.AboutHandler#handleAbout(com.apple.eawt.AppEvent.AboutEvent) */
    @Override
    public void handleAbout(AboutEvent e) {
        controller.getAbout().showDialog();
    }
}
