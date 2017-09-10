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

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Range;

/**
 * Implementations of {@link AbstractFormatter} for dialog boxes.
 */
public class Formatter {

    public static DoubleFormatter doubles()  {
        return new DoubleFormatter();
    }

    public static DoubleFormatter doubles(int digits)  {
        return new DoubleFormatter(digits);
    }

    public static FloatFormatter floats()  {
        return new FloatFormatter();
    }

    public static FloatFormatter floats(int digits)  {
        return new FloatFormatter(digits);
    }

    public static IntegerFormatter integers()  {
        return new IntegerFormatter();
    }

    public static IntegerFormatter integers(int min, int max)  {
        return new IntegerFormatter(min, max);
    }

    public static LongFormatter longs()  {
        return new LongFormatter();
    }

    public static LongFormatter longs(long min, long max)  {
        return new LongFormatter(min, max);
    }

    public static StringFormatter strings()  {
        return new StringFormatter();
    }

    public static <T> OptionalFormatter<T> optional(BaseFormatter<T> formatter)  {
        return new OptionalFormatter(formatter);
    }

    public static <T extends Number & Comparable<T>> BaseFormatter<T> range(Range<T> range, BaseFormatter<T> formatter)  {
        return new RangeFormatter<T>(range, formatter);
    }

    public static abstract class BaseFormatter<T> extends AbstractFormatter {

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (Strings.isNullOrEmpty(text)) {
                return getDefault();
            } else {
                try {
                    return tryParse(text);
                } catch (Exception e) {
                    return getDefault();
                }
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            } else {
                return toString((T) value);
            }
        }

        public abstract T tryParse(String text);

        public abstract String toString(T value);

        public abstract T getDefault();

    }

    public static class OptionalFormatter<T> extends BaseFormatter<Optional<T>> {

        protected BaseFormatter<T> formatter;

        public OptionalFormatter(BaseFormatter<T> formatter) {
            this.formatter = formatter;
        }

        @Override
        public Optional<T> tryParse(String text) {
            return Optional.of(formatter.tryParse(text));
        }

        @Override
        public String toString(Optional<T> value) {
            if (value.isPresent()) {
                return formatter.toString(value.get());
            } else {
                return "";
            }
        }

        @Override
        public Optional<T> getDefault() { return Optional.absent(); }

    }

    public static class RangeFormatter<T extends Number & Comparable<T>> extends BaseFormatter<T> {

        protected BaseFormatter<T> formatter;
        protected Range<T> range;

        RangeFormatter(Range<T> range, BaseFormatter<T> formatter) {
            this.formatter = formatter;
            this.range = range;
        }

        @Override
        public T tryParse(String text) {
            T value = formatter.tryParse(text);
            if (range.contains(value)) {
                return value;
            } else if (value.compareTo(range.upperEndpoint()) > 0) {
                return range.upperEndpoint();
            } else {
                return range.lowerEndpoint();
            }
        }

        @Override
        public String toString(T value) {
            return formatter.toString(value);
        }

        @Override
        public T getDefault() { return formatter.getDefault(); }

    }

    /**
     * Formatter for {@link Double} values in {@link JFormattedTextField}s.
     */
    public static class DoubleFormatter extends BaseFormatter<Double> {

        private int digits;

        DoubleFormatter() {
            this(3);
        }

        DoubleFormatter(int digits) {
            this.digits = digits;
        }

        @Override
        public Double tryParse(String text) {
            return Double.valueOf(text);
        }

        @Override
        public String toString(Double value) {
            String text = String.format("%." + digits + "f", value);
            if (text.matches("^-?[0-9]*\\.0+$")) { // ###.000
                text = text.replaceAll("\\.0+$", ""); // ###
            }
            return text;
        }

        @Override
        public Double getDefault() { return 0d; }

    }

    /**
     * Formatter for {@link Float} values in {@link JFormattedTextField}s.
     */
    public static class FloatFormatter extends BaseFormatter<Float> {

        private DoubleFormatter formatter;

        FloatFormatter() {
            this(3);
        }

        FloatFormatter(int digits) {
            this.formatter = new DoubleFormatter(digits);
        }

        @Override
        public Float tryParse(String text) {
            Double doubleValue = formatter.tryParse(text);
            return doubleValue.floatValue();
        }

        @Override
        public String toString(Float value) {
            Double doubleValue = value.doubleValue();
            return formatter.toString(doubleValue);
        }

        @Override
        public Float getDefault() { return 0f; }

    }

    /**
     * Formatter for {@link Integer} values in {@link JFormattedTextField}s.
     */
    public static class IntegerFormatter extends BaseFormatter<Integer> {

        private int min, max;

        IntegerFormatter() {
            this(Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        IntegerFormatter(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public Integer tryParse(String text) {
            return Integer.min(max, Integer.max(min, Integer.valueOf(text)));
        }

        @Override
        public String toString(Integer value) {
            return Integer.toString(value);
        }

        @Override
        public Integer getDefault() { return 0; }

    }

    /**
     * Formatter for {@link Long} values in {@link JFormattedTextField}s.
     */
    public static class LongFormatter extends BaseFormatter<Long> {

        private long min, max;

        LongFormatter() {
            this(Long.MIN_VALUE, Long.MAX_VALUE);
        }

        LongFormatter(long min, long max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public Long tryParse(String text) {
            return Long.min(max, Long.max(min, Long.valueOf(text)));
        }

        @Override
        public String toString(Long value) {
            return Long.toString(value);
        }

        @Override
        public Long getDefault() { return 0l; }

    }

    /**
     * Formatter for {@link String} values in {@link JFormattedTextField}s.
     */
    public static class StringFormatter extends BaseFormatter<String> {

        StringFormatter() { }

        @Override
        public String tryParse(String text) {
            return text;
        }

        @Override
        public String toString(String value) {
            return value;
        }

        @Override
        public String getDefault() { return ""; }

    }

}