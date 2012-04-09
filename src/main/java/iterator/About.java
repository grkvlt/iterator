/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.eventbus.EventBus;

/**
 * IFS Editor.
 */
public class About extends JPanel implements MouseListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private final EventBus bus;
    private final Explorer controller;

    private JDialog about;
    private JLabel link;
    private BufferedImage splash;

    public About(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;
        
        splash = controller.getSplash();
        
        about = new JDialog(controller, "IFS Explorere 1.0.0", ModalityType.APPLICATION_MODAL);
        about.setLayout(new BorderLayout());
        about.add(this, BorderLayout.CENTER);
        
        link = new JLabel("http://grkvlt.github.com/iterator", JLabel.CENTER);
        link.setFont(new Font("Calibri", Font.BOLD, 13));
        link.setOpaque(true);
        link.setBackground(Color.WHITE);
        link.setForeground(Color.BLACK);
        about.add(link, BorderLayout.SOUTH);
        
        about.setSize(splash.getWidth(), splash.getHeight() + about.getInsets().top + link.getHeight());
        about.setResizable(false);
        about.addMouseListener(this);

        bus.register(this);
    }
    
    public void showDialog() {
        about.setLocation((controller.getWidth() / 2) - (about.getWidth() / 2), (controller.getHeight() / 2) - (about.getHeight() / 2));
        about.setVisible(true);
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(splash, new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
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
