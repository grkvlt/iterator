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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.google.common.eventbus.EventBus;

import iterator.Explorer;

/**
 * Abstract dialog box for setting properties.
 */
public abstract class AbstractPropertyDialog extends JDialog implements Dialog, KeyListener, ActionListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -7626627964747215623L;

    protected static final Font CALIBRI_PLAIN_14 = new Font("Calibri", Font.PLAIN, 14);
    protected static final Font CALIBRI_ITALIC_14 = new Font("Calibri", Font.ITALIC, 14);
    protected static final Font CALIBRI_BOLD_14 = new Font("Calibri", Font.BOLD, 14);
    protected static final Font CALIBRI_BOLD_16 = new Font("Calibri", Font.BOLD, 16);

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

        setFont(CALIBRI_PLAIN_14);
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
        title.setFont(CALIBRI_BOLD_16);
        gridbag.setConstraints(title, constraints);
        add(title);
    }

    protected void setSuccess(String text) {
        JButton select = new JButton(text);
        select.addActionListener(this);
        select.setFont(CALIBRI_PLAIN_14);
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
        cancel.setFont(CALIBRI_PLAIN_14);
        cancel.addKeyListener(this);

        constraints.gridx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancel, constraints);

        add(cancel);
    }

    protected <T> Property<T> addProperty(String string, T value, AbstractFormatter formatter) {
        return addFormattedTextField(string, value, formatter, true);
    }

    protected <T> Property<T> addReadOnlyProperty(String string, T value, AbstractFormatter formatter) {
        return addFormattedTextField(string, value, formatter, false);
    }

    protected <T> Property<T> addFormattedTextField(String string, T value, AbstractFormatter formatter, boolean editable) {
        addLabel(string, editable ? CALIBRI_PLAIN_14 : CALIBRI_ITALIC_14);

        final JFormattedTextField field = new JFormattedTextField(formatter);
        field.setHorizontalAlignment(JTextField.LEFT);
        field.setBorder(editable ? BorderFactory.createLoweredSoftBevelBorder() : BorderFactory.createEmptyBorder(3, 3, 3, 3));
        field.setMargin(new Insets(2, 2, 2, 2));
        field.setValue(value);
        field.setFont(CALIBRI_ITALIC_14);
        field.addKeyListener(this);
        field.setEditable(editable);

        setConstraints(field);
        add(field);

        Property<T> property = new Property<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                return (T) field.getValue();
            }
            @Override
            public void set(T value) {
                field.setValue(value);
            }
        };
        return property;
    }

    protected <T> Property<T> addDropDown(String string, T value, T...items) {
        addLabel(string);

        final JComboBox field = new JComboBox<T>(items);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setSelectedItem(value);
        field.setEditable(false);
        field.setFont(CALIBRI_ITALIC_14);
        field.addKeyListener(this);

        setConstraints(field);
        add(field);

        Property<T> property = new Property<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                return (T) field.getSelectedItem();
            }
            @Override
            public void set(T value) {
                field.setSelectedItem(value);
            }
        };
        return property;
    }

    protected Property<Integer> addSpinner(String string, int value, int min, int max) {
        addLabel(string);

        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, 1);
        final JSpinner field = new JSpinner(model);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.addKeyListener(this);
        field.setFont(CALIBRI_ITALIC_14);

        setConstraints(field);
        add(field);

        Property<Integer> property = new Property<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public Integer get() {
                return (Integer) field.getValue();
            }
            @Override
            public void set(Integer value) {
                field.setValue(value);
            }
        };
        return property;
    }

    protected Property<Boolean> addCheckBox(String string, Boolean value) {
        addLabel(string);

        final JCheckBox field = new JCheckBox();
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setSelected(value);
        field.addKeyListener(this);
        field.setFont(CALIBRI_ITALIC_14);

        setConstraints(field);
        add(field);

        Property<Boolean> property = new Property<Boolean>() {
            @Override
            public Boolean get() {
                return field.isSelected();
            }
            @Override
            public void set(Boolean value) {
                field.setSelected(value);
            }
        };
        return property;
    }

    private void addLabel(String string) {
        addLabel(string, CALIBRI_PLAIN_14);
    }

    private void addLabel(String string, Font font) {
        JLabel label = new JLabel(string, JLabel.RIGHT);
        label.setFont(font);

        constraints.gridx = 0;
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        constraints.weightx = 2.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(label, constraints);

        add(label);
    }

    private void setConstraints(JComponent component) {
        constraints.gridx = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(component, constraints);
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
