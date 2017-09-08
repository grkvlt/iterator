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

import static iterator.Utils.DASHED_LINE_1;
import static iterator.Utils.DASHED_LINE_2;
import static iterator.Utils.DOTTED_LINE_2;
import static iterator.Utils.NEWLINE;
import static iterator.Utils.RGB24;
import static iterator.Utils.SOLID_LINE_2;
import static iterator.Utils.alpha;
import static iterator.Utils.calibri;
import static iterator.Utils.checkBoxItem;
import static iterator.Utils.context;
import static iterator.Utils.copyPoint;
import static iterator.Utils.menuItem;
import static iterator.Utils.unity;
import static iterator.Utils.weight;
import static iterator.util.Messages.MENU_VIEWER_GRID;
import static iterator.util.Messages.MENU_VIEWER_INFO;
import static iterator.util.Messages.MENU_VIEWER_OVERLAY;
import static iterator.util.Messages.MENU_VIEWER_PAUSE;
import static iterator.util.Messages.MENU_VIEWER_RESUME;
import static iterator.util.Messages.MENU_VIEWER_ZOOM;

import java.awt.AlphaComposite;
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
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.math.LongMath;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import iterator.Explorer;
import iterator.dialog.Zoom;
import iterator.model.Function;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
import iterator.util.Dialog;
import iterator.util.Formatter;
import iterator.util.Formatter.DoubleFormatter;
import iterator.util.Formatter.FloatFormatter;
import iterator.util.Messages;
import iterator.util.Subscriber;

/**
 * Rendered IFS viewer.
 */
public class Viewer extends JPanel implements ActionListener, KeyListener, MouseInputListener, Printable, Subscriber, Runnable, ThreadFactory {

    private static enum Task { ITERATE, PLOT_DENSITY }

    private final Explorer controller;
    private final EventBus bus;
    private final Messages messages;

