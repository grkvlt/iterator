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
import com.apple.eawt.event.GestureUtilities;
import com.apple.eawt.event.MagnificationEvent;
import com.apple.eawt.event.MagnificationListener;
import com.apple.eawt.event.RotationEvent;
import com.apple.eawt.event.RotationListener;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;

import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Dialog;
import iterator.util.Output;

/**
 * Apple OSX native support.
 */
public class AppleSupport implements OpenFilesHandler, AboutHandler, PreferencesHandler, QuitHandler {

    private final Explorer controller;
    private final EventBus bus;
    private final Output out;

    public AppleSupport(Explorer controller) {
        super();
        this.controller = controller;
        this.bus = controller.getEventBus();
        this.out = controller.getOutput();
    }

    public void setup() {
        Application app = Application.getApplication();
        app.setOpenFileHandler(this);
        app.setAboutHandler(this);
        app.setPreferencesHandler(this);
        app.setQuitHandler(this);
        app.setDockIconImage(controller.getIcon());

        GestureUtilities.addGestureListenerTo(controller.getEditor(), new RotationListener() {
            @Override
            public void rotate(RotationEvent re) {
                if (controller.getEditor().isVisible()) {
                    Transform selected = controller.getEditor().getSelected();
                    if (selected != null && !selected.isMatrix()) {
                        selected.r -= Math.toRadians(re.getRotation());
                    }
                }
            }
        });
        GestureUtilities.addGestureListenerTo(controller.getEditor(), new MagnificationListener() {
            @Override
            public void magnify(MagnificationEvent me) {
                if (controller.getEditor().isVisible()) {
                    Transform selected = controller.getEditor().getSelected();
                    if (selected != null && !selected.isMatrix()) {
                        selected.w *= 1d + me.getMagnification();
                        selected.h *= 1d + me.getMagnification();
                    }
                }
            }
        });
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
        Dialog.show(controller::getPreferences, controller);
    }

    /** @see com.apple.eawt.QuitHandler#handleQuitRequestWith(com.apple.eawt.AppEvent.QuitEvent, com.apple.eawt.QuitResponse) */
    @Override
    public void handleQuitRequestWith(QuitEvent e, QuitResponse r) {
        out.print("Exiting");
        r.performQuit();
        System.exit(0);
    }

    /** @see com.apple.eawt.AboutHandler#handleAbout(com.apple.eawt.AppEvent.AboutEvent) */
    @Override
    public void handleAbout(AboutEvent e) {
        Dialog.show(controller::getAbout, controller);
    }
}
