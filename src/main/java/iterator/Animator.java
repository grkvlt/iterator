/*
 * Copyright 2012-2013 by Andrew Kennedy.
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

import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Subscriber;
import iterator.view.Viewer;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;

/**
 * IFS Animator.
 */
public class Animator implements Subscriber {
    private Explorer explorer;
    private IFS ifs;
    private EventBus bus;
    private Dimension size;
    private CountDownLatch ready = new CountDownLatch(3);
    private long delay = 500l, frames = 1000l, segment;
    private double scale;
    private Point2D centre;
    private File config, input, output;
    private List<Change> list = Lists.newArrayList();
    private Map<List<Change>, Long> segments = Maps.newLinkedHashMap();

    public Animator(String...argv) throws Exception {
        config = new File(argv[0]);
        if (!config.exists()) {
            throw new IllegalArgumentException("Config file not found: " + config);
        }
        parse();
        if (output.exists() && !output.isDirectory()) {
            throw new IllegalArgumentException("Output location not a directory: " + output);
        } else {
            output.mkdirs();
        }
        if (!input.exists()) {
            throw new IllegalArgumentException("Input file not found: " + input);
        }

        // Start application
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                explorer = new Explorer();
                explorer.start();
            }
        });
 
        waitFor(new Supplier<Explorer>() {
            @Override
            public Explorer get() { return explorer; }
        });
        bus = waitFor(new Supplier<EventBus>() {
            @Override
            public EventBus get() { return explorer.getEventBus(); }
        });

        bus.register(this);
    }

    private <T> T waitFor(Supplier<T> input) {
        for (int spin = 0; Predicates.isNull().apply(input.get());) {
            if (++spin % 10_000_000 == 0) System.err.print(".");
        }
        T result = input.get();
        System.err.println(result.getClass().getName());
        return result;
    }

    private class Change {
        public int transform;
        public double start, end;
        public char field;
    }

    private void parse() throws IOException {
        for (String line : Files.readLines(config, Charsets.UTF_8)) {
            Iterable<String> tokens = Splitter.on(' ').omitEmptyStrings().trimResults().split(line);
            if (Iterables.isEmpty(tokens)) continue;
            String type = Iterables.get(tokens, 0);
            if (type.equalsIgnoreCase("ifs")) {
                if (Iterables.size(tokens) != 2) {
                    throw new IllegalStateException("Parse error at 'ifs': " + line);
                }
                input = new File(Iterables.get(tokens, 1).replace("~", System.getProperty("user.home")));
            } else if (type.equalsIgnoreCase("save")) {
                if (Iterables.size(tokens) != 2) {
                    throw new IllegalStateException("Parse error at 'save': " + line);
                }
                output = new File(Iterables.get(tokens, 1).replace("~", System.getProperty("user.home")));
            } else if (type.equalsIgnoreCase("frames")) {
                if (Iterables.size(tokens) != 2) {
                    throw new IllegalStateException("Parse error at 'frames': " + line);
                }
                frames = Long.valueOf(Iterables.get(tokens, 1));
            } else if (type.equalsIgnoreCase("delay")) {
                if (Iterables.size(tokens) != 2) {
                    throw new IllegalStateException("Parse error at 'delay': " + line);
                }
                delay = Long.valueOf(Iterables.get(tokens, 1));
            } else if (type.equalsIgnoreCase("zoom")) {
                if (Iterables.size(tokens) != 4) {
                    throw new IllegalStateException("Parse error at 'zoom': " + line);
                }
                scale = Double.valueOf(Iterables.get(tokens, 1));
                centre = new Point2D.Double(
                        Double.valueOf(Iterables.get(tokens, 3)),
                        Double.valueOf(Iterables.get(tokens, 4)));
            } else if (type.equalsIgnoreCase("transform")) {
                if (Iterables.size(tokens) != 5) {
                    throw new IllegalStateException("Parse error at 'transform': " + line);
                }
                Change change = new Change();
                change.transform = Integer.valueOf(Iterables.get(tokens, 1));
                String field = Iterables.get(tokens, 2);
                if (field.length() == 1 && CharMatcher.anyOf("xywhr").matches(field.charAt(0))) {
                    change.field = field.charAt(0);
                } else {
                    throw new IllegalStateException("Parse error at 'transform' field: " + line);
                }
                change.start = Double.valueOf(Iterables.get(tokens, 3));
                change.end = Double.valueOf(Iterables.get(tokens, 4));
                list.add(change);
            } else if (type.equalsIgnoreCase("segment")) {
                if (Iterables.size(tokens) == 2) {
                    segment = Long.valueOf(Iterables.get(tokens, 1));
                } else {
                    segment = frames;
                }
                list.clear();
            } else if (type.equalsIgnoreCase("end")) {
                if (Iterables.size(tokens) != 1) {
                    throw new IllegalStateException("Parse error at 'end': " + line);
                }
                segments.put(ImmutableList.copyOf(list), segment);
            } else if (type.startsWith("#")) {
                continue;
            } else {
                throw new IllegalStateException("Parse error: " + line);
            }
        }

        // Deal with single segment case (no 'segment' or 'end' token)
        if (segments.isEmpty() && list.size() > 0) {
            segments.put(ImmutableList.copyOf(list), frames);
        }
    }

    @Subscribe
    @Override
    public void updated(IFS ifs) {
        ready.countDown();
    }

    @Subscribe
    @Override
    public void resized(Dimension size) {
        this.size = size;
    }

    public void start() throws Exception {
        ready.await();

        explorer.show(Explorer.VIEWER);
        Thread.sleep(delay);

        Viewer viewer = explorer.getViewer();
        ifs = explorer.load(input);
        ifs.setSize(size);

        long frame = 0;
        for (Map.Entry<List<Change>, Long> segment : segments.entrySet()) {
            long length = segment.getValue();
            for (int i = 0; i < length; i++) {
                double fraction = (double) i / (double) length;
                for (Change change : segment.getKey()) {
                    Transform transform = ifs.get(change.transform);
                    if (transform.isMatrix()) {
                        throw new UnsupportedOperationException("Cannot animate matrix transforms currently");
                    }
                    double delta = (change.end - change.start) * fraction;
                    switch (change.field) {
                    case 'x': transform.x = change.start + delta; break;
                    case 'y': transform.y = change.start + delta; break;
                    case 'w': transform.w = change.start + delta; break;
                    case 'h': transform.h = change.start + delta; break;
                    case 'r': transform.r = Math.toRadians(change.start + delta); break;
                    }
                }
                bus.post(ifs);
                viewer.reset();
                viewer.start();
                Thread.sleep(delay);
                explorer.getViewer().save(new File(output, String.format("%04d.png", frame++)));
                viewer.stop();
            }
        }

        System.exit(0);
    }

    /**
     * Animator.
     */
    public static void main(final String...argv) throws Exception {
        Animator animator = new Animator(argv);
        animator.start();
    }

}
