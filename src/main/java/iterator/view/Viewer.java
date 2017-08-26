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

import static iterator.util.Messages.MENU_VIEWER_GRID;
import static iterator.util.Messages.MENU_VIEWER_INFO;
import static iterator.util.Messages.MENU_VIEWER_OVERLAY;
import static iterator.util.Messages.MENU_VIEWER_PAUSE;
import static iterator.util.Messages.MENU_VIEWER_RESUME;
import static iterator.util.Messages.MENU_VIEWER_ZOOM;

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
import java.awt.SecondaryLoop;
import java.awt.Shape;
import java.awt.Toolkit;
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
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import iterator.Explorer;
import iterator.Utils;
import iterator.dialog.Zoom;
import iterator.model.Function;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.Config.Render;
import iterator.util.Messages;
import iterator.util.Subscriber;

/**
 * Rendered IFS viewer.
 */
public class Viewer extends JPanel implements ActionListener, KeyListener, ComponentListener, MouseInputListener, Printable, Subscriber, Runnable {
    /** serialVersionUID */
    private static final long serialVersionUID = -3294847597249688714L;

    private final Explorer controller;
    private final EventBus bus;
    private final Messages messages;

    private IFS ifs;
    private BufferedImage image;
    private int top[], density[];
    private double rgb[];
    private int max;
    private Timer timer;
    private double points[] = new double[4];
    private AtomicLong count = new AtomicLong(0);
    private AtomicInteger task = new AtomicInteger(0);
    private AtomicBoolean running = new AtomicBoolean(false);
    private Random random = new Random();
    private float scale = 1.0f;
    private Point2D centre;
    private Rectangle zoom;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private boolean overlay, info, grid;
    private JPopupMenu viewer;
    private Zoom properties;
    private JCheckBoxMenuItem showGrid, showOverlay, showInfo;
    private JMenuItem pause, resume;
    private SecondaryLoop loop;

