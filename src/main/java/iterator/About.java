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
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RescaleOp;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.google.common.eventbus.EventBus;

/**
 * About dialog.
 */
public class About extends JPanel implements MouseListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private JDialog about;
    private BufferedImage splash;

    public About(EventBus bus, Explorer controller) {
        super();

        splash = controller.getSplash();
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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float[] scales = { 1f, 1f, 1f, 0.5f };
        float[] offsets = new float[4];
        BufferedImageOp filter = new RescaleOp(scales, offsets, null);
        g.drawImage(filter.filter(splash, null), 0, 0, null);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Calibri", Font.BOLD, 40));
        g.drawString("IFS Explorer Version 1.0.0", 10, 40);
        g.setFont(new Font("Calibri", Font.BOLD, 15));
        g.drawString("Copyright 2012 by Andrew Kennedy", 10, getHeight() - 10);
        g.drawString("http://grkvlt.github.com/iterator", 260, getHeight() - 10);
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
