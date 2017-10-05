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

import static iterator.Utils.DEBUG;
import static iterator.Utils.ERROR;
import static iterator.Utils.NEWLINE;
import static iterator.Utils.PAUSE;
import static iterator.Utils.PRINT;
import static iterator.Utils.STACK;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;

/**
 * Text output.
 */
public class Output implements UncaughtExceptionHandler, BiConsumer<Throwable, String> {

    public Output() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void debug(String format, Object...varargs) {
        output(System.out, DEBUG + format, varargs);
    }

    public void error(String format, Object...varargs) {
        error(Optional.absent(), format, varargs);
    }

    public void error(Throwable t, String format, Object...varargs) {
        error(Optional.of(t), format, varargs);
    }

    public void error(Throwable t, String message) {
        error(Optional.of(t), "%s: %s", message, t.getMessage());
    }

    public void error(Optional<Throwable> t, String format, Object...varargs) {
        output(System.out, ERROR + format, varargs);
        if (t.isPresent()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream print = new PrintStream(bytes);
            t.get().printStackTrace(print);
            String trace = Splitter.on(CharMatcher.anyOf("\r\n"))
                    .omitEmptyStrings()
                    .splitToList(bytes.toString())
                    .stream()
                    .map(STACK::concat)
                    .collect(Collectors.joining(NEWLINE));
            System.err.println(trace);
        }
        System.exit(1);
    }

    public void timestamp(String format, Object...varargs) {
        String message = String.format(format,  varargs);
        String timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        debug("%s at %s", message, timestamp);
    }

    public void dumpStack() {
        List<StackTraceElement> stack = Arrays.asList(Thread.getAllStackTraces().get(Thread.currentThread()));
        String trace = stack.stream()
                .skip(3)
                .map(e -> String.format("  %s.%s(%s:%d)", e.getClassName(), e.getMethodName(), e.getFileName(), e.getLineNumber()))
                .map(STACK::concat)
                .collect(Collectors.joining(NEWLINE));
        timestamp("Dumping stack");
        System.err.println(trace);
    }

    public void print(String format, Object...varargs) {
        output(System.out, PRINT + format, varargs);
    }

    public void stack(String format, Object...varargs) {
        output(System.out, STACK + format, varargs);
    }

    public void pause(String format, Object...varargs) {
        System.out.printf("\r" + PAUSE + format, varargs);
    }

    public void blank() {
        System.out.printf("\r");
    }

    public void println() {
        System.out.println();
    }

    protected void output(PrintStream out, String format, Object...varargs) {
        String output = String.format(format, varargs);
        if (!output.endsWith(NEWLINE)) output = output.concat(NEWLINE);
        out.print(output);
    }

    /** @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable) */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        error(Optional.of(e), "Error: Thread %s (%d) caused %s: %s", t.getName(), t.getId(), e.getClass().getName(), e.getMessage());
    }

    @Override
    public void accept(Throwable t, String message) {
        error(t, message);
    }

}
