/*
 * Copyright 2012-2017 by Andrew Kennedy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iterator;

import iterator.model.IFS;

import java.io.File;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

/**
 * Apple OSX native support.
 */
public class AppleSupport implements OpenFilesHandler, AboutHandler, PreferencesHandler, QuitHandler {
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
        app.setQuitHandler(this);
        app.setDockIconImage(controller.getIcon());
    }

    /** @see com.apple.eawt.OpenFilesHandler#openFiles(com.apple.eawt.AppEvent.OpenFilesEvent) */
    @Override
    public void openFiles(OpenFilesEvent e) {
        File open = Iterables.<File>getFirst(e.getFiles(), null);
        IFS loaded = controller.load(open);
        loaded.setSize(controller.getSize());
        bus.post(loaded);
    }

    /** @see com.apple.eawt.PreferencesHandler#handlePreferences(com.apple.eawt.AppEvent.PreferencesEvent) */
    @Override
    public void handlePreferences(PreferencesEvent e) {
    }

    /** @see com.apple.eawt.QuitHandler#handleQuitRequestWith(com.apple.eawt.AppEvent.QuitEvent, com.apple.eawt.QuitResponse) */
    @Override
    public void handleQuitRequestWith(QuitEvent e, QuitResponse r) {
        r.performQuit();
        System.exit(0);
    }

    /** @see com.apple.eawt.AboutHandler#handleAbout(com.apple.eawt.AppEvent.AboutEvent) */
    @Override
    public void handleAbout(AboutEvent e) {
        controller.getAbout().showDialog();
    }
}
