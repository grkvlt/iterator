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

import static iterator.util.Messages.DIALOG_PREFERENCES_BLUR;
import static iterator.util.Messages.DIALOG_PREFERENCES_BUTTON_CANCEL;
import static iterator.util.Messages.DIALOG_PREFERENCES_BUTTON_UPDATE;
import static iterator.util.Messages.DIALOG_PREFERENCES_DEBUG;
import static iterator.util.Messages.DIALOG_PREFERENCES_GAMMA;
import static iterator.util.Messages.DIALOG_PREFERENCES_ITERATIONS_LIMIT;
import static iterator.util.Messages.DIALOG_PREFERENCES_MODE;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_FILE;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_SEED;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_SIZE;
import static iterator.util.Messages.DIALOG_PREFERENCES_RENDER;
import static iterator.util.Messages.DIALOG_PREFERENCES_THREADS;
import static iterator.util.Messages.DIALOG_PREFERENCES_TITLE;
import static iterator.util.Messages.DIALOG_PREFERENCES_TRANSFORM;
import static iterator.util.Messages.DIALOG_PREFERENCES_VIBRANCY;

import com.google.common.base.Optional;
import com.google.common.collect.Range;

import iterator.Explorer;
import iterator.model.functions.CoordinateTransform;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Config;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
import iterator.util.Formatter;
import iterator.util.Property;
import iterator.util.Property.OptionalProperty;

/**
 * Preferences dialog.
 */
public class Preferences extends AbstractPropertyDialog {

    private final Property<Mode> mode;
    private final Property<Render> render;
    private final Property<CoordinateTransform.Type> transform;
    private final Property<String> paletteFile;
    private final Property<Integer> paletteSize, threads, blurKernel;
    private final Property<Long> seed;
    private final OptionalProperty<Long> limit;
    private final Property<Float> gamma, vibrancy;
    private final Property<Boolean> debug;

    private boolean running = false;

    public Preferences(Explorer controller) {
        super(controller);

        setLabel(messages.getText(DIALOG_PREFERENCES_TITLE));

        mode = addDropDown(messages.getText(DIALOG_PREFERENCES_MODE), Mode.values());
        render = addDropDown(messages.getText(DIALOG_PREFERENCES_RENDER), Render.values());
        transform = addDropDown(messages.getText(DIALOG_PREFERENCES_TRANSFORM), CoordinateTransform.Type.values());
        paletteFile = addDropDown(messages.getText(DIALOG_PREFERENCES_PALETTE_FILE), Config.PALETTE_FILES);
        paletteSize = addSpinner(messages.getText(DIALOG_PREFERENCES_PALETTE_SIZE), Config.MIN_PALETTE_SIZE, Config.MAX_PALETTE_SIZE);
        seed = addProperty(messages.getText(DIALOG_PREFERENCES_PALETTE_SEED), Formatter.longs());
        gamma = addProperty(messages.getText(DIALOG_PREFERENCES_GAMMA), Formatter.range(Range.open(0f, 5f), Formatter.floats(1)));
        vibrancy = addProperty(messages.getText(DIALOG_PREFERENCES_VIBRANCY), Formatter.range(Range.open(0f, 1f), Formatter.floats(1)));
        blurKernel = addProperty(messages.getText(DIALOG_PREFERENCES_BLUR), Formatter.integers(1, 256));
        limit = addOptionalProperty(messages.getText(DIALOG_PREFERENCES_ITERATIONS_LIMIT), Formatter.optional(Formatter.longs()));
        threads = addSpinner(messages.getText(DIALOG_PREFERENCES_THREADS), Config.MIN_THREADS, Runtime.getRuntime().availableProcessors());
        debug = addCheckBox(messages.getText(DIALOG_PREFERENCES_DEBUG));

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
        transform.set(controller.getCoordinateTransformType());
        paletteFile.set(controller.getPaletteFile());
        paletteSize.set(controller.getPaletteSize());
        seed.set(controller.getSeed());
        gamma.set(controller.getGamma());
        vibrancy.set(controller.getVibrancy());
        blurKernel.set(controller.getBlurKernel());
        limit.set(controller.isIterationsUnlimited() ? Optional.absent() : Optional.of(controller.getIterationsLimit()));
        threads.set(controller.getThreads());
        debug.set(controller.isDebug());

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        controller.setMode(mode.get());
        controller.setRender(render.get());
        controller.setCoordinateTransformType(transform.get());
        controller.setPaletteFile(paletteFile.get());
        controller.setPaletteSize(paletteSize.get());
        controller.setSeed(seed.get());
        controller.setGamma(gamma.get());
        controller.setVibrancy(vibrancy.get());
        controller.setBlurKernel(blurKernel.get());

        if (limit.isPresent()) {
            controller.setIterationsLimit(limit.get().get());
            controller.setIterationsUnimited(false);
        } else {
            controller.setIterationsUnimited(true);
        }
        controller.setThreads(threads.get());
        controller.setDebug(debug.get());
        controller.loadColours();

        controller.getViewer().reset();
        if (controller.getCurrent().equals(Explorer.VIEWER)) {
            controller.getViewer().start();
        }

        controller.getDetails().setDetails();
        controller.getCurrentComponent().repaint();
    }

    @Override
    public void onCancel() {
        if (controller.getCurrent().equals(Explorer.VIEWER) && running) {
            controller.getViewer().start();
        }
    }
}
