/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

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
        g.drawImage(splash, new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        g.dispose();
    }

    public void open() {
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
