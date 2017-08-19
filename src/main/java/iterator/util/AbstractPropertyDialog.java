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
package iterator.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;

/**
 * Abstract dialog box for setting properties.
 */
public abstract class AbstractPropertyDialog extends JDialog implements Dialog, KeyListener, ActionListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -7626627964747215623L;

    protected final Messages messages;
    protected final Explorer controller;
    protected final EventBus bus;
    protected final GridBagLayout gridbag = new GridBagLayout();
    protected final GridBagConstraints constraints = new GridBagConstraints();

    public AbstractPropertyDialog(final Explorer controller, final EventBus bus, final Window parent) {
        super(parent, null, ModalityType.APPLICATION_MODAL);

        this.controller = controller;
        this.bus = bus;
        this.messages = controller.getMessages();

        addKeyListener(this);
        Dimension size = new Dimension(200, 200);
        setMinimumSize(size);
        setResizable(false);
        setUndecorated(true);

        setFont(new Font("Calibri", Font.PLAIN, 14));
        getContentPane().setBackground(Color.WHITE);
        setLayout(gridbag);

        constraints.insets = new Insets(2, 5, 2, 5);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weighty = 1.0;
    }

    protected void setLabel(String text) {
        JLabel title = new JLabel(text, JLabel.CENTER);
        title.setFont(new Font("Calibri", Font.BOLD, 16));
        gridbag.setConstraints(title, constraints);
        add(title);
    }

    protected void setSuccess(String text) {
        JButton select = new JButton(text);
        select.addActionListener(this);
        select.setFont(new Font("Calibri", Font.PLAIN, 14));
        select.addKeyListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weighty = 1.0;
        gridbag.setConstraints(select, constraints);
        add(select);
    }

    protected void setCancel(String text) {
        Action failure = new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                onCancel();
            }
        };
        JButton cancel = new JButton(failure);
        cancel.setFont(new Font("Calibri", Font.PLAIN, 14));
        cancel.addKeyListener(this);
        constraints.gridx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancel, constraints);
        add(cancel);
    }

    protected <T> Supplier<T> addProperty(String string, T value, AbstractFormatter formatter) {
        addLabel(string);

        final JFormattedTextField field = new JFormattedTextField(formatter);
        field.setHorizontalAlignment(JTextField.LEFT);
        field.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        field.setMargin(new Insets(2, 2, 2, 2));
        field.setValue(value);
        field.setFont(new Font("Cambria", Font.ITALIC, 14));
        field.addKeyListener(this);

        constraints.gridx = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(field, constraints);
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

    protected <T> Supplier<T> addDropDown(String string, T value, T...items) {
        addLabel(string);

        final JComboBox field = new JComboBox<T>(items);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setSelectedItem(value);
        field.setEditable(false);
        field.setFont(new Font("Cambria", Font.ITALIC, 14));
        field.addKeyListener(this);

        constraints.gridx = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(field, constraints);
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

    protected Supplier<Integer> addSpinner(String string, int value, int min, int max) {
        addLabel(string);

        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, 1);
        final JSpinner field = new JSpinner(model);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.addKeyListener(this);

        constraints.gridx = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(field, constraints);
        add(field);

        Supplier<Integer> supplier = new Supplier<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public Integer get() {
                return (Integer) field.getValue();
            }
        };
        return supplier;
    }

    protected Supplier<Boolean> addCheckBox(String string, Boolean value) {
        addLabel(string);

        final JCheckBox field = new JCheckBox();
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setSelected(value);
        field.addKeyListener(this);

        constraints.gridx = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(field, constraints);
        add(field);

        Supplier<Boolean> supplier = new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return field.isSelected();
            }
        };
        return supplier;
    }

    private void addLabel(String string) {
        JLabel label = new JLabel(string, JLabel.RIGHT);
        label.setFont(new Font("Calibri", Font.PLAIN, 14));
        constraints.gridx = 0;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        constraints.weightx = 2.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(label, constraints);
        add(label);
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        pack();

        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
    @Override
    public void actionPerformed(ActionEvent e) {
        setVisible(false);
        onSuccess();
    }

    public void onCancel() { }

    public void onSuccess() { }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                if (e.isMetaDown()) {
                    setVisible(false);
                    onSuccess();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                setVisible(false);
                onCancel();
                break;
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

}
