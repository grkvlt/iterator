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
package iterator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import javax.imageio.ImageIO;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import iterator.model.Transform;

/**
 * Useful static methods.
 */
public class Utils {

    public static final int RGB24 = 0xffffff;
    public static final Range<Double> UNITY = Range.open(0d, 1d);

    public static DoubleFunction<Double> unity() {
        return v -> clamp().apply(UNITY, v);
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