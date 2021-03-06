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

import static iterator.util.Messages.DIALOG_ZOOM_BUTTON_CANCEL;
import static iterator.util.Messages.DIALOG_ZOOM_BUTTON_UPDATE;
import static iterator.util.Messages.DIALOG_ZOOM_SCALE;
import static iterator.util.Messages.DIALOG_ZOOM_TITLE;
import static iterator.util.Messages.DIALOG_ZOOM_X;
import static iterator.util.Messages.DIALOG_ZOOM_Y;

import iterator.Explorer;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Formatter;
import iterator.util.Property;

/**
 * Zoom properties dialog.
 */
public class Zoom extends AbstractPropertyDialog<Zoom> {

    private final Property<Double> x, y;
    private final Property<Float> scale;

    private boolean running = false;

    public static Zoom dialog(Explorer controller) {
        return new Zoom(controller);
    }

    private Zoom(Explorer controller) {
        super(controller);

        setLabel(messages.getText(DIALOG_ZOOM_TITLE));

        x = addProperty(messages.getText(DIALOG_ZOOM_X), Formatter.doubles(4));
        y = addProperty(messages.getText(DIALOG_ZOOM_Y), Formatter.doubles(4));
        scale = addProperty(messages.getText(DIALOG_ZOOM_SCALE), Formatter.floats(2));

        setSuccess(messages.getText(DIALOG_ZOOM_BUTTON_UPDATE));
        setCancel(messages.getText(DIALOG_ZOOM_BUTTON_CANCEL));
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        if (controller.getViewer().isRunning()) {
            running = controller.getViewer().stop();
        }

        x.set(config.getDisplayCentreX());
        y.set(config.getDisplayCentreY());
        scale.set(config.getDisplayScale());

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        config.setDisplayCentreX(x.get());
        config.setDisplayCentreY(y.get());
        config.setDisplayScale(scale.get());

        controller.getViewer().rescale();
        controller.getViewer().reset();
        controller.getViewer().start();
    }

    @Override
    public void onCancel() {
        if (running) {
            controller.getViewer().start();
        }
    }

}
