/*
 * Copyright 2012-2019 by Andrew Kennedy.
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
package iterator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import iterator.model.Transform;
import iterator.util.Config;
import iterator.util.Version;

/**
 * Useful static methods.
 */
public class Utils {

    public static String version() {
        return Version.instance().get();
    }

    @Nonnull
    public static final String NEWLINE = Objects.requireNonNull(StandardSystemProperty.LINE_SEPARATOR.value());

    public static final String DEBUG = "[?] ";
    public static final String PRINT = "[-] ";
    public static final String ERROR = "[!] ";
    public static final String STACK = "[>] ";
    public static final String PAUSE = "[.] ";

    public static final int RGB24 = 0xffffff;

    public static final Range<Double> UNITY = Range.open(0d, 1d);

    public static DoubleFunction<Double> unity() {
        return v -> clamp().apply(UNITY, v);
    }

    public static IntFunction<Integer> octet() {
        return v -> clamp(0, 255).apply(v);
    }

    public static BiFunction<Range<Double>, Double, Double> clamp() {
        return (r,v) -> Math.min(r.upperEndpoint(), Math.max(r.lowerEndpoint(), v));
    }

    public static DoubleFunction<Double> clamp(Range<Double> range) {
        return v -> clamp().apply(range, v);
    }

    public static IntFunction<Integer> clamp(Integer lowerBound, Integer upperBound) {
        return v -> Math.min(upperBound, Math.max(lowerBound, v));
    }

    public static LongFunction<Long> clamp(Long lowerBound, Long upperBound) {
        return v -> Math.min(upperBound, Math.max(lowerBound, v));
    }

    public static IntFunction<Integer> threads() {
        return clamp(Config.MIN_THREADS, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Concatenates a series of optional elements onto an initial {@link List}.
     * <pre>
     * {@code List<Transform> all = concatenate(ifs, selected);}
     * </pre>
     *
     * @param initial the initial {@link Collection}
     * @param optional a series of optional elements that may be null
     * @return a new {@link List} including the non-null optional elements
     *   not in the initial collection
     */
    @SafeVarargs
    public static <T> List<T> concatenate(Collection<T> initial, T...optional) {
        List<T> joined = Lists.newArrayList(initial);
        for (T item : optional) {
            if (item != null && !initial.contains(item)) {
                joined.add(item);
            }
        }
        return joined;
    }

    public static final String CALIBRI = "Calibri";
    public static final String CAMBRIA = "Cambria";
    public static final String CONSOLAS = "Consolas";

    /** Calibri {@link Font}. */
    public static Font calibri(int style, int size) {
        return(new Font(CALIBRI, style, size));
    }

    /** Cambria {@link Font}. */
    public static Font cambria(int style, int size) {
        return(new Font(CAMBRIA, style, size));
    }

    /** Consolas {@link Font}. */
    public static Font consolas(int style, int size) {
        return(new Font(CONSOLAS, style, size));
    }

    /** Solid {@link BasicStroke line}. */
    public static Stroke solid(float width) {
        return new BasicStroke(width);
    }

    /** Dashed {@link BasicStroke line}. */
    public static Stroke dashed(float width, float[] pattern) {
        return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, pattern, 0f);
    }

    public static final Stroke SOLID_LINE_1 = solid(1f);
    public static final Stroke SOLID_LINE_2 = solid(2f);
    public static final Stroke DASHED_LINE_1 = dashed(1f, new float[] { 5f, 5f });
    public static final Stroke DASHED_LINE_2 = dashed(2f, new float[] { 10f, 10f });
    public static final Stroke DOTTED_LINE_2 = dashed(2f, new float[] { 5f, 5f });
    public static final Stroke PATTERNED_LINE_2 = dashed(2f, new float[] { 15f, 10f, 5f, 10f });

    public static void sleep(long duration, TimeUnit unit) {
        long millis = unit.toMillis(duration);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();  // set interrupt flag
        }
    }

    /** Critical section with a {@link Semaphore}. */
    @SafeVarargs
    public static void critical(Semaphore semaphore, Runnable action, BiConsumer<Throwable, String>...exceptionHandlers) {
        try {
            semaphore.acquire();
            action.run();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();  // set interrupt flag
        } catch (Throwable t) {
            for (BiConsumer<Throwable, String> h : exceptionHandlers) {
                h.accept(t, "Error executing critical section");
            }
        } finally {
            semaphore.release();
        }
    }

