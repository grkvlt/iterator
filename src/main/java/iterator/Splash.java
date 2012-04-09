/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
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

    private final EventBus bus;
    private final Explorer controller;

    private JFrame parent;
    private BufferedImage splash;

    public Splash(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;

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

        bus.register(this);
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
