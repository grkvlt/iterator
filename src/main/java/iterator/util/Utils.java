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
package iterator.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Useful static methods.
 */
public class Utils {
    /**
     * Waits until the {@code input} {@link Supplier#get() supplies} a non-null object.
     * <p>
     * This does not have a timeout, and will wait forever.
     * <pre>
     * {@code bus = waitFor(new Supplier<EventBus>() {
     *     public EventBus get() { return explorer.getEventBus(); }
     * });}
     * </pre>
     *
     * @param input a {@link Supplier} for the required object
     * @return the supplied object
     * @see Supplier#get()
     * @see Predicates#notNull()
     */
    public static <T> T waitFor(final Supplier<T> input) {
        return waitFor(Predicates.<T>notNull(), input);
    }
    public static <T> T waitFor(final Predicate<T> predicate, final Supplier<T> input) {
        for (int spin = 0; Predicates.not(predicate).apply(input.get());) {
            if (++spin % 10_000_000 == 0) System.err.print('.');
        }
        T result = input.get();
        System.err.println(result.getClass().getName());
        return result;
    }

    /**
     * Concatenates a series of optional elements onto an initial {@link List}.
     * <pre>
     * {@code List<Transform> all = concatenate(ifs, selected);}
     * </pre>
     *
     * @param initial the initial {@link List}
     * @param optional a series of optional elements that may be null
     * @return a new {@link List} including the non-null optional elements
     * @see Iterables#concat(Iterable)
     * @see Optional#presentInstances(Iterable)
     */
    @SafeVarargs
    public static <T> List<T> concatenate(List<T> initial, T...optional) {
        List<Optional<T>> extra = Lists.newArrayList();
        for (T nullable : optional) {
            extra.add(Optional.fromNullable(nullable));
        }
        Iterable<T> joined = Iterables.concat(initial, Optional.presentInstances(extra));
        return Lists.newArrayList(joined);
    }

    /**
     * Paints text over the spash screen image.
     */
    public static void paintSplash(Graphics2D g, int width, int height) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Calibri", Font.BOLD, 80));
        g.drawString("IFS Explorer", 10, 65);
        g.setFont(new Font("Calibri", Font.BOLD, 25));
        g.drawString("Version " + Version.instance().get(), 10, 100);
        g.setFont(new Font("Calibri", Font.BOLD, 13));
        g.drawString("Copyright 2012-2017 by Andrew Kennedy", 10, height - 15);
        g.setFont(new Font("Consolas", Font.BOLD, 12));
        g.drawString("http://grkvlt.github.io/iterator/", 260, height - 15);
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

    /**
     * Formatter for {@link Double} values in {@link JFormattedTextField}s.
     */
    public static class DoubleFormatter extends AbstractFormatter {
        @Override
        public Object stringToValue(String text) throws ParseException {
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException e) {
                return 0d;
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return String.format("%.4f", value);
        }
    }

    /**
     * Formatter for {@link Optional<Double>} values in {@link JFormattedTextField}s.
     */
    public static class OptionalDoubleFormatter extends AbstractFormatter {
        @Override
        public Object stringToValue(String text) throws ParseException {
            if (Strings.isNullOrEmpty(text)) {
                return Optional.absent();
            }
            try {
                return Optional.of(Double.valueOf(text));
            } catch (NumberFormatException e) {
                return Optional.absent();
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            Optional<Double> optional = (Optional<Double>) value;
            if (optional.isPresent()) {
                return String.format("%.4f", optional.get());
            } else {
                return "";
            }
        }
    }

    /**
     * Formatter for {@link Float} values in {@link JFormattedTextField}s.
     */
    public static class FloatFormatter extends AbstractFormatter {
        @Override
        public Object stringToValue(String text) throws ParseException {
            try {
                return Float.valueOf(text);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return String.format("%.4f", value);
        }
    }

    /**
     * Formatter for {@link Integer} values in {@link JFormattedTextField}s.
     */
    public static class IntegerFormatter extends AbstractFormatter {
        private final int min, max;

        public IntegerFormatter() {
            this(Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        public IntegerFormatter(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            try {
                return Integer.min(max, Integer.max(min, Integer.valueOf(text)));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return Objects.toString(value);
        }
    }

    /**
     * Formatter for {@link Long} values in {@link JFormattedTextField}s.
     */
    public static class LongFormatter extends AbstractFormatter {
        private final long min, max;

        public LongFormatter() {
            this(Long.MIN_VALUE, Long.MAX_VALUE);
        }

        public LongFormatter(long min, long max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            try {
                return Long.min(max, Long.max(min, Long.valueOf(text)));
            } catch (NumberFormatException e) {
                return 0l;
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return Objects.toString(value);
        }
    }

    /**
     * Formatter for {@link String} values in {@link JFormattedTextField}s.
     */
    public static class StringFormatter extends AbstractFormatter {
        @Override
        public Object stringToValue(String text) throws ParseException {
            return text;
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return Objects.toString(value);
        }
    }

    /**
     * Loads an image from a {@link URL} into a {@link BufferedImage}.
     *
     * @param url
     * @return the loaded image
     */
    public static BufferedImage loadImage(URL url) {
        try {
            return ImageIO.read(url);
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }
}