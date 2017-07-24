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
package iterator.view.dialog;

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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JFormattedTextField.AbstractFormatter;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Utils;

/**
 * Properties dialog.
 */
public class Properties extends AbstractPropertyDialog {

    private final Supplier<Double> x, y, w, h, r, shx, shy;

    public Properties(final Transform transform, final IFS ifs, final Explorer controller, final EventBus bus, final Window parent) {
        super(controller, bus, parent);

        String label = MessageFormat.format(messages.getText(DIALOG_PROPERTIES_TITLE), transform.getId());
        setLabel(label);

        AbstractFormatter doubleFormatter = new Utils.DoubleFormatter();
        x = addProperty(messages.getText(DIALOG_PROPERTIES_X), transform.x, gridbag, c, doubleFormatter);
        y = addProperty(messages.getText(DIALOG_PROPERTIES_Y), transform.y, gridbag, c, doubleFormatter);
        w = addProperty(messages.getText(DIALOG_PROPERTIES_W), transform.w, gridbag, c, doubleFormatter);
        h = addProperty(messages.getText(DIALOG_PROPERTIES_H), transform.h, gridbag, c, doubleFormatter);
        r = addProperty(messages.getText(DIALOG_PROPERTIES_R), Math.toDegrees(transform.r), gridbag, c, doubleFormatter);
        shx = addProperty(messages.getText(DIALOG_PROPERTIES_SHX), transform.shx, gridbag, c, doubleFormatter);
        shy = addProperty(messages.getText(DIALOG_PROPERTIES_SHY), transform.shy, gridbag, c, doubleFormatter);

        setAction(new AbstractAction(messages.getText(DIALOG_PROPERTIES_BUTTON_UPDATE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                transform.x = x.get();
                transform.y = y.get();
                transform.w = w.get();
                transform.h = h.get();
                transform.r = Math.toRadians(r.get());
                transform.shx = shx.get();
                transform.shy = shy.get();
                bus.post(ifs);
                setVisible(false);
            }
        });
        setCancel(messages.getText(DIALOG_PROPERTIES_BUTTON_CANCEL));
    }

}
