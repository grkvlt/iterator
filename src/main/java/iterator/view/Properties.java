/*
 * Copyright 2012-2013 by Andrew Kennedy.
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
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

/**
 * Properties dialog.
 *
 * TODO implement matrix editor
 */
public class Properties extends JDialog {
    /** serialVersionUID */
    private static final long serialVersionUID = -7626627964747215623L;

    private final Supplier<Double> x, y, w, h, r;

    public Properties(final Transform transform, final IFS ifs, final EventBus bus, final Explorer controller) {
        super(controller, "Transform Properties", ModalityType.APPLICATION_MODAL);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        setFont(new Font("Calibri", Font.PLAIN, 14));
        setLayout(gridbag);

        JLabel title = new JLabel(String.format("Transform T%02d Properties", transform.getId()), JLabel.CENTER);
        title.setFont(new Font("Calibri", Font.BOLD, 16));
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(title, c);
        add(title);

        x = addProperty("X", transform.x, gridbag, c);
        y = addProperty("Y", transform.y, gridbag, c);
        w = addProperty("Width", transform.w, gridbag, c);
        h = addProperty("Height", transform.h, gridbag, c);
        r = addProperty("Angle", Math.toDegrees(transform.r), gridbag, c);

        @SuppressWarnings("serial")
        JButton update = new JButton(new AbstractAction("Update") {
            @Override
            public void actionPerformed(ActionEvent e) {
                transform.x = x.get();
                transform.y = y.get();
                transform.w = w.get();
                transform.h = h.get();
                transform.r = Math.toRadians(r.get());
                bus.post(ifs);
                setVisible(false);
            }
        });
        update.setFont(new Font("Calibri", Font.PLAIN, 14));
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridwidth = 1; // GridBagConstraints.RELATIVE;
        gridbag.setConstraints(update, c);
        add(update);

        @SuppressWarnings("serial")
        JButton cancel = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        cancel.setFont(new Font("Calibri", Font.PLAIN, 14));
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancel, c);
        add(cancel);

        pack();

        Dimension size = new Dimension(200, 200);
        setMinimumSize(size);
        setResizable(false);
    }

    private <T> Supplier<T> addProperty(String string, T value, GridBagLayout gridbag, GridBagConstraints c) {
        JLabel label = new JLabel(string, JLabel.RIGHT);
        label.setFont(new Font("Calibri", Font.PLAIN, 14));
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 2.0;
        gridbag.setConstraints(label, c);
        add(label);

        final JFormattedTextField field = new JFormattedTextField(value);
        field.setFont(new Font("Cambria", Font.ITALIC, 14));
        Supplier<T> supplier = new Supplier<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                return (T) field.getValue();
            }
        };
        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        gridbag.setConstraints(field, c);
        add(field);

        return supplier;
    }

    public void showDialog() {
        setLocationByPlatform(true);
        setVisible(true);
    }

}
