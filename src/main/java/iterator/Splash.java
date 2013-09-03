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

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.google.common.eventbus.EventBus;

/**
 * Splash screen.
 */
public class Splash extends JPanel implements ActionListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private JFrame parent;
    private BufferedImage splash;

    public Splash(EventBus bus, Explorer controller) {
        super();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        splash = controller.getSplash();

        parent = new JFrame();
        parent.setUndecorated(true);
        parent.setAlwaysOnTop(true);
        parent.setResizable(false);

        Dimension size = new Dimension(splash.getWidth(), splash.getHeight());
        parent.setSize(size);
        parent.setPreferredSize(size);
        parent.setMinimumSize(size);
        parent.setLocation((screen.width / 2) - (size.width / 2), (screen.height / 2) - (size.height / 2));

        parent.getContentPane().add(this);
    }

    public static void paintSplash(Graphics2D g, BufferedImage image, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float[] scales = { 1f, 1f, 1f, 0.5f };
        float[] offsets = new float[4];
        BufferedImageOp filter = new RescaleOp(scales, offsets, null);
        g.drawImage(filter.filter(image, null), 0, 0, null);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Calibri", Font.BOLD, 80));
        g.drawString("IFS Explorer", 10, 65);
        g.setFont(new Font("Calibri", Font.BOLD, 25));
        g.drawString("Version 1.0.4-SNAPSHOT", 10, 100);
        g.setFont(new Font("Calibri", Font.BOLD, 13));
        g.drawString("Copyright 2012-2013 by Andrew Kennedy", 10, height - 15);
        g.setFont(new Font("Consolas", Font.BOLD, 12));
        g.drawString("http://grkvlt.github.com/iterator", 260, height - 15);
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        paintSplash(g, splash, getWidth(), getHeight());
        g.dispose();
    }

    public void showDialog() {
        parent.setVisible(true);
        Timer timer = new Timer(5000, this);
        timer.setRepeats(false);
        timer.start();
    }

    /** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
    @Override
    public void actionPerformed(ActionEvent e) {
        parent.setVisible(false);
    }
}
