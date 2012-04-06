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
import java.awt.Shape;
import java.awt.event.ActionEvent;
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
public class Editor extends JPanel implements MouseInputListener {
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
        repaint();
    }
    
    @Subscribe
    public void size(Dimension size) {
        ifs.setSize(size);
        repaint();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
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
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10, 10 }, 0f));
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
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 25f, 25f}, 0f));
        } else {
            g.setStroke(new BasicStroke(4f));
        }
        g.draw(rect);

        double scaleX = Math.max(0.25d, t.getTransform().getScaleX());
        double scaleY = Math.max(0.25d, t.getTransform().getScaleY());
        double fiveX = 5d / scaleX; 
        double fiveY = 5d / scaleY; 
        
        // Draw the resize handles
        g.setStroke(new BasicStroke(4f));
        g.setPaint(Color.BLACK);
        double[] cornerX = new double[] { 0d, 0d, getWidth(), getWidth() };
        double[] cornerY = new double[] { 0d, getHeight(), getHeight(), 0d };
        for (int i = 0; i < 4; i++) {
            Rectangle corner = new Rectangle((int) (cornerX[i] - fiveX), (int) (cornerY[i] - fiveY), (int) (2d * fiveX), (int) (2d * fiveY));
            Shape handle = t.getTransform().createTransformedShape(corner);
            g.fill(handle);
        }
        
        // Draw the number
        Graphics2D gr = (Graphics2D) g.create();
        if (highlight) {
            gr.setPaint(Color.BLACK);
        } else {
            gr.setPaint(new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 128));
        }
        gr.setFont(new Font("Calibri", Font.BOLD, 25));
        float[] first = new float[2];
        rect.getPathIterator(null).currentSegment(first);
        AffineTransform rotation = new AffineTransform();
        rotation.rotate(t.r, first[0], first[1]);
        gr.setTransform(rotation);
        gr.drawString(String.format("T%02d", t.getId()), first[0] + 5, first[1] + 25f);
        gr.dispose();
    }

    public void paintGrid(Graphics2D g) {
        Rectangle s = new Rectangle(getSize());
        g.setPaint(Color.WHITE);
        g.fill(s);
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
        double scaleX = Math.max(0.25d, t.getTransform().getScaleX());
        double scaleY = Math.max(0.25d, t.getTransform().getScaleY());
        double fiveX = 5d / scaleX; 
        double fiveY = 5d / scaleY; 
        double[] cornerX = new double[] { 0d, 0d, getWidth(), getWidth() };
        double[] cornerY = new double[] { 0d, getHeight(), getHeight(), 0d };
        int[] cursors = new int[] { Cursor.NW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR };
        for (int i = 0; i < 4; i++) {
            Rectangle corner = new Rectangle((int) (cornerX[i] - fiveX), (int) (cornerY[i] - fiveY), (int) (2d * fiveX), (int) (2d * fiveY));
            Shape handle = t.getTransform().createTransformedShape(corner);
            if (handle.contains(point)) {
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
	            e.getPoint();
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
                    start = e.getPoint();
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	            } else {
                    selected = clicked;
                    move = selected;
                    ifs.deleteTransform(move);
                    start = e.getPoint();
                    setCursor(new Cursor(Cursor.MOVE_CURSOR));
	            }
	        } else  {
	            start = e.getPoint();
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
            } else if (selected != null  && start != null && end != null) {
                ifs.addTransform(selected);
            }
	        start = null;
	        end = null;
	        resize = null;
	        move = null;
	        rotate = null;
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            bus.post(ifs);
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
	        end = e.getPoint();
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
                int dx = end.x - start.x;
                int dy = end.y - start.y;
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
}
