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

import static iterator.Utils.context;
import static iterator.Utils.throwError;
import static iterator.util.Config.GRADIENT_END_PROPERTY;
import static iterator.util.Config.GRADIENT_START_PROPERTY;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.Utils;
import iterator.util.Formatter.BaseFormatter;
import iterator.util.Property.OptionalProperty;

/**
 * Abstract dialog box for setting properties.
 */
public abstract class AbstractPropertyDialog<T extends AbstractPropertyDialog<T>> extends JDialog implements Dialog<T>, KeyListener, ActionListener {

    protected final Messages messages;
    protected final Explorer controller;
    protected final Config config;
    protected final EventBus bus;
    protected final GridBagLayout gridbag = new GridBagLayout();
    protected final GridBagConstraints constraints = new GridBagConstraints();

    public AbstractPropertyDialog(Explorer controller) {
        super(controller, null, ModalityType.APPLICATION_MODAL);

        this.controller = controller;
        this.bus = controller.getEventBus();
        this.messages = controller.getMessages();
        this.config = controller.getConfig();

        addKeyListener(this);
        Dimension size = new Dimension(200, 100);
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
        JButton success = new JButton(text);
        success.setFont(CALIBRI_BOLD_14);
        success.addActionListener(this);
        success.addKeyListener(this);

        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weighty = 1.0;
        gridbag.setConstraints(success, constraints);

        add(success);
    }

    protected void setCancel(String text) {
        Action failure = Utils.action(text, e -> {
            setVisible(false);
            onCancel();
        });

        JButton cancel = new JButton(failure);
        cancel.setFont(CALIBRI_BOLD_ITALIC_14);
        cancel.addKeyListener(this);

        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cancel, constraints);

        addComponent(cancel);
    }

    protected <V, P extends Property<V>> P addProperty(String string,  BaseFormatter<V> formatter) {
        return addFormattedTextField(string, formatter, true, false);
    }

    protected <V, P extends Property<V>> P addReadOnlyProperty(String string, BaseFormatter<V> formatter) {
        return addFormattedTextField(string, formatter, false, false);
    }

    protected <V> OptionalProperty<V> addOptionalProperty(String string, BaseFormatter<Optional<V>> formatter) {
        return addFormattedTextField(string, formatter, true, true);
    }

    protected <V, P extends Property<V>> P addFormattedTextField(String string, BaseFormatter<V> formatter, boolean editable, boolean optional) {
        addLabel(string, editable ? CALIBRI_PLAIN_14 : CALIBRI_ITALIC_14);

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setHorizontalAlignment(JTextField.LEFT);
        Border border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5), BorderFactory.createLoweredSoftBevelBorder());
        field.setBorder(editable ? border : BorderFactory.createEmptyBorder(5, 5, 5, 5));
        field.setFont(CALIBRI_ITALIC_14);
        field.addKeyListener(this);
        field.setEditable(editable);

        addComponent(field);

        if (optional) {
            return (P) OptionalProperty.attach(field);
        } else {
            return (P) Property.attach(field);
        }
    }

    protected <V> Property<V> addDropDown(String string, V...items) {
        addLabel(string);

        JComboBox<V> field = new JComboBox<V>(items);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setEditable(false);
        field.setFont(CALIBRI_ITALIC_14);
        field.addKeyListener(this);

        setConstraints(field, 1, GridBagConstraints.REMAINDER);
        add(field);

        return Property.attach(field);
    }

    protected Property<Integer> addSpinner(String string, int min, int max) {
        addLabel(string);

        SpinnerNumberModel model = new SpinnerNumberModel(min, min, max, 1);
        JSpinner field = new JSpinner(model);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.addKeyListener(this);
        field.setFont(CALIBRI_ITALIC_14);

        addComponent(field);

        return Property.attach(field);
    }

    protected Property<Boolean> addCheckBox(String string) {
        addLabel(string);

        JCheckBox field = new JCheckBox();
        field.setBorder(BorderFactory.createEmptyBorder());
        field.addKeyListener(this);
        field.setFont(CALIBRI_ITALIC_14);

        addComponent(field);

        return Property.attach(field);
    }

    protected Property<Pair<Color>> addGradientPicker(String string) {
        addLabel(string);

        JButton field = new JButton() {
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);

                // Draw the gradient fill inside the border
                context(throwError(), graphics, g -> {
                    Color start = (Color) getClientProperty(GRADIENT_START_PROPERTY);
                    Color end = (Color) getClientProperty(GRADIENT_END_PROPERTY);
                    GradientPaint fill = new GradientPaint(20f, 0f, start, getWidth() - 20, 0, end);
                    g.setPaint(fill);
                    g.fill(new Rectangle(7, 2, getWidth() - 14, getHeight() - 4));
                });
            }
        };

        Property<Pair<Color>> property = new Property<Pair<Color>>() {
            @Override
            public Pair<Color> get() {
                Color left = (Color) field.getClientProperty(GRADIENT_START_PROPERTY);
                Color right = (Color) field.getClientProperty(GRADIENT_END_PROPERTY);
                return Pair.of(left, right);
            }
            @Override
            public void set(Pair<Color> value) {
                field.putClientProperty(GRADIENT_START_PROPERTY, value.getLeft());
                field.putClientProperty(GRADIENT_END_PROPERTY, value.getRight());
            }
        };

        // Show custom colour picker depending on side clicked
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                boolean left = (me.getX() < field.getWidth() / 2);
                Pair<Color> gradient = property.get();
                JColorChooser chooser = new JColorChooser(left ? gradient.getLeft() : gradient.getRight());
                chooser.setPreviewPanel(new JPanel());
                Arrays.asList(chooser.getChooserPanels()).stream()
                        .filter(c -> c.getDisplayName().toLowerCase(Locale.UK).contains("swatch"))
                        .findFirst()
                        .ifPresent(c -> {
                            chooser.removeChooserPanel(c);
                        });;
                JDialog dialog = JColorChooser.createDialog(field, string, true, chooser, e -> {
                    if (left) {
                        gradient.setLeft(chooser.getColor());
                    } else {
                        gradient.setRight(chooser.getColor());
                    }
                    property.set(gradient);
                }, null);
                dialog.setVisible(true);
            }
        });

        field.setOpaque(true);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5), BorderFactory.createLoweredSoftBevelBorder()));
        field.addKeyListener(this);

        setConstraints(field, 1, GridBagConstraints.REMAINDER);
        add(field);
        return property;
    }

    private void addComponent(JComponent field) {
        setConstraints(field, 1, GridBagConstraints.RELATIVE);
        add(field);
        JLabel spacer = new JLabel(" ");
        setConstraints(spacer, 2, GridBagConstraints.REMAINDER);
        add(spacer);
    }

    private void addLabel(String string) {
        addLabel(string, CALIBRI_PLAIN_14);
    }

    private void addLabel(String string, Font font) {
        JLabel label = new JLabel(string, JLabel.RIGHT);
        label.setFont(font);
        label.setBorder(BorderFactory.createEmptyBorder(2, 22, 2, 2));

        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 2.0;
        constraints.weighty = 2.0;
        gridbag.setConstraints(label, constraints);

        add(label);
    }

    private void setConstraints(JComponent component, int x, int width) {
        constraints.gridx = x;
        constraints.gridwidth = width;
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

    /** @see java.lang.AutoCloseable#close() */
    @Override
    public void close() throws Exception {
        dispose();
    }

}
