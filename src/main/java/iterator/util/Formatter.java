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

/**
 * Implementations of {@link AbstractFormatter} for dialog boxes.
 */
public class Formatter {

    public static AbstractFormatter doubles()  {
        return new DoubleFormatter();
    }

    public static AbstractFormatter doubles(int digits)  {
        return new DoubleFormatter(digits);
    }

    public static AbstractFormatter optionalDoubles()  {
        return new OptionalDoubleFormatter();
    }

    public static AbstractFormatter optionalDoubles(int digits)  {
        return new OptionalDoubleFormatter(digits);
    }

    public static AbstractFormatter floats()  {
        return new FloatFormatter();
    }

    public static AbstractFormatter floats(int digits)  {
        return new FloatFormatter(digits);
    }

    public static AbstractFormatter integers()  {
        return new IntegerFormatter();
    }

    public static AbstractFormatter integers(int min, int max)  {
        return new IntegerFormatter(min, max);
    }

    public static AbstractFormatter longs()  {
        return new LongFormatter();
    }

    public static AbstractFormatter longs(int min, int max)  {
        return new LongFormatter(min, max);
    }

    public static AbstractFormatter strings()  {
        return new StringFormatter();
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

        protected abstract T tryParse(String text);

        protected abstract String toString(T value);

        protected abstract T getDefault();

    }

    public static abstract class BaseOptionalFormatter<T> extends BaseFormatter<Optional<T>> {

        protected BaseFormatter<T> formatter;

        @Override
        protected Optional<T> tryParse(String text) {
            return Optional.of(formatter.tryParse(text));
        }

        @Override
        protected String toString(Optional<T> value) {
            if (value.isPresent()) {
                return formatter.toString(value.get());
            } else {
                return "";
            }
        }

        @Override
        protected Optional<T> getDefault() { return Optional.absent(); }

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
        protected Double tryParse(String text) {
            return Double.valueOf(text);
        }

        @Override
        protected String toString(Double value) {
            String text = String.format("%." + digits + "f", value);
            if (text.matches("^-?[0-9]*\\.0+$")) { // ###.000
                text = text.replaceAll("\\.0+$", ""); // ###
            }
            return text;
        }

        @Override
        protected Double getDefault() { return 0d; }

    }

    /**
     * Formatter for {@link Optional<Double>} values in {@link JFormattedTextField}s.
     */
    public static class OptionalDoubleFormatter extends BaseOptionalFormatter<Double> {

        OptionalDoubleFormatter() {
            this(3);
        }

        OptionalDoubleFormatter(int digits) {
            this.formatter = new DoubleFormatter(digits);
        }

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
        protected Float tryParse(String text) {
            Double doubleValue = formatter.tryParse(text);
            return doubleValue.floatValue();
        }

        @Override
        protected String toString(Float value) {
            Double doubleValue = value.doubleValue();
            return formatter.toString(doubleValue);
        }

        @Override
        protected Float getDefault() { return 0f; }

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
        protected Integer tryParse(String text) {
            return Integer.min(max, Integer.max(min, Integer.valueOf(text)));
        }

        @Override
        protected String toString(Integer value) {
            return Integer.toString(value);
        }

        @Override
        protected Integer getDefault() { return 0; }

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
        protected Long tryParse(String text) {
            return Long.min(max, Long.max(min, Long.valueOf(text)));
        }

        @Override
        protected String toString(Long value) {
            return Long.toString(value);
        }

        @Override
        protected Long getDefault() { return 0l; }

    }

    /**
     * Formatter for {@link String} values in {@link JFormattedTextField}s.
     */
    public static class StringFormatter extends BaseFormatter<String> {

        StringFormatter() { }

        @Override
        protected String tryParse(String text) {
            return text;
        }

        @Override
        protected String toString(String value) {
            return value;
        }

        @Override
        protected String getDefault() { return ""; }

    }

}