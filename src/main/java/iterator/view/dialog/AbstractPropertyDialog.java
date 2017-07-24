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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.util.Messages;

/**
 * Abstract dialog for setting properties.
 */
public abstract class AbstractPropertyDialog extends JDialog {
    /** serialVersionUID */
    private static final long serialVersionUID = -7626627964747215623L;

    protected final Messages messages;
    protected final Explorer controller;
    protected final EventBus bus;
    protected final GridBagLayout gridbag = new GridBagLayout();
    protected final GridBagConstraints c = new GridBagConstraints();

    public AbstractPropertyDialog(final Explorer controller, final EventBus bus, final Window parent) {
        super(parent, null, ModalityType.APPLICATION_MODAL);

        this.controller = controller;
        this.bus = bus;
        this.messages = controller.getMessages();

        Dimension size = new Dimension(200, 200);
        setMinimumSize(size);
        setResizable(false);
        setUndecorated(true);

        setFont(new Font("Calibri", Font.PLAIN, 14));
        getContentPane().setBackground(Color.WHITE);
        setLayout(gridbag);

        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1.0;
    }

    protected void setLabel(String text) {
        JLabel title = new JLabel(text, JLabel.CENTER);
        title.setFont(new Font("Calibri", Font.BOLD, 16));
        gridbag.setConstraints(title, c);
        add(title);
    }

    protected void setAction(Action action) {
        JButton select = new JButton(action);
        select.setFont(new Font("Calibri", Font.PLAIN, 14));
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weighty = 1.0;
        gridbag.setConstraints(select, c);
        add(select);
    }

    protected void setCancel(String text) {
        @SuppressWarnings("serial")
        JButton cancel = new JButton(new AbstractAction(text) {
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
    }

    protected <T> Supplier<T> addProperty(String string, T value, GridBagLayout gridbag, GridBagConstraints c, AbstractFormatter formatter) {
        addLabel(string, gridbag, c);

        final JFormattedTextField field = new JFormattedTextField(formatter);
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

    protected <T> Supplier<T> addDropDown(String string, T value, GridBagLayout gridbag, GridBagConstraints c, T...items) {
        addLabel(string, gridbag, c);

        final JComboBox field = new JComboBox<T>(items);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setSelectedItem(value);
        field.setEditable(false);
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
                return (T) field.getSelectedItem();
            }
        };
        return supplier;
    }

    protected Supplier<Boolean> addCheckBox(String string, Boolean value, GridBagLayout gridbag, GridBagConstraints c) {
        addLabel(string, gridbag, c);

        final JCheckBox field = new JCheckBox();
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setSelected(value);

        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 2.0;
        gridbag.setConstraints(field, c);
        add(field);

        Supplier<Boolean> supplier = new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return field.isSelected();
            }
        };
        return supplier;
    }

    private void addLabel(String string, GridBagLayout gridbag, GridBagConstraints c) {
        JLabel label = new JLabel(string, JLabel.RIGHT);
        label.setFont(new Font("Calibri", Font.PLAIN, 14));
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 2.0;
        c.weighty = 2.0;
        gridbag.setConstraints(label, c);
        add(label);
    }

    public void showDialog() {
        pack();

        setLocationRelativeTo(getParent());
        setVisible(true);
    }

}
