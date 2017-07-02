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
package iterator.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;

import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;

/**
 * Matrix dialog.
 */
public class Matrix extends JDialog {
    /** serialVersionUID */
    private static final long serialVersionUID = 2515091470183119489L;

    private final Supplier<Double> c0, c1, c2, c3, c4, c5;

    public Matrix(final Transform transform, final IFS ifs, final EventBus bus, final Explorer controller) {
        super(controller, "Transform Matrix", ModalityType.APPLICATION_MODAL);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        setFont(new Font("Calibri", Font.PLAIN, 14));
        setLayout(gridbag);

        JLabel title = new JLabel(String.format("Transform T%02d Matrix", transform.getId()), JLabel.CENTER);
        title.setFont(new Font("Calibri", Font.BOLD, 16));
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 3.0;
        c.gridwidth = 3; // GridBagConstraints.REMAINDER;
        gridbag.setConstraints(title, c);
        add(title);

        double matrix[] = new double[6];
        transform.getTransform().getMatrix(matrix);
        
        c0 = addProperty(0, matrix[0], gridbag, c);
        c1 = addProperty(1, matrix[2], gridbag, c);
        c2 = addProperty(2, matrix[4], gridbag, c);
        c3 = addProperty(3, matrix[1], gridbag, c);
        c4 = addProperty(4, matrix[3], gridbag, c);
        c5 = addProperty(5, matrix[5], gridbag, c);

        @SuppressWarnings("serial")
        JButton update = new JButton(new AbstractAction("Update") {
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
        });
        update.setFont(new Font("Calibri", Font.PLAIN, 14));
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridwidth = 1;
        c.weightx = 2.0;
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
        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancel, c);
        add(cancel);

        pack();

        Dimension size = new Dimension(300, 150);
        setMinimumSize(size);
        setResizable(false);
    }


    private class DoubleFormatter extends AbstractFormatter {
        /** serialVersionUID */
        private static final long serialVersionUID = 1107330803545615990L;

        @Override
        public Object stringToValue(String text) throws ParseException {
            return Double.valueOf(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return String.format("%.06f", value);
        }
        
    }
    private Supplier<Double> addProperty(int number, double value, GridBagLayout gridbag, GridBagConstraints c) {
        final JFormattedTextField field = new JFormattedTextField(new DoubleFormatter());
        field.setValue(value);
        field.setFont(new Font("Cambria", Font.ITALIC, 14));
        field.setColumns(10);
        Supplier<Double> supplier = new Supplier<Double>() {
            @Override
            public Double get() {
                return (Double) field.getValue();
            }
        };

        int position = number % 3;
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = position == 0 ? GridBagConstraints.WEST : position == 2 ? GridBagConstraints.EAST : GridBagConstraints.CENTER;
        c.gridx = position;
        c.gridwidth = position == 2 ? GridBagConstraints.REMAINDER : 1;
        c.weightx = position == 1 ? 2.0 : 1.0;
        gridbag.setConstraints(field, c);
        add(field);

        return supplier;
    }

    public void showDialog() {
        setLocationByPlatform(true);
        setVisible(true);
    }

}
