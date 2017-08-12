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
package iterator.view;

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
import java.awt.Shape;
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
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Config.Render;
import iterator.util.Subscriber;

/**
 * Rendered IFS viewer.
 */
public class Viewer extends JPanel implements ActionListener, KeyListener, ComponentListener, MouseInputListener, Printable, Subscriber, Runnable {
    /** serialVersionUID */
    private static final long serialVersionUID = -3294847597249688714L;

    private final Explorer controller;

    private IFS ifs;
    private BufferedImage image;
    private int top[];
    private Timer timer;
    private double points[] = new double[4];
    private AtomicLong count = new AtomicLong(0);
    private AtomicBoolean running = new AtomicBoolean(false);
    private Random random = new Random();
    private float scale = 1.0f;
    private Point2D centre;
    private Rectangle zoom;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private boolean overlay = false, info = false;

    public Viewer(EventBus bus, Explorer controller) {
        super();

        this.controller = controller;

        timer = new Timer(100, this);
        timer.setCoalesce(true);
        timer.setInitialDelay(0);

        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);

        bus.register(this);
    }

    public BufferedImage getImage() { return image; }

    public long getCount() { return count.get(); }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
        if (!running.get()) {
            overlay = false;
            info = false;
            reset();
            rescale();
        }
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension size) {
        centre = new Point2D.Double(getWidth() / 2d, getHeight() / 2d);
        reset();
    }

    /** @see javax.swing.JComponent#paintComponent(Graphics) */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (image != null) {
            g.drawImage(image, new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        }

        if (zoom != null) {
            g.setPaint(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5f, 5f }, 0f));
            g.draw(zoom);
        }

        if (overlay) {
            for (Transform t : ifs) {
                paintTransform(t, g);
            }
        }

        if (info) {
            g.setPaint(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK);
            Font font = new Font("Calibri", Font.PLAIN, 20);
            FontRenderContext frc = g.getFontRenderContext();

            TextLayout scaleText = new TextLayout(String.format("%.1fx (%.3f, %.3f)", scale, centre.getX() / getWidth(), centre.getY() / getHeight()), font, frc);
            scaleText.draw(g, 10f, getHeight() - 10f);
            TextLayout countText = new TextLayout(String.format("%,dK", count.get()).replaceAll("[^0-9K]", " "), font, frc);
            countText.draw(g, getWidth() - 10f - (float) countText.getBounds().getWidth(), getHeight() - 10f);
        }

        if (controller.isDebug()) {
            paintGrid((Graphics2D) g.create());
        }

        g.dispose();
    }

    public void paintTransform(Transform t, Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        // Transform unit square to view space
        int x = (int) (-centre.getX() * scale) + (getWidth() / 2);
        int y = (int) (-centre.getY() * scale) + (getHeight() / 2);
        Rectangle unit = new Rectangle(getSize());
        Shape rect = t.getTransform().createTransformedShape(unit);
        AffineTransform view = AffineTransform.getTranslateInstance(x, y);
        view.scale(scale, scale);
        rect = view.createTransformedShape(rect);

        // Draw the outline
        g.setPaint(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.draw(rect);
        g.dispose();
    }

    public void paintGrid(Graphics2D g) {
        Color red = new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), controller.getRender() == Render.MEASURE ? 64 : 16);
        g.setPaint(red);
        g.setStroke(new BasicStroke(1f));

        g.drawLine((int) centre.getX() - 5, (int) centre.getY(), (int) centre.getX() + 5, (int) centre.getY());
        g.drawLine((int) centre.getX(), (int) centre.getY() - 5, (int) centre.getX(), (int) centre.getY() + 5);

        int max = controller.getMaxGrid();
        for (int x = 0; x < getWidth(); x += max) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += max) {
            g.drawLine(0, y, getWidth(), y);
        }
        g.dispose();
    }

    public void rescale(float scale, Point2D centre) {
        this.scale = scale;
        this.centre = centre;
    }

    public void rescale() {
        rescale(1f, new Point2D.Double(getWidth() / 2d, getHeight() / 2d));
    }

    public void reset() {
        if (getWidth() <= 0 && getHeight() <= 0) return;

        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        if (isVisible()) {
            g.setColor(controller.getRender() == Render.MEASURE ? Color.BLACK : Color.WHITE);
        } else {
            g.setColor(new Color(1f, 1f, 1f, 0f));
        }
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();

        points[0] = random.nextInt(getWidth());
        points[1] = random.nextInt(getHeight());
        points[2] = random.nextInt(getWidth());
        points[3] = random.nextInt(getHeight());

        top = new int[getWidth() * getHeight()];

        count.set(0l);
    }

    public void save(File file) {
        try {
            ImageIO.write(getImage(), "png", file);

            controller.debug("File %s: %d transforms: %.1fx scale at (%.2f, %.2f) with %,dK iterations",
                    file.getName(), ifs.size(), scale, centre.getX(), centre.getY(), count.get());
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    /** @see java.awt.print.Printable#print(Graphics, PageFormat, int) */
    @Override
    public int print(Graphics graphics, PageFormat pf, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;

        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(pf.getImageableX(), pf.getImageableY());
        double scale = pf.getImageableWidth() / (double) getWidth();
        if ((scale * getHeight()) > pf.getImageableHeight()) {
            scale = pf.getImageableHeight() / (double) getHeight();
        }
        g.scale(scale, scale);
        printAll(g);

        return PAGE_EXISTS;
    }

    public void iterate(int k, float scale, Point2D centre) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));

        List<Transform> transforms = controller.getEditor().getTransforms();
        double weight = controller.getEditor().getWeight(transforms);

        int n = transforms.size();
        int r = isVisible() ? 1 : 2;
        int a = isVisible() ? 8 : (int) Math.min(128d, Math.pow(n,  1.2) * 16d);
        for (int i = 0; i < k; i++) {
            if (i % 1000 == 0) count.incrementAndGet();
            int j = random.nextInt(transforms.size());
            Transform t = transforms.get(j);
            if (t.getDeterminant() < random.nextDouble() * weight) continue;

            Point2D old = new Point2D.Double(points[2], points[3]);
            t.getTransform().transform(points, 0, points, 0, 2);

            if (isVisible() && count.get() < 10) continue; // discard first 10K points

            int x = (int) ((points[0] - centre.getX()) * scale) + (getWidth() / 2);
            int y = (int) ((points[1] - centre.getY()) * scale) + (getHeight() / 2);
            if (x >= 0 && y >= 0 && x < getWidth() && y < getWidth()) {
                int p = x + y * getWidth();

                if (controller.getRender() == Render.TOP) {
                    if (j > top[p]) top[p] = j;
                }

                Rectangle rect = new Rectangle(x, y, r, r);

                // Choose the colour based on the display mode
                Color color = Color.BLACK;
                if (controller.isColour()) {
                    if (controller.isIFSColour()) {
                        color = Color.getHSBColor((float) old.getX() / getWidth(), (float) old.getY() / getHeight(), 0.8f);
                    } else if (controller.hasPalette()) {
                        if (controller.isStealing()) {
                            color = controller.getPixel(old.getX(), old.getY());
                        } else {
                            if (controller.getRender() == Render.TOP) {
                                color = Iterables.get(controller.getColours(), top[p] % controller.getPaletteSize());
                            } else {
                                color = Iterables.get(controller.getColours(), j % controller.getPaletteSize());
                            }
                        }
                    } else {
                        if (controller.getRender() == Render.TOP) {
                            color = Color.getHSBColor((float) top[p] / (float) transforms.size(), 0.8f, 0.8f);
                        } else {
                            color = Color.getHSBColor((float) j / (float) transforms.size(), 0.8f, 0.8f);
                        }
                    }
                }

                // Set the paint colour according to the rendering mode
                if (controller.getRender() == Render.IFS) {
                    g.setPaint(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
                } else if (controller.getRender() == Render.MEASURE) {
                    if (top[p] == 0) {
                        color = new Color(color.getRed(), color.getGreen(), color.getBlue());
                    } else {
                        color = new Color(top[p]);
                        float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                        if (hsb[2] < 0.66f) {
                            color = color.brighter();
                        }
                    }
                    top[p] = color.getRGB();
                    g.setPaint(new Color(color.getRed(), color.getGreen(), color.getBlue(), isVisible() ? 16 : 128));
                } else {
                    g.setPaint(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
                }

                g.fill(rect);
            }
        }

        g.dispose();
    }

    /**
     * Invoked when the timer fires, to refresh the image when rendering.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    /**
     * Called as a task to render the IFS.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        do {
            iterate(25_000, scale, centre);
        } while (running.get());
    }

    public void start() {
        timer.start();
        if (running.compareAndSet(false, true)) {
            for (int i = 0; i < controller.getThreads(); i++) {
                executor.submit(this);
            }
        }
    }

    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
        running.compareAndSet(true, false);
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (isVisible()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    if (running.get()) {
                        stop();
                    } else {
                        start();
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    if (zoom != null) {
                        zoom = null;
                        repaint();
                    }
                    break;
                case KeyEvent.VK_MINUS:
                    stop();
                    rescale(scale / 2f, centre);
                    reset();
                    start();
                    break;
                case KeyEvent.VK_EQUALS:
                    stop();
                    if (e.isShiftDown()) {
                        rescale(scale * 2f, centre);
                    } else {
                        rescale();
                    }
                    reset();
                    start();
                    break;
                case KeyEvent.VK_I:
                    info = !info;
                    repaint();
                    break;
                case KeyEvent.VK_O:
                    overlay = !overlay;
                    repaint();
                    break;
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
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
                stop();
                // Calculate new centre point and scale
                Point2D origin = new Point2D.Double((centre.getX() * scale) - (getWidth() / 2d), (centre.getY() * scale) - (getHeight() / 2d));
                Point2D updated = new Point2D.Double((zoom.x + (zoom.width / 2d) + origin.getX()) / scale, (zoom.y + (zoom.height / 2d) + origin.getY()) / scale);
                if (zoom.width == 0 && zoom.height == 0) {
                    rescale(scale * 2f, updated);
                } else {
                    rescale(scale * ((float) getWidth() / (float) zoom.width), updated);
                }

                controller.debug("Zoom: %.1fx scale, centre (%.1f, %.1f) via click at (%d, %d)",
                        scale, centre.getX(), centre.getY(), (int) (zoom.x + (zoom.width / 2d)), (int) (zoom.y + (zoom.height / 2d)));

                zoom = null;
                reset();
                start();
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

    /** @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent) */
    @Override
    public void componentResized(ComponentEvent e) {
    }

    /** @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent) */
    @Override
    public void componentMoved(ComponentEvent e) {
    }

    /** @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent) */
    @Override
    public void componentShown(ComponentEvent e) {
        reset();
    }

    /** @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent) */
    @Override
    public void componentHidden(ComponentEvent e) {
       stop();
    }
}
