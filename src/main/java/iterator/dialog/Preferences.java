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

import static iterator.util.Config.MAX_PALETTE_SIZE;
import static iterator.util.Config.MIN_PALETTE_SIZE;
import static iterator.util.Config.MIN_THREADS;
import static iterator.util.Config.PALETTE_FILES;
import static iterator.util.Messages.DIALOG_PREFERENCES_BLUR;
import static iterator.util.Messages.DIALOG_PREFERENCES_BUTTON_CANCEL;
import static iterator.util.Messages.DIALOG_PREFERENCES_BUTTON_UPDATE;
import static iterator.util.Messages.DIALOG_PREFERENCES_DEBUG;
import static iterator.util.Messages.DIALOG_PREFERENCES_GAMMA;
import static iterator.util.Messages.DIALOG_PREFERENCES_GRADIENT_COLOUR;
import static iterator.util.Messages.DIALOG_PREFERENCES_ITERATIONS_LIMIT;
import static iterator.util.Messages.DIALOG_PREFERENCES_MODE;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_FILE;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_SEED;
import static iterator.util.Messages.DIALOG_PREFERENCES_PALETTE_SIZE;
import static iterator.util.Messages.DIALOG_PREFERENCES_RENDER;
import static iterator.util.Messages.DIALOG_PREFERENCES_REVERSE;
import static iterator.util.Messages.DIALOG_PREFERENCES_THREADS;
import static iterator.util.Messages.DIALOG_PREFERENCES_TITLE;
import static iterator.util.Messages.DIALOG_PREFERENCES_TRANSFORM;
import static iterator.util.Messages.DIALOG_PREFERENCES_VIBRANCY;

import java.awt.Color;

import com.google.common.base.Optional;
import com.google.common.collect.Range;

import iterator.Explorer;
import iterator.model.functions.CoordinateTransform;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
import iterator.util.Formatter;
import iterator.util.Pair;
import iterator.util.Property;
import iterator.util.Property.OptionalProperty;

/**
 * Preferences dialog.
 */
public class Preferences extends AbstractPropertyDialog<Preferences> {

    private final Property<Mode> mode;
    private final Property<Render> render;
    private final Property<CoordinateTransform.Type> transform;
    private final Property<Boolean> reverse;
    private final Property<String> paletteFile;
    private final Property<Pair<Color>> gradientColour;
    private final Property<Integer> paletteSize, threads, blurKernel;
    private final Property<Long> seed;
    private final OptionalProperty<Long> limit;
    private final Property<Float> gamma, vibrancy;
    private final Property<Boolean> debug;

    private boolean running = false;

    public static Preferences dialog(Explorer controller) {
        return new Preferences(controller);
    }

    private Preferences(Explorer controller) {
        super(controller);

        setLabel(messages.getText(DIALOG_PREFERENCES_TITLE));

        mode = addDropDown(messages.getText(DIALOG_PREFERENCES_MODE), Mode.values());
        render = addDropDown(messages.getText(DIALOG_PREFERENCES_RENDER), Render.values());
        transform = addDropDown(messages.getText(DIALOG_PREFERENCES_TRANSFORM), CoordinateTransform.Type.ordered());
        reverse = addCheckBox(messages.getText(DIALOG_PREFERENCES_REVERSE));
        gradientColour = addGradientPicker(messages.getText(DIALOG_PREFERENCES_GRADIENT_COLOUR));
        paletteFile = addDropDown(messages.getText(DIALOG_PREFERENCES_PALETTE_FILE), PALETTE_FILES);
        paletteSize = addSpinner(messages.getText(DIALOG_PREFERENCES_PALETTE_SIZE), MIN_PALETTE_SIZE, MAX_PALETTE_SIZE);
        seed = addProperty(messages.getText(DIALOG_PREFERENCES_PALETTE_SEED), Formatter.longs());
        gamma = addProperty(messages.getText(DIALOG_PREFERENCES_GAMMA), Formatter.range(Range.openClosed(0f, 4f), Formatter.floats(2)));
        vibrancy = addProperty(messages.getText(DIALOG_PREFERENCES_VIBRANCY), Formatter.range(Range.openClosed(0f, 2f), Formatter.floats(2)));
        blurKernel = addProperty(messages.getText(DIALOG_PREFERENCES_BLUR), Formatter.integers(2, 255));
        limit = addOptionalProperty(messages.getText(DIALOG_PREFERENCES_ITERATIONS_LIMIT), Formatter.optional(Formatter.longs()));
        threads = addSpinner(messages.getText(DIALOG_PREFERENCES_THREADS), MIN_THREADS, Runtime.getRuntime().availableProcessors());
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

        mode.set(config.getMode());
        render.set(config.getRender());
        transform.set(config.getCoordinateTransformType());
        reverse.set(config.isReverseOrder());
        gradientColour.set(Pair.of(config.getGradientStart(), config.getGradientEnd()));
        paletteFile.set(config.getPaletteFile());
        paletteSize.set(config.getPaletteSize());
        seed.set(config.getSeed());
        gamma.set(config.getGamma());
        vibrancy.set(config.getVibrancy());
        blurKernel.set(config.getBlurKernel());
        limit.set(config.isIterationsUnlimited() ? Optional.absent() : Optional.of(config.getIterationsLimit()));
        threads.set(config.getThreads());
        debug.set(config.isDebug());

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        config.setMode(mode.get());
        config.setRender(render.get());
        config.setCoordinateTransformType(transform.get());
        config.setReverseOrder(reverse.get());
        config.setGradientStart(gradientColour.get().getLeft());
        config.setGradientEnd(gradientColour.get().getRight());
        config.setPaletteFile(paletteFile.get());
        config.setPaletteSize(paletteSize.get());
        config.setSeed(seed.get());
        config.setGamma(gamma.get());
        config.setVibrancy(vibrancy.get());
        config.setBlurKernel(blurKernel.get());

        if (limit.isPresent()) {
            config.setIterationsLimit(limit.getNullable());
            config.setIterationsUnimited(false);
        } else {
            config.setIterationsUnimited(true);
        }
        config.setThreads(threads.get());
        config.setDebug(debug.get());

        config.loadColours();

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
