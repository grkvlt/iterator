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

import static iterator.util.Messages.DIALOG_PROPERTIES_BUTTON_CANCEL;
import static iterator.util.Messages.DIALOG_PROPERTIES_BUTTON_UPDATE;
import static iterator.util.Messages.DIALOG_PROPERTIES_H;
import static iterator.util.Messages.DIALOG_PROPERTIES_R;
import static iterator.util.Messages.DIALOG_PROPERTIES_SHX;
import static iterator.util.Messages.DIALOG_PROPERTIES_SHY;
import static iterator.util.Messages.DIALOG_PROPERTIES_TITLE;
import static iterator.util.Messages.DIALOG_PROPERTIES_W;
import static iterator.util.Messages.DIALOG_PROPERTIES_X;
import static iterator.util.Messages.DIALOG_PROPERTIES_Y;
import static iterator.util.Messages.DIALOG_PROPERTIES_WEIGHT;

import java.awt.Window;
import java.text.MessageFormat;

import javax.swing.JFormattedTextField.AbstractFormatter;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Property;
import iterator.util.Utils;

/**
 * Properties dialog.
 */
public class Properties extends AbstractPropertyDialog {

    private final Transform transform;
    private final IFS ifs;
    private final Property<Double> x, y, w, h, r, shx, shy;
    private final Property<Optional<Double>> weight;

    public Properties(final Transform transform, final IFS ifs, final Explorer controller, final EventBus bus, final Window parent) {
        super(controller, bus, parent);

        this.transform = transform;
        this.ifs = ifs;

        String label = MessageFormat.format(messages.getText(DIALOG_PROPERTIES_TITLE), transform.getId());
        setLabel(label);

        AbstractFormatter doubleFormatter = new Utils.DoubleFormatter();
        x = addProperty(messages.getText(DIALOG_PROPERTIES_X), transform.x, doubleFormatter);
        y = addProperty(messages.getText(DIALOG_PROPERTIES_Y), transform.y, doubleFormatter);
        w = addProperty(messages.getText(DIALOG_PROPERTIES_W), transform.w, doubleFormatter);
        h = addProperty(messages.getText(DIALOG_PROPERTIES_H), transform.h, doubleFormatter);
        r = addProperty(messages.getText(DIALOG_PROPERTIES_R), Math.toDegrees(transform.r), doubleFormatter);
        shx = addProperty(messages.getText(DIALOG_PROPERTIES_SHX), transform.shx, doubleFormatter);
        shy = addProperty(messages.getText(DIALOG_PROPERTIES_SHY), transform.shy, doubleFormatter);
        AbstractFormatter optionalDoubleFormatter = new Utils.OptionalDoubleFormatter();
        weight = addProperty(messages.getText(DIALOG_PROPERTIES_WEIGHT), Optional.fromNullable(transform.weight), optionalDoubleFormatter);

        setSuccess(messages.getText(DIALOG_PROPERTIES_BUTTON_UPDATE));
        setCancel(messages.getText(DIALOG_PROPERTIES_BUTTON_CANCEL));
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        x.set(transform.x);
        y.set(transform.y);
        w.set(transform.w);
        h.set(transform.h);
        r.set(Math.toDegrees(transform.r));
        shx.set(transform.shx);
        shy.set(transform.shy);
        weight.set(Optional.fromNullable(transform.weight));

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        transform.x = x.get();
        transform.y = y.get();
        transform.w = w.get();
        transform.h = h.get();
        transform.r = Math.toRadians(r.get());
        transform.shx = shx.get();
        transform.shy = shy.get();
        if (weight.get().isPresent()) {
            transform.weight = weight.get().get();
        }
        bus.post(ifs);
    }

}
