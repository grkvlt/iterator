/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * IFS Editor.
 */
public class Editor extends JPanel implements MouseInputListener, KeyListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private final EventBus bus;
    private final Explorer controller;

    private JPopupMenu transform;
    
    private IFS ifs;
    private Transform selected;
    private Transform resize, move, rotate;
    private Point start, end;
    private Cursor corner;

    @SuppressWarnings("serial")
    public Editor(EventBus bus, Explorer controller) {
        super();
        this.bus = bus;
        this.controller = controller;
        
        transform = new JPopupMenu();
        transform.add(new AbstractAction("Properties") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        transform.add(new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ifs.deleteTransform(selected);
                selected = null;
                Editor.this.bus.post(ifs);
            }
        });
        transform.add(new AbstractAction("Duplicate") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transform copy = ifs.newTransform(getSize());
                copy.x = selected.x + 50;
                copy.y = selected.y + 50;
                copy.w = selected.w;
                copy.h = selected.h;
                copy.r = selected.r;
                selected = copy;
                Editor.this.bus.post(ifs);
            }
        });
        transform.add(new AbstractAction("Raise") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(selected.getZIndex() + 1);
                Editor.this.bus.post(ifs);
            }
        });
        transform.add(new AbstractAction("Lower") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(selected.getZIndex() - 1);
                Editor.this.bus.post(ifs);
            }
        });
        transform.add(new AbstractAction("Move to Front") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(Iterables.getLast(ifs.getTransforms()).getZIndex() + 1);
                Editor.this.bus.post(ifs);
            }
        });
        transform.add(new AbstractAction("Move to Back") {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(Iterables.get(ifs.getTransforms(), 0).getZIndex() - 1);
                Editor.this.bus.post(ifs);
            }
        });
        add(transform);
        
        addMouseListener(this);
        addMouseMotionListener(this);

        bus.register(this);
    }
    
    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;
        start = null;
        end = null;
        resize = null;
        move = null;
        rotate = null;
        if (!ifs.getTransforms().contains(selected)) {
	        selected = null;
        }
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }
    
    @Subscribe
    public void size(Dimension size) {
        ifs.setSize(size);
        bus.post(ifs);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintGrid(g);
        if (ifs != null) {
            for (Transform t : ifs.getTransforms()) {
                if (!t.equals(selected)) {
                    paintTransform(t, false, g);
                }
            }
            if (selected != null) {
	            paintTransform(selected, true, g);
            }
            
            if (selected == null && start != null && end != null) {
                g.setPaint(Color.BLACK);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5f, 5f }, 0f));
                int x = Math.min(start.x, end.x);
                int y = Math.min(start.y, end.y);
                int w = Math.max(start.x, end.x) - x;
                int h = Math.max(start.y, end.y) - y;
                Rectangle ants = new Rectangle(x, y, w, h);
                g.draw(ants);
            }
        }
        g.dispose();
    }
    
    public void paintTransform(Transform t, boolean highlight, Graphics2D g) {
        Rectangle unit = new Rectangle(getSize());
        Shape rect = t.getTransform().createTransformedShape(unit);
        
        // Fill the rectangle
        if (highlight) {
            g.setPaint(new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 128));
        } else {
            g.setPaint(new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 128));
        }
        g.fill(rect);
        
        // Draw the outline
        g.setPaint(Color.BLACK);
        if (highlight) {
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10f, 10f }, 0f));
        } else {
            g.setStroke(new BasicStroke(3f));
        }
        g.draw(rect);
        
        // Draw the resize handles
        g.setStroke(new BasicStroke(3f));
        g.setPaint(Color.BLACK);
        int[] cornerX = new int[] { 0, 0, getWidth(), getWidth() };
        int[] cornerY = new int[] { 0, getHeight(), getHeight(), 0 };
        for (int i = 0; i < 4; i++) {
            Point center = new Point();
            t.getTransform().transform(new Point(cornerX[i], cornerY[i]), center);
            Rectangle corner = new Rectangle(center.x - 5, center.y - 5, 10, 10);
            g.fill(corner);
        }
        
        // Draw the number
        Graphics2D gr = (Graphics2D) g.create();
        if (highlight) {
            gr.setPaint(Color.BLACK);
        } else {
            gr.setPaint(new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 128));
        }
        gr.setFont(new Font("Calibri", Font.BOLD, 25));
        Point text = new Point();
        t.getTransform().transform(new Point(0, 0), text);
        AffineTransform rotation = new AffineTransform();
        rotation.rotate(t.r, text.x, text.y);
        gr.setTransform(rotation);
        gr.drawString(String.format("T%02d%s", t.getId(), (highlight && rotate != null) ? String.format(" (%d)", (int) Math.toDegrees(t.r)) : ""), text.x + 5, text.y + 25);
        gr.dispose();
    }

    public void paintGrid(Graphics2D g) {
        Rectangle s = new Rectangle(getSize());
        g.setPaint(Color.WHITE);
        g.fill(s);
        g.setPaint(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x < getWidth(); x += 10) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 10) {
            g.drawLine(0, y, getWidth(), y);
        }
        g.setPaint(Color.GRAY);
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x < getWidth(); x += 50) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 50) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    public Transform getTransformAt(int x, int y) {
        Point point = new Point(x, y);
        return getTransformAt(point);
    }
    
    public Transform getTransformAt(Point point) {
        for (Transform t : Lists.reverse(ifs.getTransforms())) {
            Shape box = t.getTransform().createTransformedShape(new Rectangle(0, 0, getWidth(), getHeight()));
            if (box.contains(point)) {
                return t;
            }
        }
        return null;
    }
    
    public boolean isResize(Transform t, Point point) {
        return getCorner(t, point) != null;
    }
    
    public Cursor getCorner(Transform t, Point point) {
        int[] cornerX = new int[] { 0, 0, getWidth(), getWidth() };
        int[] cornerY = new int[] { 0, getHeight(), getHeight(), 0 };
        int[] cursors = new int[] { Cursor.NW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR };
        for (int i = 0; i < 4; i++) {
            Point center = new Point();
            t.getTransform().transform(new Point(cornerX[i], cornerY[i]), center);
            Rectangle corner = new Rectangle(center.x - 5, center.y - 5, 10, 10);
            if (corner.contains(point)) {
                return new Cursor(cursors[i]);
            }
        }
        return null;
    }
    
    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
        Transform clicked = getTransformAt(e.getPoint());
        if (SwingUtilities.isLeftMouseButton(e)) {
            resize = null; 
	        for (Transform t : ifs.getTransforms()) {
	            if (isResize(t, e.getPoint())) {
	                resize = t;
	                break;
	            }
	        }
	        if (resize != null) {
	            corner = getCorner(resize, e.getPoint());
	            setCursor(corner);
	            Point nw = new Point();
	            Point ne = new Point();
	            Point sw = new Point();
	            Point se = new Point();
	            resize.getTransform().transform(new Point(0, 0), nw);
	            resize.getTransform().transform(new Point(getWidth(), 0), ne);
	            resize.getTransform().transform(new Point(0, getHeight()), sw);
	            resize.getTransform().transform(new Point(getWidth(), getHeight()), se);
	            switch(corner.getType()) {
	            case Cursor.NW_RESIZE_CURSOR: start = nw; break;
	            case Cursor.NE_RESIZE_CURSOR: start = ne; break;
	            case Cursor.SW_RESIZE_CURSOR: start = sw; break;
	            case Cursor.SE_RESIZE_CURSOR: start = se; break;
	            }
	            ifs.deleteTransform(resize);
	            selected = resize;
	        } else if (clicked != null) {
	            if (e.isShiftDown()) {
                    selected = clicked;
                    rotate = selected;
                    ifs.deleteTransform(rotate);
                    start = snap(e.getPoint(), 10);
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	            } else {
                    selected = clicked;
                    move = selected;
                    ifs.deleteTransform(move);
                    start = snap(e.getPoint(), 10);
                    setCursor(new Cursor(Cursor.MOVE_CURSOR));
	            }
	        } else  {
	            start = snap(e.getPoint(), 10);
	            selected = null;
	            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	        }
            end = new Point(start.x, start.y);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            if (clicked != null) {
                selected = clicked;
                transform.show(e.getComponent(), e.getX(), e.getY());
            }
        }
        repaint();
    }

    private Point snap(Point point, int i) {
        return new Point(i * (point.x / i), i * (point.y / i));
    }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (selected == null && start != null && end != null) {
                int x = Math.min(start.x, end.x);
                int y = Math.min(start.y, end.y);
                int w = Math.max(start.x, end.x) - x;
                int h = Math.max(start.y, end.y) - y;
                
                w = Math.max(10, w);
                h = Math.max(10, h);
                
                selected = ifs.newTransform(getSize());
                selected.x = x;
                selected.y = y;
                selected.w = w;
                selected.h = h;
	            bus.post(ifs);
            } else if (selected != null  && start != null && end != null) {
                ifs.addTransform(selected);
	            bus.post(ifs);
            }
        }
    }

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /** @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent) */
    @Override public void mouseDragged(MouseEvent e) {
        if (start != null) {
	        end = snap(e.getPoint(), 10);
            if (selected != null && resize != null) {
                int w = resize.w;
                int h = resize.h;
                int dx = end.x - start.x;
                int dy = end.y - start.y;
                
                Point delta = new Point();
                Point inverseX = new Point();
                Point inverseY = new Point();
                AffineTransform reverse = new AffineTransform();
                reverse.rotate(-resize.r);
                reverse.transform(new Point(dx, dy), delta);
                try {
	                reverse.inverseTransform(new Point(delta.x, 0), inverseX);
	                reverse.inverseTransform(new Point(0, delta.y), inverseY);
                } catch (NoninvertibleTransformException e1) {
                    Throwables.propagate(e1);
                }
                
                int x = resize.x;
                int y = resize.y;

                switch(corner.getType()) {
                case Cursor.NW_RESIZE_CURSOR:
                    x += dx; y += dy; w -= delta.x; h -= delta.y;
                    break;
                case Cursor.NE_RESIZE_CURSOR: 
                    x += inverseY.x; y += inverseY.y; w += delta.x; h -= delta.y;
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    x += inverseX.x; y += inverseX.y; w -= delta.x; h += delta.y;
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    w += delta.x; h += delta.y;
                    break;
                }
                
                w = Math.max(10, w);
                h = Math.max(10, h);
                
                selected = new Transform(selected.getId(), selected.getZIndex(), getSize());
                selected.x = x;
                selected.y = y;
                selected.w = w;
                selected.h = h;
                selected.r = resize.r;
	        } else if (selected != null && move != null) {
                int dx = end.x - start.x;
                int dy = end.y - start.y;
                
                int x = move.x + dx;
                int y = move.y + dy;

                selected = new Transform(selected.getId(), selected.getZIndex(), getSize());
                selected.x = x;
                selected.y = y;
                selected.w = move.w;
                selected.h = move.h;
                selected.r = move.r;
            } else if (selected != null && rotate != null) {
                Point origin = new Point();
                rotate.getTransform().transform(new Point(0, 0), origin);
                int dx = end.x - origin.x;
                int dy = end.y - origin.y;
                double r = Math.atan2(dy, dx);

                selected = new Transform(selected.getId(), selected.getZIndex(), getSize());
                selected.x = rotate.x;
                selected.y = rotate.y;
                selected.w = rotate.w;
                selected.h = rotate.h;
                selected.r = r;
            }
	        repaint();
	    }
    }

    /** @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent) */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (start == null) {
	        if (getTransformAt(e.getPoint()) != null) {
	            setCursor(new Cursor(Cursor.HAND_CURSOR));
	        } else {
	            for (Transform t : ifs.getTransforms()) {
		            Cursor corner = getCorner(t, e.getPoint());
		            if (corner != null) {
		                setCursor(corner);
		                return;
		            }
	            }
	            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	        }
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (selected != null) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                ifs.deleteTransform(selected);
                selected = null;
                Editor.this.bus.post(ifs);
                break;
            case KeyEvent.VK_RIGHT:
                selected.r += Math.PI / 2d;
                Editor.this.bus.post(ifs);
                break;
            case KeyEvent.VK_LEFT:
                selected.r -= Math.PI / 2d;
                Editor.this.bus.post(ifs);
                break;
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }
}