    /** Critical section with a {@code synchronized} block. */
    @SafeVarargs
    public static void locked(@Nonnull Object mutex, Runnable action, BiConsumer<Throwable, String>...exceptionHandlers) {
        synchronized (mutex) {
            try {
                action.run();
            } catch (Throwable t) {
                for (BiConsumer<Throwable, String> h : exceptionHandlers) {
                    h.accept(t, "Error executing critical section");
                }
            }
        }
    }

    /** Perform an {@link Consumer action} with a disposable {@link Graphics2D graphics context}. */
    public static void context(BiConsumer<Throwable, String> exceptionHandler, Graphics graphics, Consumer<Graphics2D> action) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            action.accept(g);
        } catch (Throwable t) {
            exceptionHandler.accept(t, "Error executing graphics context action");
        } finally {
            g.dispose();
        }
    }

    public static Action action(String text, Consumer<ActionEvent> action) {
        return new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        };
    }

    public static JButton button(String text, Consumer<ActionEvent> handler) {
        return new JButton(action(text, handler));
    }

    public static JMenuItem menuItem(String text, Consumer<ActionEvent> handler) {
        return new JMenuItem(action(text, handler));
    }

    public static JCheckBoxMenuItem checkBoxItem(String text, Consumer<ActionEvent> handler) {
        return new JCheckBoxMenuItem(action(text, handler));
    }

    public static BiConsumer<Throwable, String> printError() {
        return (t, m) -> {
            System.out.println(String.format("%s%s: %s", ERROR, m, t.getMessage()));
            t.printStackTrace(System.err);
            System.exit(1);
        };
    }

    public static BiConsumer<Throwable, String> throwError() {
        return (t, m) -> {
            throw new RuntimeException(m, t);
        };
    }

    public static Point2D copyPoint(Point2D source) {
        return new Point2D.Double(source.getX(), source.getY());
    }

    /**
     * Returns a new {@link Color} with the same RGB value but updated alpha channel.
     *
     * @param c the colour to copy
     * @param alpha the alpha channel value
     * @return the colour
     */
    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** @see #alpha(Color, int) */
    public static Color alpha(int rgb, int alpha) {
        int rgba = (rgb & RGB24) | ((alpha & 0xff) << 24);
        return new Color(rgba, true);
    }

    public static FileSystem initFileSystem(URI uri) {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException fsnfe) {
            try {
                Map<String, String> env = ImmutableMap.of("create", "true");
                return FileSystems.newFileSystem(uri, env);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
    }

    /**
     * Loads an image from a {@link URL} into a {@link BufferedImage}.
     *
     * @param url the URL of the image to load
     * @return the loaded image
     */
    public static BufferedImage loadImage(URL url) {
        try {
            return ImageIO.read(url);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Saves a {@link BufferedImage} to a {@link File} as a {@literal PNG}.
     *
     * @param source the image data
     * @param file the target image file location
     */
    public static void saveImage(BufferedImage source, File file) {
        try {
            ImageIO.write(source, "png", file);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    public static Color getPixel(BufferedImage source, Dimension size, double x, double y) {
        int sx = (int) Math.max(0, Math.min(source.getWidth() - 1, (x / size.getWidth()) * source.getWidth()));
        int sy = (int) Math.max(0, Math.min(source.getHeight() - 1, (y / size.getHeight()) * source.getHeight()));
        return new Color(source.getRGB(sx, sy));
    }

    public static double weight(List<Transform> transforms) {
        return transforms.stream().map(Transform::getWeight).reduce(0d, Double::sum);
    }

    public static double area(List<Transform> transforms) {
        return transforms.stream().map(t -> t.getWidth() * t.getHeight()).reduce(0d, Double::sum);
    }

    public static double width(List<Transform> transforms) {
        double min = transforms.stream().map(Transform::getTranslateX).reduce(0d, Double::min);
        double max = transforms.stream().map(Transform::getTranslateX).reduce(0d, Double::max);
        return max - min;
    }

    public static double height(List<Transform> transforms) {
        double min = transforms.stream().map(Transform::getTranslateY).reduce(0d, Double::min);
        double max = transforms.stream().map(Transform::getTranslateY).reduce(0d, Double::max);
        return max - min;
    }
}