    public Viewer(Explorer controller) {
        super();

        this.controller = controller;
        this.bus = controller.getEventBus();
        this.messages = controller.getMessages();

        timer = new Timer(200, this);
        timer.setCoalesce(true);
        timer.setInitialDelay(0);

        properties = new Zoom(controller);
        viewer = new JPopupMenu();
        viewer.add(new AbstractAction(messages.getText(MENU_VIEWER_ZOOM)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                properties.showDialog();
            }
        });
        pause = new JMenuItem(new AbstractAction(messages.getText(MENU_VIEWER_PAUSE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });
        viewer.add(pause);
        resume = new JMenuItem(new AbstractAction(messages.getText(MENU_VIEWER_RESUME)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });
        viewer.add(resume);
        JMenuItem separator = new JMenuItem("-");
        separator.setEnabled(false);
        viewer.add(separator);
        showGrid = new JCheckBoxMenuItem(new AbstractAction(messages.getText(MENU_VIEWER_GRID)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setGrid(!grid);
            }
        });
        viewer.add(showGrid);
        showOverlay = new JCheckBoxMenuItem(new AbstractAction(messages.getText(MENU_VIEWER_OVERLAY)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOverlay(!overlay);
            }
        });
        viewer.add(showOverlay);
        showInfo = new JCheckBoxMenuItem(new AbstractAction(messages.getText(MENU_VIEWER_INFO)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInfo(!info);
            }
        });
        viewer.add(showInfo);
        add(viewer);

        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);

        bus.register(this);
    }

    public BufferedImage getImage() { return image; }

    public long getCount() { return count.get(); }

    public Point2D getCentre() { return centre; }

    public float getScale() { return scale; }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
        if (!running.get()) {
            setOverlay(controller.isDebug());
            setInfo(controller.isDebug());
            setGrid(controller.isDebug());
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

        try {
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
                for (Transform t : ifs.getTransforms()) {
                    paintTransform(t, g);
                }
                for (Reflection r : ifs.getReflections()) {
                    paintReflection(r, g);
                }
            }

            if (info) {
                g.setPaint(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK);
                Font font = Utils.calibri(Font.PLAIN, 20);
                FontRenderContext frc = g.getFontRenderContext();

                TextLayout scaleText = new TextLayout(String.format("%.1fx (%.3f, %.3f) %s/%s", scale, centre.getX() / getWidth(), centre.getY() / getHeight(), controller.getMode(), controller.getRender()), font, frc);
                scaleText.draw(g, 10f, getHeight() - 10f);
                TextLayout countText = new TextLayout(String.format("%,dK", count.get()).replaceAll("[^0-9K]", " "), font, frc);
                countText.draw(g, getWidth() - 10f - (float) countText.getBounds().getWidth(), getHeight() - 10f);
            }

            if (grid) {
                paintGrid(g);
            }
        } catch (Exception e) {
            controller.error(e, "Failure painting IFS");
        } finally {
            g.dispose();
        }
    }

    public void paintTransform(Transform t, Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            // Transform unit square to view space
            double x = (getWidth() / 2) - (centre.getX() * scale);
            double y = (getHeight() / 2) - (centre.getY() * scale);
            Rectangle unit = new Rectangle(getSize());
            Shape rect = t.getTransform().createTransformedShape(unit);
            AffineTransform view = AffineTransform.getTranslateInstance(x, y);
            view.scale(scale, scale);
            rect = view.createTransformedShape(rect);

            // Draw the outline
            Color c = Utils.alpha(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK, 64);
            g.setPaint(c);
            g.setStroke(new BasicStroke(2f));
            g.draw(rect);
        } catch (Exception e) {
            controller.error(e, "Failure painting transform");
        } finally {
            g.dispose();
        }
    }

    public void paintReflection(Reflection r, Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            // Transform unit square to view space
            double x = (getWidth() / 2) - (centre.getX() * scale);
            double y = (getHeight() / 2) - (centre.getY() * scale);
            AffineTransform view = AffineTransform.getTranslateInstance(x, y);
            view.scale(scale, scale);

            // Draw the line
            Color c = Utils.alpha(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK, 64);
            g.setPaint(c);
            g.setStroke(new BasicStroke(2f));
            Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO);
            if ((r.r < Math.toRadians(0.1d) && r.r > Math.toRadians(-0.1d)) ||
                    (r.r < Math.toRadians(180.1d) && r.r > Math.toRadians(179.9d))) {
                line.moveTo(r.x, -1d * (getHeight() / scale));
                line.lineTo(r.x, (getHeight() / scale));
            } else {
                line.moveTo(r.x - getWidth() - (getWidth() / scale), r.y - (getWidth() / Math.tan(r.r)) - (getWidth() / (Math.tan(r.r) * scale)));
                line.lineTo(r.x + getWidth() + (getWidth() / scale), r.y + (getWidth() / Math.tan(r.r)) + (getWidth() / (Math.tan(r.r) * scale)));
            }
            g.draw(view.createTransformedShape(line));
        } catch (Exception e) {
            controller.error(e, "Failure painting reflection");
        } finally {
            g.dispose();
        }
    }

    public void paintGrid(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            // Set colour and width
            Color c = Utils.alpha(controller.getRender() == Render.MEASURE ? Color.WHITE : Color.BLACK, 64);
            g.setPaint(c);
            g.setStroke(new BasicStroke(1f));

            // Transform unit square to view space
            double x0 = (getWidth() / 2) - (centre.getX() * scale);
            double y0 = (getHeight() / 2) - (centre.getY() * scale);
            double spacing = controller.getMaxGrid() / scale;
            AffineTransform view = AffineTransform.getTranslateInstance(x0, y0);
            view.scale(scale, scale);

            // Draw grid lines
            double cx = Math.abs(centre.getX()) + getWidth() / scale;
            double cy = Math.abs(centre.getY()) + getHeight() / scale;
            double ox = Math.max(getWidth() / scale, cx);
            double oy = Math.max(getHeight() / scale, cy);
            for (double x = -ox; x < ox + getWidth(); x += spacing) {
                Line2D line = new Line2D.Double(x, -oy, x, oy + getHeight());
                g.draw(view.createTransformedShape(line));
            }
            for (double y = -oy; y < oy + getWidth(); y += spacing) {
                Line2D line = new Line2D.Double(-ox, y, ox + getWidth(), y);
                g.draw(view.createTransformedShape(line));
            }
        } catch (Exception e) {
            controller.error(e, "Failure painting grid");
        } finally {
            g.dispose();
        }
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
        try {
            if (isVisible()) {
                g.setColor(controller.getRender() == Render.MEASURE ? Color.BLACK : Color.WHITE);
            } else {
                g.setColor(new Color(1f, 1f, 1f, 0f));
            }
            g.fillRect(0, 0, getWidth(), getHeight());
        } catch (Exception e) {
            controller.error(e, "Failure resetting image");
        } finally {
            g.dispose();
        }

        points[0] = random.nextInt(getWidth());
        points[1] = random.nextInt(getHeight());
        points[2] = random.nextInt(getWidth());
        points[3] = random.nextInt(getHeight());

        top = new int[getWidth() * getHeight()];
        density = new int[getWidth() * getHeight()];
        rgb = new double[getWidth() * getHeight()];
        max = 1;

        count.set(0l);
    }

    public void save(File file) {
        try {
            ImageIO.write(getImage(), "png", file);

            controller.debug("File %s: %d transforms/%d reflections: %.1fx scale at (%.2f, %.2f) with %,dK iterations",
                    file.getName(), ifs.getTransforms().size(), ifs.getReflections().size(),
                    scale, centre.getX(), centre.getY(), count.get());
        } catch (IOException e) {
            controller.error(e,  "Error saving image file %s", file.getName());
        }
    }

    /** @see java.awt.print.Printable#print(Graphics, PageFormat, int) */
    @Override
    public int print(Graphics graphics, PageFormat pf, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.translate(pf.getImageableX(), pf.getImageableY());
            double scale = pf.getImageableWidth() / (double) getWidth();
            if ((scale * getHeight()) > pf.getImageableHeight()) {
                scale = pf.getImageableHeight() / (double) getHeight();
            }
            g.scale(scale, scale);
            printAll(g);
        } catch (Exception e) {
            controller.error(e, "Failure printing image");
        } finally {
            g.dispose();
        }

        return PAGE_EXISTS;
    }

    public void iterate(long k, float scale, Point2D centre) {
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));

            List<Transform> transforms = controller.getEditor().getTransforms();
            List<Reflection> reflections = controller.getEditor().getReflections();
            List<Function> functions = ImmutableList.<Function>builder()
                    .addAll(transforms)
                    .addAll(reflections)
                    .build();
            if (functions.isEmpty()) return;

            double weight = Utils.weight(transforms);
            int n = transforms.size();
            int m = reflections.size();
            int s = isVisible() ? 1 : 2;
            int a = isVisible() ? 8 : (int) Math.min(128d, Math.pow(n,  1.2) * 16d);
            int l = getWidth() * getHeight();

            for (long i = 0L; i < k; i++) {
                if (i % 1000L == 0L) {
                    count.incrementAndGet();
                }

                // Skip based on transform weighting
                int j = random.nextInt(functions.size());
                Function f = functions.get(j);
                if ((j < n ? ((Transform) f).getWeight() : weight) < random.nextDouble() * weight * (m + 1d)) {
                    continue;
                }

                // Evaluate the function
                Point2D old = new Point2D.Double(points[2], points[3]);
                f.getTransform().transform(points, 0, points, 0, 2);

                // Discard first 10K points
                if (isVisible() && count.get() < 10) {
                    continue;
                }

                int x = (int) ((points[0] - centre.getX()) * scale) + (getWidth() / 2);
                int y = (int) ((points[1] - centre.getY()) * scale) + (getHeight() / 2);
                if (x >= 0 && y >= 0 && x < getWidth() && y < getWidth()) {
                    int p = x + y * getWidth();

                    if (controller.getRender() == Render.TOP) {
                        if (j > top[p]) top[p] = j;
                    }

                    if (controller.getRender().isDensity()) {
                        density[p]++;
                        if (controller.getRender() == Render.LOG_DENSITY_BLUR) {
                            density[p]++;
                            density[(p + 1) % l]++;
                            density[(p + getWidth()) % l]++;
                            density[(p + 1 + getWidth()) % l]++;
                        }
                        max = Math.max(max, density[p]);
                    }

                    Rectangle rect = new Rectangle(x, y, s, s);

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
                                color = Color.getHSBColor((float) top[p] / (float) ifs.size(), 0.8f, 0.8f);
                            } else {
                                color = Color.getHSBColor((float) j / (float) ifs.size(), 0.8f, 0.8f);
                            }
                        }
                        if (controller.getRender().isDensity()) {
                            if (controller.isIFSColour()) {
                                rgb[p] = (double) color.getRGB() / (double) (2 << 24);
                            } else {
                                rgb[p] = (rgb[p] + (double) color.getRGB() / (double) (2 << 24)) / 2d;
                            }
                        }
                    }

                    // Set the paint colour according to the rendering mode
                    if (controller.getRender() == Render.IFS) {
                        g.setPaint(Utils.alpha(color, 255));
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
                        g.setPaint(Utils.alpha(color, isVisible() ? 16 : 128));
                    } else {
                        g.setPaint(Utils.alpha(color, a));
                    }

                    // Don't paint when using density rendering in viewer
                    if (!(controller.getRender().isDensity() && isVisible())) {
                        g.fill(rect);
                    }
                }
            }
        } catch (Exception e) {
            controller.error(e, "Failure iterating IFS");
        } finally {
            g.dispose();
        }
    }

    public void plotDensity() {
        Graphics2D g = image.createGraphics();

        if (isVisible()) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(new Color(1f, 1f, 1f, 0f));
        }
        g.fillRect(0, 0, getWidth(), getHeight());

        try {
            int r = (isVisible() ? 1 : 2) * (controller.getRender() == Render.LOG_DENSITY_BLUR ? 1 : 2);
            boolean log = (controller.getRender() == Render.LOG_DENSITY || controller.getRender() == Render.LOG_DENSITY_BLUR);
            for (int x = 0; x < getWidth(); x++) {
                for (int y = 0; y < getHeight(); y++) {
                    int p = x + y * getWidth();
                    if (density[p] > 0) {
                        float ratio = log ? (float) (Math.log(density[p]) / Math.log(max)) : ((float) density[p] / (float) max);
                        float alpha = (float) (Math.log(density[p]) / density[p]);
                        float gray = 1f - Math.min(1f, ratio);
                        if (controller.isColour()) {
                            Color color = new Color((int) (rgb[p] * (2 << 24)));
                            if (controller.getRender() == Render.LOG_DENSITY_FLAME) {
                                g.setPaint(new Color((int) (alpha * color.getRed()), (int) (alpha * color.getGreen()), (int) (alpha * color.getBlue()), (int) ((1f - gray) * 255f)));
                            } else {
                                float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                                g.setPaint(Utils.alpha(new Color(Color.HSBtoRGB(hsb[0], hsb[1], gray)), (int) ((1f - gray) * 255f)));
                            }
                        } else {
                            g.setPaint(new Color(gray, gray, gray, 1f - gray));
                        }
                        Rectangle rect = new Rectangle(x, y, r, r);
                        g.fill(rect);
                    }
                }
            }
        } catch (Exception e) {
            controller.error(e, "Failure plotting density map");
        } finally {
            g.dispose();
        }
    }

    /**
     * Invoked when the timer fires, to refresh the image when rendering.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller.getRender().isDensity()) {
            plotDensity();
        }
        repaint();
    }

    /**
     * Called as a task to render the IFS.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        int id = task.incrementAndGet();
        controller.debug("Started task %d", id);;
        do {
            iterate(controller.getIterations(), scale, centre);
        } while (running.get());
        controller.debug("Stopped task %d", id);;
    }

    public void start() {
        timer.start();
        if (running.compareAndSet(false, true)) {
            loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            for (int i = 0; i < controller.getThreads(); i++) {
                executor.submit(this);
            }
            pause.setEnabled(true);
            resume.setEnabled(false);
            loop.enter();
        }
    }

    public boolean stop() {
        boolean status = running.compareAndSet(true, false);
        if (timer.isRunning()) {
            timer.stop();
        }
        if (status) {
            loop.exit();
            pause.setEnabled(false);
            resume.setEnabled(true);
        }
        try {
            boolean stopped = executor.awaitTermination(1, TimeUnit.SECONDS);
            controller.debug("Executor %s all tasks", stopped ? "stopped" : "failed to stop");;
        } catch (InterruptedException e) {
            Thread.interrupted();
            controller.debug("Executor interrupted stopping tasks");
        }
        return status;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

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
                    setInfo(!info);
                    break;
                case KeyEvent.VK_O:
                    setOverlay(!overlay);
                    break;
                case KeyEvent.VK_G:
                    setGrid(!grid);
                    break;
            }
        }
    }

    private void setInfo(boolean state) {
        info = state;
        showInfo.setSelected(state);
        repaint();
    }

    private void setOverlay(boolean state) {
        overlay = state;
        showOverlay.setSelected(state);
        repaint();
    }

    private void setGrid(boolean state) {
        grid = state;
        showGrid.setSelected(state);
        repaint();
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            zoom = new Rectangle(e.getX(), e.getY(), 0, 0);
            repaint();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            viewer.show(e.getComponent(), e.getX(), e.getY());
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
            int side = Math.min(x2 - x1,  y2 - y1);
            if (side < controller.getSnapGrid()) {
                side = 0;
            }
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
                if (zoom.width < 2 *  controller.getSnapGrid()) {
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
    public void mouseEntered(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) { }

    /** @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent) */
    @Override
    public void mouseMoved(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) { }

    /** @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent) */
    @Override
    public void componentResized(ComponentEvent e) { }

    /** @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent) */
    @Override
    public void componentMoved(ComponentEvent e) { }

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
