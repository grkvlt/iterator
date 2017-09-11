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

import static iterator.util.Messages.DIALOG_MATRIX_BUTTON_CANCEL;
import static iterator.util.Messages.DIALOG_MATRIX_BUTTON_UPDATE;
import static iterator.util.Messages.DIALOG_MATRIX_TITLE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.Utils;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Dialog;
import iterator.util.Formatter;
import iterator.util.Messages;
import iterator.util.Property;
import iterator.view.Details;

/**
 * Matrix dialog.
 */
public class Matrix extends JDialog implements Dialog<Matrix>, KeyListener, ComponentListener {

    private final Property<Double> c0, c1, c2, c3, c4, c5;
    private final Messages messages;
    private final Transform transform;
    private final EventBus bus;

    private JPanel matrix;
    private JButton update, cancel;

    public static Matrix dialog(Explorer controller, Transform transform, IFS ifs) {
        return new Matrix(controller, transform, ifs);
    }

    private Matrix(Explorer controller, Transform transform, IFS ifs) {
        super(controller, null, ModalityType.APPLICATION_MODAL);

        this.transform = transform;
        this.messages = controller.getMessages();
        this.bus = controller.getEventBus();

        addKeyListener(this);
        addComponentListener(this);

        setUndecorated(true);
        setResizable(false);
        setLayout(new BorderLayout());
        setFont(CALIBRI_PLAIN_14);
        getContentPane().setBackground(Color.WHITE);

        JLabel title = new JLabel(messages.getText(DIALOG_MATRIX_TITLE), JLabel.CENTER);
        title.setFont(CALIBRI_BOLD_16);
        add(title, BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        add(panel, BorderLayout.CENTER);

        JTextPane left = new JTextPane();
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        left.setEditable(false);
        left.setContentType(Details.HTML_MIME_TYPE);
        left.setText(Details.INITIAL_CONTENT_HTML);
        HTMLEditorKit leftKit = (HTMLEditorKit) left.getEditorKitForContentType(Details.HTML_MIME_TYPE);
        StyleSheet leftCss = leftKit.getStyleSheet();
        for (String rule : Details.CSS_BRACKET_RULES) {
            leftCss.addRule(rule);
        }
        left.setText("<div class=\"bracketl\" height=\"60px\" width=\"10px\">&nbsp;</div>");
        panel.add(left, BorderLayout.WEST);

        matrix = new JPanel();
        matrix.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridLayout grid = new GridLayout(0, 3);
        grid.setHgap(10);
        grid.setVgap(4);
        matrix.setLayout(grid);
        matrix.setBackground(Color.WHITE);
        panel.add(matrix, BorderLayout.CENTER);

        c0 = addProperty(matrix, 4);
        c1 = addProperty(matrix, 4);
        c2 = addProperty(matrix, 1);
        c3 = addProperty(matrix, 4);
        c4 = addProperty(matrix, 4);
        c5 = addProperty(matrix, 1);

        JTextPane right = new JTextPane();
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        right.setEditable(false);
        right.setContentType(Details.HTML_MIME_TYPE);
        right.setText(Details.INITIAL_CONTENT_HTML);
        HTMLEditorKit rightKit = (HTMLEditorKit) left.getEditorKitForContentType(Details.HTML_MIME_TYPE);
        StyleSheet rightCss = rightKit.getStyleSheet();
        for (String rule : Details.CSS_BRACKET_RULES) {
            rightCss.addRule(rule);
        }
        right.setText("<div class=\"bracketr\" height=\"60px\" width=\"10px\">&nbsp;</div>");
        panel.add(right, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(Color.WHITE);
        add(buttons, BorderLayout.SOUTH);

        Action success = Utils.action(messages.getText(DIALOG_MATRIX_BUTTON_UPDATE), e -> {
            double m[] = new double[6];

            m[0] = c0.get(); m[2] = c1.get(); m[4] = c2.get();
            m[1] = c3.get(); m[3] = c4.get(); m[5] = c5.get();

            transform.setMatrix(m);
            bus.post(ifs);
            setVisible(false);
        });
        update = new JButton(success);
        update.setFont(CALIBRI_BOLD_14);
        update.addKeyListener(this);
        buttons.add(update);

        Action failure = Utils.action(messages.getText(DIALOG_MATRIX_BUTTON_CANCEL), e -> {
            setVisible(false);
        });
        cancel = new JButton(failure);
        cancel.setFont(CALIBRI_BOLD_ITALIC_14);
        cancel.addKeyListener(this);
        buttons.add(cancel);
    }

    private Property<Double> addProperty(JPanel panel, int digits) {
        final JFormattedTextField field = new JFormattedTextField(Formatter.doubles(digits));
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        field.setFont(CALIBRI_ITALIC_14);
        field.setColumns(8);
        field.addKeyListener(this);
        panel.add(field);

        Property<Double> supplier = new Property<Double>() {
            @Override
            public Double get() {
                return (Double) field.getValue();
            }
            @Override
            public void set(Double value) {
                field.setValue(value);
            }
        };
        return supplier;
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        double m[] = new double[6];
        transform.getTransform().getMatrix(m);;

        c0.set(m[0]); c1.set(m[2]); c2.set(m[4]);
        c3.set(m[1]); c4.set(m[3]); c5.set(m[5]);

        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                if (e.isMetaDown()) {
                    update.doClick();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                cancel.doClick();
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

    @Override
    public void componentResized(ComponentEvent e) { }

    @Override
    public void componentMoved(ComponentEvent e) { }

    @Override
    public void componentShown(ComponentEvent e) {
        matrix.getComponent(0).requestFocusInWindow();
    }

    @Override
    public void componentHidden(ComponentEvent e) { }

}
