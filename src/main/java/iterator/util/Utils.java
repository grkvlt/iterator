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
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
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

    /**
     * Paints text over the spash screen image.
     */
    public static void paintSplashText(Graphics2D g, int width, int height) {
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