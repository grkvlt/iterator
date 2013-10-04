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
package iterator;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Resources;

/**
 * About dialog.
 */
public class About extends JPanel implements MouseListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1378560167379537595L;

    private JDialog about;
    private BufferedImage splash;

    public About(EventBus bus, Explorer controller) {
        super();

        splash = Explorer.loadImage(Resources.getResource("splash.png"));
        setSize(splash.getWidth(), splash.getHeight());

        about = new JDialog(controller, "About IFS Explorer", ModalityType.APPLICATION_MODAL);
        about.setLayout(new BorderLayout());
        about.add(this, BorderLayout.CENTER);
        about.pack();

        Dimension size = new Dimension(splash.getWidth(), splash.getHeight() + about.getInsets().top);

        about.setSize(size);
        about.setPreferredSize(size);
        about.setMinimumSize(size);
        about.setResizable(false);

        about.addMouseListener(this);
    }

    public void showDialog() {
        about.setLocationByPlatform(true);
        about.setVisible(true);
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Splash.paintSplash(g, splash, getWidth(), getHeight());
        g.dispose();
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) {
        about.setVisible(false);
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) {
    }
}
