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
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

import com.google.common.io.Resources;

import iterator.util.Dialog;
import iterator.util.Utils;
import iterator.util.Version;

/**
 * Splash screen.
 */
public class Splash extends JPanel implements Dialog, ActionListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1028745784181961863L;

    public static final Long SPLASH_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(3);

    private JWindow splash;
    private BufferedImage image;

    public Splash() {
        super();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        image = Utils.loadImage(Resources.getResource("splash.png"));
        Dimension size = new Dimension(image.getWidth(), image.getHeight());
        setSize(size);

        splash = new JWindow();

        splash.getContentPane().setLayout(new BorderLayout());
        splash.getContentPane().add(this, BorderLayout.CENTER);
        splash.pack();

        splash.setSize(size);
        splash.setPreferredSize(size);
        splash.setMinimumSize(size);
        splash.setLocation((screen.width / 2) - (size.width / 2), (screen.height / 2) - (size.height / 2));
        splash.setAlwaysOnTop(true);
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
        paintSplash(g, image, getWidth(), getHeight());
        g.dispose();
    }

    /** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
    @Override
    public void actionPerformed(ActionEvent e) {
        splash.setVisible(false);
    }

    /** @see iterator.util.Dialog#showDialog() */
    @Override
    public void showDialog() {
        splash.setVisible(true);

        Timer timer = new Timer(SPLASH_TIMEOUT_MS.intValue(), this);
        timer.setRepeats(false);
        timer.start();
    }

}
