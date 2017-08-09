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
package iterator.dialog;

import static iterator.util.Messages.DIALOG_PREFERENCES_BUTTON_CANCEL;
import static iterator.util.Messages.DIALOG_PREFERENCES_BUTTON_UPDATE;
import static iterator.util.Messages.DIALOG_PREFERENCES_DEBUG;
import static iterator.util.Messages.DIALOG_PREFERENCES_MODE;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_FILE;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_SEED;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_SIZE;
import static iterator.util.Messages.DIALOG_PREFERENCES_RENDER;
import static iterator.util.Messages.DIALOG_PREFERENCES_THREADS;
import static iterator.util.Messages.DIALOG_PREFERENCES_TITLE;

import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Config;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
import iterator.util.Utils;

/**
 * Preferences dialog.
 */
public class Preferences extends AbstractPropertyDialog {

    private final Supplier<Mode> mode;
    private final Supplier<Render> render;
    private final Supplier<String> paletteFile;
    private final Supplier<Integer> paletteSize, threads;
    private final Supplier<Long> seed;
    private final Supplier<Boolean> debug;

    public Preferences(final Explorer controller, final EventBus bus, final Window parent) {
        super(controller, bus, parent);

        setLabel(messages.getText(DIALOG_PREFERENCES_TITLE));

        mode = addDropDown(messages.getText(DIALOG_PREFERENCES_MODE), controller.getMode(), Mode.values());
        render = addDropDown(messages.getText(DIALOG_PREFERENCES_RENDER), controller.getRender(), Render.values());
        paletteFile = addDropDown(messages.getText(DIALOG_PREFERENCES_PALETTE_FILE), controller.getPaletteFile(), Config.PALETTE_FILES);
        paletteSize = addProperty(messages.getText(DIALOG_PREFERENCES_PALETTE_SIZE), controller.getPaletteSize(), new Utils.IntegerFormatter(0, 256));
        seed = addProperty(messages.getText(DIALOG_PREFERENCES_PALETTE_SEED), controller.getSeed(), new Utils.LongFormatter());
        threads = addProperty(messages.getText(DIALOG_PREFERENCES_THREADS), controller.getThreads(), new Utils.IntegerFormatter(Config.MIN_THREADS, 8));
        debug = addCheckBox(messages.getText(DIALOG_PREFERENCES_DEBUG), controller.isDebug());

        setAction(new AbstractAction(messages.getText(DIALOG_PREFERENCES_BUTTON_UPDATE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.setMode(mode.get().name());
                controller.setRender(render.get().name());
                controller.setPaletteFile(paletteFile.get());
                controller.setPaletteSize(paletteSize.get());
                controller.setSeed(seed.get());
                controller.setThreads(threads.get());
                controller.setDebug(debug.get());
                controller.loadColours();
                controller.getViewer().reset();
                setVisible(false);
            }
        });
        setCancel(messages.getText(DIALOG_PREFERENCES_BUTTON_CANCEL));
    }
}
