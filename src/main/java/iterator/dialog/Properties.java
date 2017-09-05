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
import static iterator.util.Messages.DIALOG_PROPERTIES_DETERMINANT;
import static iterator.util.Messages.DIALOG_PROPERTIES_H;
import static iterator.util.Messages.DIALOG_PROPERTIES_R;
import static iterator.util.Messages.DIALOG_PROPERTIES_SHX;
import static iterator.util.Messages.DIALOG_PROPERTIES_SHY;
import static iterator.util.Messages.DIALOG_PROPERTIES_TITLE;
import static iterator.util.Messages.DIALOG_PROPERTIES_W;
import static iterator.util.Messages.DIALOG_PROPERTIES_WEIGHT;
import static iterator.util.Messages.DIALOG_PROPERTIES_X;
import static iterator.util.Messages.DIALOG_PROPERTIES_Y;

import java.text.MessageFormat;

import com.google.common.base.Optional;

import iterator.Explorer;
import iterator.model.Function;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.AbstractPropertyDialog;
import iterator.util.Formatter;
import iterator.util.Property;
import iterator.util.Property.OptionalProperty;

/**
 * Properties dialog.
 */
public class Properties extends AbstractPropertyDialog {

    private final Transform transform;
    private final Reflection reflection;
    private final IFS ifs;

    private Property<Double> x, y, w, h, r, shx, shy, det;
    private OptionalProperty<Double> weight;

    public Properties(Function function, IFS ifs, Explorer controller) {
        super(controller);

        if (function instanceof Transform) {
            this.transform = (Transform) function;
            this.reflection = null;
        } else {
            this.transform =  null;
            this.reflection = (Reflection) function;
        }
        this.ifs = ifs;

        String label = MessageFormat.format(messages.getText(DIALOG_PROPERTIES_TITLE), function.getClass().getSimpleName());
        setLabel(label);

        if (transform != null) {
            x = addProperty(messages.getText(DIALOG_PROPERTIES_X), Formatter.doubles(1));
            y = addProperty(messages.getText(DIALOG_PROPERTIES_Y), Formatter.doubles(1));
            r = addProperty(messages.getText(DIALOG_PROPERTIES_R), Formatter.doubles(1));
            w = addProperty(messages.getText(DIALOG_PROPERTIES_W), Formatter.doubles(1));
            h = addProperty(messages.getText(DIALOG_PROPERTIES_H), Formatter.doubles(1));
            shx = addProperty(messages.getText(DIALOG_PROPERTIES_SHX), Formatter.doubles(4));
            shy = addProperty(messages.getText(DIALOG_PROPERTIES_SHY), Formatter.doubles(4));
            det = addReadOnlyProperty(messages.getText(DIALOG_PROPERTIES_DETERMINANT), Formatter.doubles(4));
            weight = addOptionalProperty(messages.getText(DIALOG_PROPERTIES_WEIGHT), Formatter.optionalDoubles(4));
        }
        if (reflection != null) {
            x = addProperty(messages.getText(DIALOG_PROPERTIES_X), Formatter.doubles(1));
            y = addProperty(messages.getText(DIALOG_PROPERTIES_Y), Formatter.doubles(1));
            r = addProperty(messages.getText(DIALOG_PROPERTIES_R), Formatter.doubles(1));
        }

        setSuccess(messages.getText(DIALOG_PROPERTIES_BUTTON_UPDATE));
        setCancel(messages.getText(DIALOG_PROPERTIES_BUTTON_CANCEL));
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        if (transform != null) {
            x.set(transform.x);
            y.set(transform.y);
            r.set(Math.toDegrees(transform.r));
            w.set(transform.w);
            h.set(transform.h);
            shx.set(transform.shx);
            shy.set(transform.shy);
            det.set(transform.getDeterminant());
            weight.set(Optional.fromNullable(transform.weight));
        }
        if (reflection != null) {
            x.set(reflection.x);
            y.set(reflection.y);
            r.set(Math.toDegrees(reflection.r));
        }

        super.showDialog();
    }

    @Override
    public void onSuccess() {
        if (transform != null) {
            transform.x = x.get();
            transform.y = y.get();
            transform.r = Math.toRadians(r.get());
            transform.w = w.get();
            transform.h = h.get();
            transform.shx = shx.get();
            transform.shy = shy.get();
            transform.weight = weight.getNullable();
        }
        if (reflection != null) {
            reflection.x = x.get();
            reflection.y = y.get();
            reflection.r = Math.toRadians(r.get());
        }

        bus.post(ifs);
    }

}
