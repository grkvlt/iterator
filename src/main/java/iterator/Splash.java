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
package iterator;

import iterator.util.Version;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RescaleOp;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

import com.google.common.io.Resources;

/**
 * Splash screen.
 */
public class Splash extends JPanel implements ActionListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1028745784181961863L;

    public static final int SPLASH_TIMEOUT_MS = 3000;

    private JWindow parent;
    private BufferedImage splash;

    public Splash() {
        super();

        splash = Explorer.loadImage(Resources.getResource("splash.png"));

        parent = new JWindow();
        parent.getContentPane().setLayout(new BorderLayout());
        parent.getContentPane().add(this, BorderLayout.CENTER);
        parent.pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = new Dimension(splash.getWidth(), splash.getHeight());

        parent.setSize(size);
        parent.setPreferredSize(size);
        parent.setMinimumSize(size);
        parent.setLocation((screen.width / 2) - (size.width / 2), (screen.height / 2) - (size.height / 2));
        parent.setAlwaysOnTop(true);
    }

    public static void paintSplash(Graphics2D g, BufferedImage image, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        float[] scales = { 1f, 1f, 1f, 0.75f };
        float[] offsets = new float[4];
        BufferedImageOp filter = new RescaleOp(scales, offsets, null);
        g.drawImage(image, filter, 0, 0);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Calibri", Font.BOLD, 80));
        g.drawString("IFS Explorer", 10, 65);
        g.setFont(new Font("Calibri", Font.BOLD, 25));
        g.drawString("Version " + Version.instance().get(), 10, 100);
        g.setFont(new Font("Calibri", Font.BOLD, 13));
        g.drawString("Copyright 2012-2017 by Andrew Kennedy", 10, height - 15);
        g.setFont(new Font("Consolas", Font.BOLD, 12));
        g.drawString("http://grkvlt.github.io/iterator/", 260, height - 15);
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        paintSplash(g, splash, getWidth(), getHeight());
        g.dispose();
    }

    /** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
    @Override
    public void actionPerformed(ActionEvent e) {
        parent.setVisible(false);
    }

    public void showDialog() {
        parent.setVisible(true);

        Timer timer = new Timer(SPLASH_TIMEOUT_MS, this);
        timer.setRepeats(false);
        timer.start();
    }

}
