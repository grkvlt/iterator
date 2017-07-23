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
import static iterator.util.Messages.DIALOG_PROPERTIES_TITLE;
import static iterator.util.Messages.DIALOG_PROPERTIES_W;
import static iterator.util.Messages.DIALOG_PROPERTIES_X;
import static iterator.util.Messages.DIALOG_PROPERTIES_Y;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Messages;
import iterator.util.Utils;

/**
 * Properties dialog.
 */
public class Properties extends JDialog {
    /** serialVersionUID */
    private static final long serialVersionUID = -7626627964747215623L;

    private final Supplier<Double> x, y, w, h, r;
    private final Messages messages;

    public Properties(final Transform transform, final IFS ifs, final Explorer controller, final EventBus bus, final Window parent) {
        super(parent, null, ModalityType.APPLICATION_MODAL);

        messages = controller.getMessages();

        setUndecorated(true);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 2, 5);

        setFont(new Font("Calibri", Font.PLAIN, 14));
        getContentPane().setBackground(Color.WHITE);
        setLayout(gridbag);

        String label = MessageFormat.format(messages.getText(DIALOG_PROPERTIES_TITLE), transform.getId());
        JLabel title = new JLabel(label, JLabel.CENTER);
        title.setFont(new Font("Calibri", Font.BOLD, 16));
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1.0;
        gridbag.setConstraints(title, c);
        add(title);

        x = addProperty(messages.getText(DIALOG_PROPERTIES_X), transform.x, gridbag, c);
        y = addProperty(messages.getText(DIALOG_PROPERTIES_Y), transform.y, gridbag, c);
        w = addProperty(messages.getText(DIALOG_PROPERTIES_W), transform.w, gridbag, c);
        h = addProperty(messages.getText(DIALOG_PROPERTIES_H), transform.h, gridbag, c);
        r = addProperty(messages.getText(DIALOG_PROPERTIES_R), Math.toDegrees(transform.r), gridbag, c);

        @SuppressWarnings("serial")
        JButton update = new JButton(new AbstractAction(messages.getText(DIALOG_PROPERTIES_BUTTON_UPDATE)) {
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
        c.gridwidth = 1;
        c.weighty = 1.0;
        gridbag.setConstraints(update, c);
        add(update);

        @SuppressWarnings("serial")
        JButton cancel = new JButton(new AbstractAction(messages.getText(DIALOG_PROPERTIES_BUTTON_CANCEL)) {
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
        c.weighty = 2.0;
        gridbag.setConstraints(label, c);
        add(label);

        final JFormattedTextField field = new JFormattedTextField(new Utils.DoubleFormatter());
        field.setHorizontalAlignment(JTextField.LEFT);
        field.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        field.setMargin(new Insets(2, 2, 2, 2));
        field.setValue(value);
        field.setFont(new Font("Cambria", Font.ITALIC, 14));

        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 2.0;
        gridbag.setConstraints(field, c);
        add(field);

        Supplier<T> supplier = new Supplier<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                return (T) field.getValue();
            }
        };
        return supplier;
    }

    public void showDialog() {
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

}