    private IFS ifs;
    private AtomicReference<BufferedImage> image = Atomics.newReference();
    private int top[];
    private long density[];
    private double colour[];
    private long max;
    private Timer timer;
    private Point2D points[] = new Point2D[2];
    private AtomicLong count = new AtomicLong(0l);
    private AtomicInteger task = new AtomicInteger(0);
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicLong token = new AtomicLong(0l);
    private Random random = new Random();
    private float scale = 1.0f;
    private Point2D centre;
    private Dimension size;
    private Rectangle zoom;
    private ThreadGroup group = new ThreadGroup("iterator");
    private ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(this));
    private Multimap<Task, Future<?>> tasks = Multimaps.synchronizedListMultimap(Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList));
    private ConcurrentMap<Future<?>, AtomicBoolean> state = Maps.newConcurrentMap();
    private boolean overlay, info, grid;
    private JPopupMenu viewer;
    private Zoom properties;
    private JCheckBoxMenuItem showGrid, showOverlay, showInfo;
    private JMenuItem pause, resume;
    private SecondaryLoop loop;

    // Listener task to clean up task state collections
    private Runnable cleaner = () -> {
            synchronized (tasks) {
                List<Future<?>> done = tasks.values()
                        .stream()
                        .filter(f -> f.isDone())
                        .collect(Collectors.toList());
                tasks.values().removeAll(done);
                state.keySet().removeAll(done);
            }
            if (tasks.isEmpty() && isRunning()) {
                stop();
            }
            repaint();
        };

    public Viewer(Explorer controller) {
        super();

        this.controller = controller;
        this.bus = controller.getEventBus();
        this.messages = controller.getMessages();

        timer = new Timer(50, this);
        timer.setCoalesce(true);

        size = getSize();

        properties = new Zoom(controller);
        viewer = new JPopupMenu();
        viewer.add(menuItem(messages.getText(MENU_VIEWER_ZOOM), e -> {
            Dialog.show(() -> properties);
        }));
        pause = menuItem(messages.getText(MENU_VIEWER_PAUSE), e -> {
            stop();
        });
        viewer.add(pause);
        resume = menuItem(messages.getText(MENU_VIEWER_RESUME), e -> {
            start();
        });
        viewer.add(resume);
        JMenuItem separator = new JMenuItem("-");
        separator.setEnabled(false);
        viewer.add(separator);
        showGrid = checkBoxItem(messages.getText(MENU_VIEWER_GRID), e -> {
            setGrid(!grid);
        });
        viewer.add(showGrid);
        showOverlay = checkBoxItem(messages.getText(MENU_VIEWER_OVERLAY), e -> {
            setOverlay(!overlay);
        });
        viewer.add(showOverlay);
        showInfo = checkBoxItem(messages.getText(MENU_VIEWER_INFO), e -> {
            setInfo(!info);
        });
        viewer.add(showInfo);
        add(viewer);

        addMouseListener(this);
        addMouseMotionListener(this);

        bus.register(this);
    }

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
        stop();
        this.size = size;
        this.centre = new Point2D.Double(size.getWidth() / 2d, size.getHeight() / 2d);
        reset();
        if (isVisible()) {
            start();
        }
    }

    /** @see javax.swing.JComponent#paintComponent(Graphics) */
    @Override
    protected void paintComponent(Graphics graphics) {
        context(controller, graphics, g -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (getImage() != null) {
                g.drawImage(getImage(), new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
            }

            if (zoom != null) {
                g.setPaint(controller.getRender().getForeground());
                g.setStroke(DOTTED_LINE_2);
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
                FloatFormatter one = Formatter.floats(1);
                DoubleFormatter four = Formatter.doubles(4);
                String scaleText = String.format("%sx (%s,%s) %s/%s %s %s() y%s [%s/%d]",
                        one.toString(scale),
                        four.toString(centre.getX() / size.getWidth()),
                        four.toString(centre.getY() / size.getHeight()),
                        controller.getMode(), controller.getRender(),
                        controller.hasPalette() ? controller.getPaletteFile() : (controller.isColour() ? "hsb" : "black"),
                        controller.getFinal().getShortName(),
                        one.toString(controller.getGamma()),
                        tasks.isEmpty() ? "-" : Integer.toString(tasks.size()), controller.getThreads());
                String countText = String.format("%,dK", count.get()).replaceAll("[^0-9K+]", " ");

                g.setPaint(controller.getRender().getForeground());
                FontRenderContext frc = g.getFontRenderContext();
                TextLayout scaleLayout = new TextLayout(scaleText, calibri(Font.BOLD, 20), frc);
                scaleLayout.draw(g, 10f, size.height - 10f);
                TextLayout countLayout = new TextLayout(countText, calibri(Font.BOLD | (isRunning() ? Font.PLAIN : Font.ITALIC), 20), frc);
                countLayout.draw(g, size.width - 10f - (float) countLayout.getBounds().getWidth(), size.height - 10f);
            }

            if (grid) {
                paintGrid(g);
            }
        });
    }

    public void paintTransform(Transform t, Graphics2D graphics) {
        context(controller, graphics, g -> {
            // Transform unit square to view space
            double x = (size.getWidth() / 2d) - (centre.getX() * scale);
            double y = (size.getHeight() / 2d) - (centre.getY() * scale);
            Rectangle unit = new Rectangle(getSize());
            Shape rect = t.getTransform().createTransformedShape(unit);
            AffineTransform view = AffineTransform.getTranslateInstance(x, y);
            view.scale(scale, scale);
            rect = view.createTransformedShape(rect);

            // Draw the outline
            Color c = alpha(controller.getRender().getForeground(), 128);
            g.setPaint(c);
            g.setStroke(SOLID_LINE_2);
            g.draw(rect);
            g.setPaint(alpha(Color.GRAY, 8));
            g.fill(rect);
        });
    }

    public void paintReflection(Reflection r, Graphics2D graphics) {
        context(controller, graphics, g -> {
            // Transform unit square to view space
            double x = (size.getWidth() / 2d) - (centre.getX() * scale);
            double y = (size.getHeight() / 2d) - (centre.getY() * scale);
            AffineTransform view = AffineTransform.getTranslateInstance(x, y);
            view.scale(scale, scale);

            // Draw the line
            Color c = alpha(controller.getRender().getForeground(), 128);
            g.setPaint(c);
            g.setStroke(DASHED_LINE_2);
            Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO);
            if ((r.r < Math.toRadians(0.1d) && r.r > Math.toRadians(-0.1d)) ||
                    (r.r < Math.toRadians(180.1d) && r.r > Math.toRadians(179.9d))) {
                line.moveTo(r.x, -1d * (size.getHeight() / scale));
                line.lineTo(r.x, (size.getHeight() / scale));
            } else {
                line.moveTo(r.x - size.getWidth() - (size.getWidth() / scale),
                        r.y - (size.getWidth() / Math.tan(r.r)) - (size.getWidth() / (Math.tan(r.r) * scale)));
                line.lineTo(r.x + size.getWidth() + (size.getWidth() / scale),
                        r.y + (size.getWidth() / Math.tan(r.r)) + (size.getWidth() / (Math.tan(r.r) * scale)));
            }
            g.draw(view.createTransformedShape(line));
        });
    }

    public void paintGrid(Graphics2D graphics) {
        context(controller, graphics, g -> {
            // Set colour and width
            Color c = alpha(controller.getRender().getForeground(), 64);
            g.setPaint(c);
            g.setStroke(DASHED_LINE_1);

            // Transform unit square to view space
            double x0 = (size.getWidth() / 2d) - (centre.getX() * scale);
            double y0 = (size.getHeight() / 2d) - (centre.getY() * scale);
            AffineTransform view = AffineTransform.getTranslateInstance(x0, y0);
            view.scale(scale, scale);

            // Draw grid lines
            Rectangle unit = new Rectangle(getSize());
            double spacing = controller.getMaxGrid() / scale;
            double mx = centre.getX() - (size.getWidth() / 2d / scale);
            double my = centre.getY() - (size.getHeight() / 2d / scale);
            double nx = centre.getX() + (size.getWidth() / 2d / scale);
            double ny = centre.getY() + (size.getHeight() / 2d / scale);
            double rx = Math.IEEEremainder(mx, spacing);
            double ry = Math.IEEEremainder(my, spacing);
            double sx = mx < 0 ? mx - (spacing - rx) : mx - rx;
            double sy = my < 0 ? my - (spacing - ry) : my - ry;
            for (double x = sx; x < nx; x += spacing) {
                Line2D line = new Line2D.Double(x, my, x, ny);
                Shape s = view.createTransformedShape(line);
                if (s.intersects(unit)) {
                    g.draw(s);
                }
            }
            for (double y = sy; y < ny; y += spacing) {
                Line2D line = new Line2D.Double(mx, y, nx, y);
                Shape s = view.createTransformedShape(line);
                if (s.intersects(unit)) {
                    g.draw(s);
                }
            }
        });
    }

    public void rescale(float scale, Point2D centre) {
        this.scale = scale;
        this.centre = centre;
    }

    public void rescale() {
        rescale(1f, new Point2D.Double(size.getWidth() / 2d, size.getHeight() / 2d));
    }

    public void reset() {
        if (size.getWidth() <= 0 && size.getHeight() <= 0) return;

        image.set(newImage(getSize()));

        points[0] = new Point2D.Double((double) random.nextInt(size.width), (double) random.nextInt(size.height));
        points[1] = new Point2D.Double((double) random.nextInt(size.width), (double) random.nextInt(size.height));

        top = new int[size.width * size.height];
        density = new long[size.width * size.height];
        colour = new double[size.width * size.height];
        max = 1;

        count.set(0l);
    }

    public BufferedImage newImage(Dimension size) {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        context(controller, image.getGraphics(), g -> {
            g.setColor(controller.getRender().getBackground());
            g.fillRect(0, 0, size.width, size.height);
        });
        return image;
    }

    public BufferedImage getImage() {
        return image.get();
    }

    /** @see java.awt.print.Printable#print(Graphics, PageFormat, int) */
    @Override
    public int print(Graphics graphics, PageFormat pf, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;

        context(controller, graphics, g -> {
            g.translate(pf.getImageableX(), pf.getImageableY());
            double scale = pf.getImageableWidth() / size.getWidth();
            if ((scale * getHeight()) > pf.getImageableHeight()) {
                scale = pf.getImageableHeight() / size.getHeight();
            }
            g.scale(scale, scale);
            g.setClip(0, 0, size.width, size.height);
            printAll(g);
        });

        return PAGE_EXISTS;
    }

    public void iterate(BufferedImage targetImage, int s, long k, float scale, Point2D centre, Render render, Mode mode, Function function) {
        context(controller, targetImage.getGraphics(), g -> {
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

            double weight = weight(transforms);
            int n = transforms.size();
            int m = reflections.size();
            int l = size.width * size.height;
            float hsb[] = new float[3];
            Rectangle rect = new Rectangle(0, 0, s, s);

            for (long i = 0l; i < k; i++) {
                if (i % 1000l == 0l) {
                    count.incrementAndGet();
                }

                // Skip based on transform weighting
                int j = random.nextInt(functions.size());
                Function f = functions.get(j);
                if ((j < n ? ((Transform) f).getWeight() : weight) < random.nextDouble() * weight * (m + 1d)) {
                    continue;
                }

                Point2D old, current;
                synchronized (points) {
                    old = copyPoint(points[1]);

                    // Evaluate the function twice, first for (x,y) position and then for hue/saturation color space
                    points[0] = f.transform(points[0]);
                    points[1] = f.transform(points[1]);

                    // Final transform function
                    points[0] = function.transform(points[0]);
                    current = copyPoint(points[0]);
                }

                // Discard first 10K points
                if (count.get() < 10) {
                    continue;
                }

                int x = (int) ((current.getX() - centre.getX()) * scale + (size.getWidth() / 2d));
                int y = (int) ((current.getY() - centre.getY()) * scale + (size.getHeight() / 2d));
                if (x >= 0 && y >= 0 && x < size.width && y < size.height) {
                    int p = x + y * size.width;

                    if (render == Render.TOP) {
                        if (j > top[p]) top[p] = j;
                    }

                    // Density estimation histogram
                    if (render.isDensity()) {
                        try {
                            int q = p;
                            density[q] = LongMath.checkedAdd(density[q], 1l);
                            switch (render) {
                                case LOG_DENSITY_BLUR:
                                case LOG_DENSITY_BLUR_INVERSE:
                                    q += 1;
                                    if (q < l && q % size.width != 0) density[q] = LongMath.checkedAdd(density[q], 1l);
                                    q += size.width;
                                    if (q < l && q % size.width != 0) density[q] = LongMath.checkedAdd(density[q], 1l);
                                    q -= 1;
                                    if (q < l && q % size.width != 0) density[q] = LongMath.checkedAdd(density[q], 1l);
                                    break;
                                case LOG_DENSITY_POWER:
                                case DENSITY_POWER:
                                case LOG_DENSITY_POWER_INVERSE:
                                    density[q] = (long) Math.min(((double) density[q]) * 1.01d, Long.MAX_VALUE);
                                    break;
                                default:
                                    break;
                            }
                            max = Math.max(max, density[p]);
                        } catch (ArithmeticException ae) { /* ignored */ }
                    }

                    // Choose the colour based on the display mode
                    Color color = Color.BLACK;
                    if (mode.isColour()) {
                        if (mode.isIFSColour()) {
                            color = Color.getHSBColor((float) (old.getX() / size.getWidth()), (float) (old.getY() / size.getHeight()), 0.8f);
                        } else if (mode.isPalette()) {
                            if (mode.isStealing()) {
                                color = controller.getSourcePixel(old.getX(), old.getY());
                            } else {
                                if (render == Render.TOP) {
                                    color = Iterables.get(controller.getColours(), top[p] % controller.getPaletteSize());
                                } else {
                                    color = Iterables.get(controller.getColours(), j % controller.getPaletteSize());
                                }
                            }
                        } else {
                            if (render == Render.TOP) {
                                color = Color.getHSBColor((float) top[p] / (float) ifs.size(), 0.8f, 0.8f);
                            } else {
                                color = Color.getHSBColor((float) j / (float) ifs.size(), 0.8f, 0.8f);
                            }
                        }
                        if (render.isDensity()) {
                            if (mode.isIFSColour()) {
                                colour[p] = (double) (color.getRGB() & RGB24) / (double) RGB24;
                            } else {
                                colour[p] = (colour[p] + (double) (color.getRGB() & RGB24) / (double) RGB24) / 2d;
                            }
                        }
                    }

                    // Set the paint colour according to the rendering mode
                    if (render == Render.IFS) {
                        g.setPaint(alpha(color, 255));
                    } else {
                        if (render == Render.MEASURE) {
                            if (top[p] != 0) {
                                color = new Color(top[p]);
                                Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
                                if (hsb[2] < 0.8f) {
                                    color = color.brighter();
                                }
                            }
                            top[p] = color.getRGB();
                        }
                        g.setPaint(alpha(color, isVisible() ? 16 : 128));
                    }

                    // Paint pixels unless using density rendering
                    if (!render.isDensity()) {
                        // Apply controller gamma correction
                        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
                        g.setPaint(alpha(new Color(Color.HSBtoRGB(hsb[0], hsb[1], (float) Math.pow(hsb[2], controller.getGamma()))), color.getAlpha()));
                        rect.setLocation(x, y);
                        g.fill(rect);
                    }
                }
            }
        });
    }

    public void plotDensity(BufferedImage targetImage, int r, Render render, Mode mode) {
        context(controller, targetImage.getGraphics(), g -> {
            boolean log = render.isLog();
            boolean invert = render.isInverse();
            float hsb[] = new float[3];
            int rgb[] = new int[3];
            float gamma = controller.getGamma();
            Rectangle rect = new Rectangle(0, 0, r, r);
            for (int x = 0; x < size.width; x++) {
                for (int y = 0; y < size.height; y++) {
                    int p = x + y * size.width;
                    double ratio = unity().apply(log ? Math.log(density[p]) / Math.log(max) : (double) density[p] / (double) max);
                    float gray = (float) Math.pow(invert ? ratio : 1d - ratio, gamma);
                    if (ratio > 0.05d) {
                        if (mode.isColour()) {
                            int color = (int) (colour[p] * RGB24);
                            rgb[0] = (color >> 16) & 0xff;
                            rgb[1] = (color >> 8) & 0xff;
                            rgb[2] = (color >> 0) & 0xff;
                            if (render == Render.LOG_DENSITY_FLAME || render == Render.LOG_DENSITY_FLAME_INVERSE) {
                                float alpha = (float) (Math.log(density[p]) / density[p]);
                                alpha = (float) Math.pow(invert ? alpha : 1f - alpha, gamma);
                                rgb[0] *= alpha;
                                rgb[1] *= alpha;
                                rgb[2] *= alpha;
                            } else {
                                rgb[0] *= gray;
                                rgb[1] *= gray;
                                rgb[2] *= gray;
                            }
                            Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsb);
                            g.setPaint(alpha(new Color(Color.HSBtoRGB(hsb[0], hsb[1], gray * 0.8f)), (int) (ratio * 255)));
                        } else {
                            g.setPaint(new Color(gray, gray, gray, (float) ratio));
                        }
                        int s = (render == Render.LOG_DENSITY_BLUR || render == Render.LOG_DENSITY_BLUR_INVERSE) ? (int) ((invert ? ratio : 1d - ratio) * r * 4f) : r;
                        rect.setLocation(x, y);
                        rect.setSize(s, s);
                        g.fill(rect);
                    }
                }
            }
        });
    }

    /**
     * Invoked when the timer fires, to refresh the image when rendering.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isVisible()) {
            repaint();
        }
    }

    /**
     * Called as a task to render the IFS.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (controller.isIterationsUnlimited() || count.get() < controller.getIterationsLimit()) {
            Function function = controller.getFinal().getFunction(getSize());
            iterate(getImage(), 1, controller.getIterations(), scale, centre,
                    controller.getRender(), controller.getMode(), function);
        } else {
            token.incrementAndGet();
        }
    }

    public Runnable task(AtomicBoolean cancel, Runnable task) {
        return () -> {
            long initial = token.get();
            String name = Thread.currentThread().getName();
            controller.debug("Started task %s", name);
            do {
                task.run();
            } while (!cancel.get() && token.get() == initial);
            controller.debug("Stopped task %s", name);
        };
    }

    public void submit(Task type, Runnable task) {
        AtomicBoolean cancel = new AtomicBoolean(false);
        ListenableFuture<?> future = executor.submit(task(cancel, task));
        future.addListener(cleaner, executor);
        synchronized (tasks) {
            tasks.put(type, future);
            state.put(future, cancel);
        }
    }

    /** @see java.util.concurrent.ThreadFactory#newThread(Runnable) */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r);
        t.setName("iterator-" + task.incrementAndGet());
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            controller.debug("Starting");
            loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            int iterators = controller.getThreads() - (controller.getRender().isDensity() ? 1 : 0);
            for (int i = 0; i < iterators; i++) {
                submit(Task.ITERATE, this);
            }
            if (controller.getRender().isDensity()) {
                submit(Task.PLOT_DENSITY, () -> {
                    BufferedImage old = image.get();
                    BufferedImage plot = newImage(getSize());
                    plotDensity(plot, 1, controller.getRender(), controller.getMode());
                    image.compareAndSet(old, plot);
                });
            }
            pause.setEnabled(true);
            resume.setEnabled(false);
            timer.start();
            loop.enter();
        }
    }

    public boolean stop() {
        boolean stopped = running.compareAndSet(true, false);
        if (stopped) {
            controller.debug("Stopping");
            token.incrementAndGet();
            synchronized (tasks) {
                state.values().forEach(b -> b.compareAndSet(false, true));
            }
            timer.stop();
            pause.setEnabled(false);
            resume.setEnabled(true);
            loop.exit();
            repaint();
        }
        return stopped;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (isVisible()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    if (isRunning()) {
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
                case KeyEvent.VK_T:
                    List<String> dump = Lists.newArrayList();
                    synchronized (tasks) {
                        for (Task type : Task.values()) {
                            int count = tasks.get(type).size();
                            dump.add(String.format("%s (%d)", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, type.name()), count));
                        }
                        Thread[] threads = new Thread[group.activeCount()];
                        group.enumerate(threads);
                        for (Thread thread : threads) {
                            StackTraceElement stack = thread.getStackTrace()[0];
                            if (stack.isNativeMethod() // FIXME OpenJDK only
                                    && stack.getMethodName().equals("park")
                                    && stack.getClassName().endsWith("Unsafe")) continue;
                            dump.add(String.format("%s - %s", thread.getName(), stack));
                        }
                    }
                    String output = dump.stream()
                            .map(Explorer.STACK::concat)
                            .collect(Collectors.joining(NEWLINE));
                    controller.timestamp("Thread dump");
                    System.err.println(output);

                    break;
                case KeyEvent.VK_UP:
                    synchronized (tasks) {
                        controller.setThreads(controller.getThreads() + 1);
                        if (controller.getThreads() > tasks.size()) {
                            if (isRunning()) {
                                submit(Task.ITERATE, this);
                            }
                        }
                    }
                    repaint();
                    break;
                case KeyEvent.VK_DOWN:
                    synchronized (tasks) {
                        controller.setThreads(controller.getThreads() - 1);
                        if (tasks.size() > controller.getThreads()) {
                            if (isRunning()) {
                                Optional<Future<?>> cancel = tasks.get(Task.ITERATE)
                                        .stream()
                                        .filter(f -> !f.isDone())
                                        .findFirst();
                                cancel.ifPresent(f -> state.get(f).set(true));
                            }
                        }
                    }
                    repaint();
                    break;
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

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
        if (SwingUtilities.isLeftMouseButton(e) && zoom != null) {
            Point end = e.getPoint();
            int x1 = Math.min(end.x, zoom.x);
            int x2 = Math.max(end.x, zoom.x);
            int y1 = Math.min(end.y, zoom.y);
            int y2 = Math.max(end.y, zoom.y);
            int side = Math.min(x2 - x1,  y2 - y1);
            if (side < controller.getSnapGrid()) {
                side = 0;
            }
            zoom.setSize(new Dimension(side, side));
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
                Point2D origin = new Point2D.Double((centre.getX() * scale) - (size.getWidth() / 2d), (centre.getY() * scale) - (size.getHeight() / 2d));
                Point2D updated = new Point2D.Double((zoom.x + (zoom.width / 2d) + origin.getX()) / scale, (zoom.y + (zoom.height / 2d) + origin.getY()) / scale);
                if (zoom.width == 0) {
                    rescale(scale * 2f, updated);
                } else {
                    rescale(scale * ((float) size.getWidth() / (float) zoom.width), updated);
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

}
