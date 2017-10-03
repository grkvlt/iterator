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

import static iterator.Utils.NEWLINE;
import static iterator.Utils.RGB24;
import static iterator.Utils.alpha;
import static iterator.Utils.context;
import static iterator.Utils.getPixel;
import static iterator.Utils.locked;
import static iterator.Utils.unity;
import static iterator.Utils.weight;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
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
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.math.LongMath;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import iterator.Explorer;
import iterator.model.Function;
import iterator.model.Transform;
import iterator.util.Config;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;

/**
 * Rendered IFS viewer.
 */
public class Iterator implements Runnable, ThreadFactory {

    public static enum Task { ITERATE, PLOT_DENSITY }

    private final Config config;
    private final BiConsumer<Throwable, String> exceptionHandler;

    private List<Function> transforms;
    private AtomicReference<BufferedImage> image = Atomics.newReference();
    private int top[];
    private long density[];
    private long blur[];
    private double colour[];
    private float vibrancy;
    private int kernel;
    private long max;
    private AtomicBoolean latch = new AtomicBoolean(true);
    private Object mutex = new Object[0];
    private AtomicReferenceArray<Point2D> points = Atomics.newReferenceArray(2);
    private AtomicLong count = new AtomicLong(0l);
    private AtomicInteger task = new AtomicInteger(0);
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicLong token = new AtomicLong(0l);
    private Random random = new Random();
    private float scale = 1.0f;
    private Point2D centre;
    private Dimension size;
    private ThreadGroup group = new ThreadGroup("iterator");
    private ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(this));
    private Multimap<Task, Future<?>> tasks = Multimaps.synchronizedListMultimap(Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList));
    private ConcurrentMap<Future<?>, AtomicBoolean> state = Maps.newConcurrentMap();

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
        };

    public Iterator(BiConsumer<Throwable, String> exceptionHandler, Config config, Dimension size) {
        this.config = config;
        this.exceptionHandler = exceptionHandler;
    }

    public long getCount() { return count.get(); }

    public void setTransforms(List<Function> transforms) {
        this.transforms = transforms;
    }

    public void rescale(float scale, Point2D centre) {
        this.scale = scale;
        this.centre = centre;
    }

    public void rescale() {
        rescale(1f, new Point2D.Double(size.getWidth() / 2d, size.getHeight() / 2d));
    }

    public void reset(Dimension size) {
        this.size = size;

        scale = config.getDisplayScale();
        centre = new Point2D.Double(config.getDisplayCentreX() * size.getWidth(), config.getDisplayCentreY() * size.getHeight());

        image.set(newImage());

        points.set(0, new Point2D.Double((double) random.nextInt(size.width), (double) random.nextInt(size.height)));
        points.set(1, new Point2D.Double((double) random.nextInt(size.width), (double) random.nextInt(size.height)));

        vibrancy = config.getVibrancy();
        kernel = config.getBlurKernel();
        top = new int[size.width * size.height];
        density = new long[size.width * size.height];
        blur = new long[(size.width / kernel + 1) * (size.height / kernel + 1)];
        colour = new double[size.width * size.height];
        max = 1;

        count.set(0l);
    }

    public void iterate(BufferedImage targetImage, int s, long k, float scale, Point2D centre, Render render, Mode mode, List<Function> functions, Function function) {
        context(exceptionHandler, targetImage.getGraphics(), g -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, vibrancy));

            if (functions.isEmpty()) return;

            int n = functions.size();
            List<Transform> transforms = Lists.newArrayList(Iterables.filter(functions, Transform.class));
            double weight = weight(transforms);
            float hsb[] = new float[3];
            Rectangle rect = new Rectangle(0, 0, s, s);
            function.setSize(size);
            Point2D old, current;

            for (long i = 0l; i < k; i++) {
                if (i % 1000l == 0l) {
                    count.incrementAndGet();
                }

                // Skip based on transform weighting
                int j = random.nextInt(functions.size());
                Function f = functions.get(j);
                if ((j < transforms.size() ? ((Transform) f).getWeight() : weight) < random.nextDouble() * weight * (n - transforms.size() + 1d)) {
                    continue;
                }

                // Evaluate the function twice, first for (x,y) position and then for hue/saturation color space
                current = points.updateAndGet(0, p -> function.apply(f.apply(p)));
                old = points.getAndUpdate(1, p -> function.apply(f.apply(p)));

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
                            density[p] = LongMath.checkedAdd(density[p], 1l);
                            switch (render) {
                                case LOG_DENSITY_BLUR:
                                case LOG_DENSITY_BLUR_INVERSE:
                                    density[p] = LongMath.checkedAdd(density[p], kernel - 1);
                                    int q = (x / kernel) + (y / kernel) * (size.width / kernel);
                                    blur[q] = LongMath.checkedAdd(blur[q], 1);
                                    break;
                                case LOG_DENSITY_POWER:
                                case DENSITY_POWER:
                                case LOG_DENSITY_POWER_INVERSE:
                                    density[p] = (long) Math.min(((double) density[p]) * 1.01d, Long.MAX_VALUE);
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
                            color = Color.getHSBColor((float) (old.getX() / size.getWidth()), (float) (old.getY() / size.getHeight()), vibrancy);
                        } else if (mode.isPalette()) {
                            if (mode.isStealing()) {
                                color = getPixel(config.getSourceImage(), size, old.getX(), old.getY());
                            } else {
                                if (render == Render.TOP) {
                                    color = Iterables.get(config.getColours(), top[p] % config.getPaletteSize());
                                } else {
                                    color = Iterables.get(config.getColours(), j % config.getPaletteSize());
                                }
                            }
                        } else {
                            if (render == Render.TOP) {
                                color = Color.getHSBColor((float) top[p] / (float) functions.size(), vibrancy, vibrancy);
                            } else {
                                color = Color.getHSBColor((float) j / (float) functions.size(), vibrancy, vibrancy);
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
                                if (hsb[2] < 0.5f) {
                                    color = color.brighter();
                                }
                            }
                            top[p] = color.getRGB();
                        }
                        g.setPaint(alpha(color, 128));
                    }

                    // Paint pixels unless using density rendering
                    if (!render.isDensity()) {
                        // Apply controller gamma correction
                        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
                        g.setPaint(alpha(Color.HSBtoRGB(hsb[0], hsb[1] * vibrancy, (float) Math.pow(hsb[2], config.getGamma())), color.getAlpha()));
                        rect.setLocation(x, y);
                        g.fill(rect);
                    }
                }
            }
        });
    }

    public void plotDensity(BufferedImage targetImage, int r, Render render, Mode mode) {
        context(exceptionHandler, targetImage.getGraphics(), g -> {
            boolean log = render.isLog();
            boolean invert = render.isInverse();
            float hsb[] = new float[3];
            int rgb[] = new int[3];
            float gamma = config.getGamma();
            Rectangle rect = new Rectangle(0, 0, r, r);
            for (int x = 0; x < size.width; x++) {
                for (int y = 0; y < size.height; y++) {
                    int p = x + y * size.width;
                    double ratio = unity().apply(log ? Math.log(density[p]) / Math.log(max) : (double) density[p] / (double) max);
                    if (render == Render.LOG_DENSITY_BLUR || render == Render.LOG_DENSITY_BLUR_INVERSE) {
                        int q = (x / kernel) + (y / kernel) * (size.width / kernel);
                        double blurred = unity().apply(Math.log(blur[q]) / Math.log(max)) / kernel;
                        ratio = (blurred + ratio) / 2d;
                    }
                    float gray = (float) Math.pow(invert ? ratio : 1d - ratio, gamma);
                    if (ratio > 0.001d) {
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
                            g.setPaint(alpha(Color.HSBtoRGB(hsb[0], hsb[1], gray * vibrancy), (int) (ratio * 255 * vibrancy)));
                        } else {
                            g.setPaint(new Color(gray, gray, gray, (float) ratio));
                        }
                        if (render == Render.LOG_DENSITY_BLUR || render == Render.LOG_DENSITY_BLUR_INVERSE) {
                            int s = 1 + (int) (gray * r * kernel);
                            rect.setSize(s, s);
                        }
                        rect.setLocation(x, y);
                        g.fill(rect);
                    }
                }
            }
        });
    }

    public BufferedImage newImage() {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        context(exceptionHandler, image.getGraphics(), g -> {
            g.setColor(config.getRender().getBackground());
            g.fillRect(0, 0, size.width, size.height);
        });
        return image;
    }

    /**
     * Called as a task to render the IFS.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (config.isIterationsUnlimited() || (count.get() * 1000l) < config.getIterationsLimit()) {
            iterate(image.get(), 1, config.getIterations(), scale, centre,
                    config.getRender(), config.getMode(), transforms, config.getCoordinateTransform());
        } else {
            token.incrementAndGet();
        }
    }

    public Runnable task(AtomicBoolean cancel, Runnable task) {
        return () -> {
            while (latch.get()); // Wait until latch released
            long initial = token.get();
            do {
                task.run();
            } while (!cancel.get() && token.get() == initial);
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

    public Collection<Future<?>> getTaskSet() {
        return tasks.values();
    }

    public Multimap<Task, Future<?>> getTasks() {
        return tasks;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }
    
    public String getThreadDump() {
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
        return output;
    }

    public void updateTasks() {
        synchronized (tasks) {
            if (isRunning()) {
                if (config.getThreads() > tasks.size()) {
                    submit(Task.ITERATE, this);
                } else if (tasks.size() > config.getThreads()) {
                    Optional<Future<?>> cancel = tasks.get(Task.ITERATE)
                            .stream()
                            .filter(f -> !f.isDone())
                            .findFirst();
                    cancel.ifPresent(f -> state.get(f).set(true));
                }
            }
        }
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            locked(mutex, () -> {
                int iterators = config.getThreads() - (config.getRender().isDensity() ? 1 : 0);
                for (int i = 0; i < iterators; i++) {
                    submit(Task.ITERATE, this);
                }
                if (config.getRender().isDensity()) {
                    submit(Task.PLOT_DENSITY, () -> {
                        BufferedImage old = image.get();
                        BufferedImage plot = newImage();
                        plotDensity(plot, 1, config.getRender(), config.getMode());
                        image.compareAndSet(old, plot);
                    });
                }
                latch.set(false);
            });
        }
    }

    public boolean stop() {
        boolean stopped = running.compareAndSet(true, false);
        if (stopped) {
            locked(mutex, () -> {
                latch.set(true);
                token.incrementAndGet();
                synchronized (tasks) {
                    state.values().forEach(b -> b.compareAndSet(false, true));
                }
                while (tasks.size() > 0); // Wait until all tasks stopped
            });
        }
        return stopped;
    }

    public boolean isRunning() {
        return running.get();
    }

    public BufferedImage getImage() {
        return image.get();
    }

}
