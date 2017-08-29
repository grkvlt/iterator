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
import java.util.Objects;

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

    /**
     * Formatter for {@link Double} values in {@link JFormattedTextField}s.
     */
    public static class DoubleFormatter extends AbstractFormatter {

        private final int digits;

        DoubleFormatter() {
            this(3);
        }

        DoubleFormatter(int digits) {
            this.digits = digits;
        }

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
            String text = String.format("%." + digits + "f", value);
            String zeros = "\\.0*$";
            if (text.matches(zeros)) {
                text = text.replaceAll(zeros, "");
            }
            return text;
        }

    }

    /**
     * Formatter for {@link Optional<Double>} values in {@link JFormattedTextField}s.
     */
    public static class OptionalDoubleFormatter extends AbstractFormatter {

        private final DoubleFormatter formatter;

        OptionalDoubleFormatter() {
            this(3);
        }

        OptionalDoubleFormatter(int digits) {
            this.formatter = new DoubleFormatter(digits);
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (Strings.isNullOrEmpty(text)) {
                return Optional.absent();
            }
            try {
                return Optional.of(formatter.stringToValue(text));
            } catch (NumberFormatException e) {
                return Optional.absent();
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            Optional<Double> optional = (Optional<Double>) value;
            if (value instanceof Optional && optional.isPresent()) {
                return formatter.valueToString(optional.get());
            } else {
                return "";
            }
        }

    }

    /**
     * Formatter for {@link Float} values in {@link JFormattedTextField}s.
     */
    public static class FloatFormatter extends AbstractFormatter {

        private final int digits;

        FloatFormatter() {
            this(3);
        }

        FloatFormatter(int digits) {
            this.digits = digits;
        }

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
            String text = String.format("%." + digits + "f", value);
            String zeros = "\\.0*$";
            if (text.matches(zeros)) {
                text = text.replaceAll(zeros, "");
            }
            return text;
        }

    }

    /**
     * Formatter for {@link Integer} values in {@link JFormattedTextField}s.
     */
    public static class IntegerFormatter extends AbstractFormatter {

        private final int min, max;

        IntegerFormatter() {
            this(Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        IntegerFormatter(int min, int max) {
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

        LongFormatter() {
            this(Long.MIN_VALUE, Long.MAX_VALUE);
        }

        LongFormatter(long min, long max) {
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

        StringFormatter() { }

        @Override
        public Object stringToValue(String text) throws ParseException {
            return text;
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            return Objects.toString(value);
        }

    }

}