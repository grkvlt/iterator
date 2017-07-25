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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
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

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Messages;
import iterator.util.Utils;
import iterator.view.Details;

/**
 * Matrix dialog.
 */
public class Matrix extends JDialog implements KeyListener {
    /** serialVersionUID */
    private static final long serialVersionUID = 2515091470183119489L;

    private final Supplier<Double> c0, c1, c2, c3, c4, c5;
    private final Messages messages;

    private Action success, failure;

    public Matrix(final Transform transform, final IFS ifs, final Explorer controller, final EventBus bus, final Window parent) {
        super(parent, null, ModalityType.APPLICATION_MODAL);

        messages = controller.getMessages();

        addKeyListener(this);
        setUndecorated(true);
        setResizable(false);
        setLayout(new BorderLayout());
        setFont(new Font("Calibri", Font.PLAIN, 14));
        getContentPane().setBackground(Color.WHITE);

        String label = MessageFormat.format(messages.getText(DIALOG_MATRIX_TITLE), transform.getId());
        JLabel title = new JLabel(label, JLabel.CENTER);
        title.setFont(new Font("Calibri", Font.BOLD, 16));
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

        JPanel values = new JPanel();
        values.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridLayout grid = new GridLayout(0, 3);
        grid.setHgap(10);
        grid.setVgap(4);
        values.setLayout(grid);
        values.setBackground(Color.WHITE);
        panel.add(values, BorderLayout.CENTER);
        double matrix[] = new double[6];
        transform.getTransform().getMatrix(matrix);

        c0 = addProperty(0, matrix[0], values);
        c1 = addProperty(1, matrix[2], values);
        c2 = addProperty(2, matrix[4], values);
        c3 = addProperty(3, matrix[1], values);
        c4 = addProperty(4, matrix[3], values);
        c5 = addProperty(5, matrix[5], values);

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

        success = new AbstractAction(messages.getText(DIALOG_MATRIX_BUTTON_UPDATE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                double matrix[] = new double[6];
                matrix[0] = c0.get();
                matrix[2] = c1.get();
                matrix[4] = c2.get();
                matrix[1] = c3.get();
                matrix[3] = c4.get();
                matrix[5] = c5.get();
                transform.setMatrix(matrix);
                bus.post(ifs);
                setVisible(false);
            }
        };
        JButton update = new JButton(success);
        update.setFont(new Font("Calibri", Font.PLAIN, 14));
        update.addKeyListener(this);
        buttons.add(update);

        failure = new AbstractAction(messages.getText(DIALOG_MATRIX_BUTTON_CANCEL)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        JButton cancel = new JButton(failure);
        cancel.setFont(new Font("Calibri", Font.PLAIN, 14));
        cancel.addKeyListener(this);
        buttons.add(cancel);
    }

    private Supplier<Double> addProperty(int number, double value, JPanel panel) {
        final JFormattedTextField field = new JFormattedTextField(new Utils.DoubleFormatter());
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        field.setValue(value);
        field.setFont(new Font("Cambria", Font.ITALIC, 14));
        field.setColumns(8);
        field.addKeyListener(this);
        panel.add(field);

        Supplier<Double> supplier = new Supplier<Double>() {
            @Override
            public Double get() {
                return (Double) field.getValue();
            }
        };
        return supplier;
    }

    public void showDialog() {
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
                    success.actionPerformed(new ActionEvent(e.getSource(), e.getID(), null));
                }
                break;
            case KeyEvent.VK_ESCAPE:
                failure.actionPerformed(new ActionEvent(e.getSource(), e.getID(), null));
                break;
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

}
