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

import iterator.Explorer;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Config;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
import iterator.util.Formatter;
import iterator.util.Property;

/**
 * Preferences dialog.
 */
public class Preferences extends AbstractPropertyDialog {

    private final Property<Mode> mode;
    private final Property<Render> render;
    private final Property<String> paletteFile;
    private final Property<Integer> paletteSize, threads;
    private final Property<Long> seed;
    private final Property<Boolean> debug;

    private boolean running = false;

    public Preferences(final Explorer controller) {
        super(controller);

        setLabel(messages.getText(DIALOG_PREFERENCES_TITLE));

        mode = addDropDown(messages.getText(DIALOG_PREFERENCES_MODE), controller.getMode(), Mode.values());
        render = addDropDown(messages.getText(DIALOG_PREFERENCES_RENDER), controller.getRender(), Render.values());
        paletteFile = addDropDown(messages.getText(DIALOG_PREFERENCES_PALETTE_FILE), controller.getPaletteFile(), Config.PALETTE_FILES);
        paletteSize = addSpinner(messages.getText(DIALOG_PREFERENCES_PALETTE_SIZE), controller.getPaletteSize(), 0, 256);
        seed = addProperty(messages.getText(DIALOG_PREFERENCES_PALETTE_SEED), controller.getSeed(), Formatter.longs());
        threads = addSpinner(messages.getText(DIALOG_PREFERENCES_THREADS), controller.getThreads(), Config.MIN_THREADS, 8);
        debug = addCheckBox(messages.getText(DIALOG_PREFERENCES_DEBUG), controller.isDebug());

        setSuccess(messages.getText(DIALOG_PREFERENCES_BUTTON_UPDATE));
        setCancel(messages.getText(DIALOG_PREFERENCES_BUTTON_CANCEL));
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        if (controller.getCurrent().equals(Explorer.VIEWER) && controller.getViewer().isRunning()) {
            running = controller.getViewer().stop();
        }

        mode.set(controller.getMode());
        render.set(controller.getRender());
        paletteFile.set(controller.getPaletteFile());
        paletteSize.set(controller.getPaletteSize());
        seed.set(controller.getSeed());
        threads.set(controller.getThreads());
        debug.set(controller.isDebug());

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        controller.setMode(mode.get().name());
        controller.setRender(render.get().name());
        controller.setPaletteFile(paletteFile.get());
        controller.setPaletteSize(paletteSize.get());
        controller.setSeed(seed.get());
        controller.setThreads(threads.get());
        controller.setDebug(debug.get());
        controller.loadColours();

        controller.getViewer().reset();
        if (controller.getCurrent().equals(Explorer.VIEWER)) {
            controller.getViewer().start();
        }
    }

    @Override
    public void onCancel() {
        if (controller.getCurrent().equals(Explorer.VIEWER) && running) {
            controller.getViewer().start();
        }
    }
}
