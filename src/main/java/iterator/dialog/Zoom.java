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

import java.awt.geom.Point2D;

import javax.swing.JFormattedTextField.AbstractFormatter;

import iterator.Explorer;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Property;
import iterator.util.Utils;

/**
 * Zoom properties dialog.
 */
public class Zoom extends AbstractPropertyDialog {

    private final Property<Double> x, y;
    private final Property<Float> scale;

    private boolean running = false;

    public Zoom(final Explorer controller) {
        super(controller);

        setLabel(messages.getText(DIALOG_ZOOM_TITLE));

        AbstractFormatter doubleFormatter = new Utils.DoubleFormatter();
        x = addProperty(messages.getText(DIALOG_ZOOM_X), 0d, doubleFormatter);
        y = addProperty(messages.getText(DIALOG_ZOOM_Y), 0d, doubleFormatter);
        AbstractFormatter floatFormatter = new Utils.FloatFormatter();
        scale = addProperty(messages.getText(DIALOG_ZOOM_SCALE), 1f, floatFormatter);

        setSuccess(messages.getText(DIALOG_ZOOM_BUTTON_UPDATE));
        setCancel(messages.getText(DIALOG_ZOOM_BUTTON_CANCEL));
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        if (controller.getViewer().isRunning()) {
            running = controller.getViewer().stop();
        }

        x.set(controller.getViewer().getCentre().getX() / controller.getWidth());
        y.set(controller.getViewer().getCentre().getY() / controller.getHeight());
        scale.set(controller.getViewer().getScale());

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        controller.getViewer().rescale(scale.get(), new Point2D.Double(x.get() * controller.getWidth(), y.get() * controller.getHeight()));
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
