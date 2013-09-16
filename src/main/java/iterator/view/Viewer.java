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
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Subscriber;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Rendered IFS viewer.
 */
public class Viewer extends JPanel implements ActionListener, KeyListener, ComponentListener, MouseInputListener, Subscriber {
    /** serialVersionUID */
    private static final long serialVersionUID = -3294847597249688714L;

    private final Explorer controller;

    private IFS ifs;
    private BufferedImage image;
    private Timer timer;
    private double x, y;
    private long count;
    private Random random = new Random();
    private float scale = 1.0f;
    private Point2D centre;
    private Rectangle zoom;

    public Viewer(EventBus bus, Explorer controller) {
        super();

        this.controller = controller;

        timer = new Timer(1, this);
        timer.setCoalesce(true);
        timer.setInitialDelay(0);

        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);

        bus.register(this);
    }

    public BufferedImage getImage() { return image; }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
        reset();
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension size) {
        reset();
        centre = new Point2D.Double(getWidth() / 2d, getHeight() / 2d);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);

        if (zoom != null) {
            g.setPaint(Color.BLACK);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5f, 5f }, 0f));
            g.draw(zoom);
        }

        if (controller.isDebug()) {
            Color red = new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), 128);
            g.setPaint(red);

            g.setStroke(new BasicStroke(2f));
            g.drawLine((int) centre.getX() - 5, (int) centre.getY(), (int) centre.getX() + 5, (int) centre.getY());
            g.drawLine((int) centre.getX(), (int) centre.getY() - 5, (int) centre.getX(), (int) centre.getY() + 5);

            Font font = new Font("Calibri", Font.BOLD, 20);
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout scaleText = new TextLayout(String.format("%.1fx", scale), font, frc);
            TextLayout countText = new TextLayout(String.format("%,dK", count).replaceAll("[^0-9K]", " "), font, frc);
            scaleText.draw(g, getWidth() - 10f - (float) scaleText.getBounds().getWidth(), getHeight() - 30f);
            countText.draw(g, getWidth() - 10f - (float) countText.getBounds().getWidth(), getHeight() - 10f);

            paintGrid(g);
        }

        g.dispose();
    }

    public void paintGrid(Graphics2D g) {
        Color grid = new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), 16);
        g.setPaint(grid);
        g.setStroke(new BasicStroke(1f));
        int max = controller.getMaxGrid();
        for (int x = 0; x < getWidth(); x += max) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += max) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    public void reset() {
        if (getWidth() <= 0 && getHeight() <= 0) return;
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(isVisible() ? Color.WHITE : new Color(1f, 1f, 1f, 0f));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
        x = random.nextInt(getWidth());
        y = random.nextInt(getHeight());
        scale = 1.0f;
        centre = new Point2D.Double(getWidth() / 2d, getHeight() / 2d);
        count = 0l;
    }

    public void save(File file) {
        try {
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public void iterate(int k) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));

        Transform selected = controller.getEditor().getSelected();
        Transform ants = null;
        Point start = controller.getEditor().getStart();
        Point end = controller.getEditor().getEnd();
        if (selected == null && start != null && end != null) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int w = Math.max(start.x, end.x) - x;
            int h = Math.max(start.y, end.y) - y;

            int grid = controller.getMinGrid();
            w = Math.max(grid, w);
            h = Math.max(grid, h);

            ants = new Transform(Integer.MIN_VALUE, 0, getSize());
            ants.x = x;
            ants.y = y;
            ants.w = w;
            ants.h = h;
        }

        if (ifs.contains(selected)) { selected = null; }
        List<Transform> transforms = Lists.newArrayList(Iterables.concat(ifs, Optional.fromNullable(selected).asSet(), Optional.fromNullable(ants).asSet()));
        Collections.sort(transforms, IFS.IDENTITY);
        double weight = ifs.getWeight() + (selected != null ? selected.getDeterminant() : 0d) + (ants != null ? ants.getDeterminant() : 0d);

        int n = transforms.size();
        int r = isVisible() ? 1 : 2;
        int a = isVisible() ? 8 : (int) Math.min(128d, Math.pow(n,  1.6) * 8d);
        for (int i = 0; i < k; i++) {
            int j = random.nextInt(transforms.size());
            Transform t = transforms.get(j);
            if (t.getDeterminant() < random.nextDouble() * weight) continue;
            Color c = Color.BLACK;
            if (controller.isColour()) {
                if (controller.hasPalette()) {
                    c = controller.getColours().get(j % controller.getPaletteSize());
                } else {
                    c = Color.getHSBColor((float) j / (float) transforms.size(), 0.8f, 0.8f);
                }
            }
            g.setPaint(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
            double dst[] = t.applyTransform(x, y);
            x = dst[0]; y = dst[1];
            double px = ((x - centre.getX()) * scale) + (getWidth() / 2d);
            double py = ((y - centre.getY()) * scale) + (getHeight() / 2d);
            if (px > 0d && py > 0d && px < getWidth() && py < getWidth()) {
                Rectangle rect = new Rectangle((int) Math.floor(px + 0.5d), (int) Math.floor(py + 0.5d), r, r);
                g.fill(rect);
            }
            if (i % 1000 == 0) count++;
        }

        g.dispose();
    }

    /** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ifs != null && !ifs.isEmpty() && image != null) {
            iterate(25_000);
            repaint();
        }
    }

    public void start() {
        reset();
        timer.start();
    }

    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && isVisible()) {
            if (timer.isRunning()) {
                timer.stop();
            } else {
                timer.start();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_Z && isVisible()) {
            float old = scale;
            Point2D last = centre;

            timer.stop();
            reset();
            if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == KeyEvent.SHIFT_DOWN_MASK) {
                scale = old / 2.0f;
            } else {
                scale = old * 2.0f;
            }
            centre = last;
            timer.start();
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            zoom = new Rectangle(e.getX(), e.getY(), 0, 0);
            repaint();
        }
    }

    /** @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent) */
    @Override public void mouseDragged(MouseEvent e) {
        if (zoom != null) {
            Point one = e.getPoint();
            Point two = new Point(zoom.x, zoom.y);
            int x1 = Math.min(one.x, two.x);
            int x2 = Math.max(one.x, two.x);
            int y1 = Math.min(one.y, two.y);
            int y2 = Math.max(one.y, two.y);
            int side = Math.max(controller.getMinGrid(), Math.min(x2 - x1,  y2 - y1));
            zoom = new Rectangle(x1, y1, side, side);
            repaint();
        }
    }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (zoom != null) {
                float old = scale;
                Point2D last = centre;

                timer.stop();
                reset();

                // Calculate new centre point and scale
                Point2D origin = new Point2D.Double((last.getX() * old) - (getWidth() / 2d), (last.getY() * old) - (getHeight() / 2d));
                if (zoom.width == 0 && zoom.height == 0) {
                    scale = old * 2f;
                } else {
                    scale = old * ((float) getWidth() / (float) zoom.width);
                }
                centre = new Point2D.Double((zoom.x + (zoom.width / 2d) + origin.getX()) / old, (zoom.y + (zoom.height / 2d) + origin.getY()) / old);

                zoom = null;
                timer.start();
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

    /** @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent) */
    @Override
    public void mouseMoved(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
       stop();
    }
}
