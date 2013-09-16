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
import iterator.model.Transform;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingConstants;

/**
 * Properties dialog.
 *
 * TODO implement matrix editor
 */
public class Properties extends JDialog {
    /** serialVersionUID */
    private static final long serialVersionUID = -7626627964747215623L;

    private Transform transform;

    public Properties(Transform transform, Explorer controller) {
        super(controller, "Transform Properties", ModalityType.APPLICATION_MODAL);

        this.transform = transform;

        setLayout(new BorderLayout());
        add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
            }
        }), SwingConstants.SOUTH_EAST);
        pack();
        Dimension size = new Dimension(200, 200);
        setSize(size);
        setPreferredSize(size);
        setMinimumSize(size);
        setResizable(false);
    }

    public void showDialog() {
        setLocationByPlatform(true);
        setVisible(true);
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.drawChars("Properties".toCharArray(), 0, "Properties".length(), 0, 100);
        g.dispose();
    }
}
