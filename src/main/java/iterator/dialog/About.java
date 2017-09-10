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

import static iterator.Utils.calibri;
import static iterator.Utils.consolas;
import static iterator.Utils.context;
import static iterator.Utils.loadImage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.google.common.io.Resources;

import iterator.Explorer;
import iterator.util.Dialog;
import iterator.util.Version;

/**
 * About dialog.
 */
public class About extends JPanel implements Dialog<About>, MouseListener {

    private final Explorer controller;

    private JDialog about;
    private BufferedImage image;

    public static About dialog(Explorer controller) {
        return new About(controller);
    }

    private About(Explorer controller) {
        super();

        this.controller = controller;

        image = loadImage(Resources.getResource("splash.png"));
        Dimension size = new Dimension(image.getWidth(), image.getHeight());
        setSize(size);

        about = new JDialog(controller, null, ModalityType.APPLICATION_MODAL);

        about.setUndecorated(true);
        about.setLayout(new BorderLayout());
        about.add(this, BorderLayout.CENTER);
        about.pack();

        about.setSize(size);
        about.setPreferredSize(size);
        about.setMinimumSize(size);
        about.setResizable(false);
        about.addMouseListener(this);
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        about.setLocationRelativeTo(null);
        about.setVisible(true);
    }

    @Override
    public void paint(Graphics graphics) {
        context(controller, graphics, g -> {
            g.drawImage(image, AffineTransform.getTranslateInstance(0d, 0d), null);
            paintSplashText(g, getWidth(), getHeight());
        });
    }

    /**
     * Paints text over the spash screen image.
     */
    public static void paintSplashText(Graphics2D g, int width, int height) {
        g.setColor(Color.BLACK);
        g.setFont(calibri(Font.BOLD, 80));
        g.drawString("IFS Explorer", 10, 65);
        g.setFont(calibri(Font.BOLD, 25));
        g.drawString("Version " + Version.instance().get(), 10, 100);
        g.setFont(calibri(Font.BOLD, 13));
        g.drawString("Copyright 2012-2017 by Andrew Kennedy", 10, height - 15);
        g.setFont(consolas(Font.BOLD, 12));
        g.drawString("http://grkvlt.github.io/iterator/", 260, height - 15);
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) {
        about.setVisible(false);
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    @Override
    public void mouseEntered(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) { }

    /** @see java.lang.AutoCloseable#close() */
    @Override
    public void close() throws Exception {
        about.dispose();
    }

}
