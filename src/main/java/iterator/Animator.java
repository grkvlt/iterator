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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * IFS Animator.
 */
public class Animator implements Subscriber {
    private Explorer explorer;
    private IFS ifs;
    private EventBus bus;
    private Dimension size;
    private CountDownLatch ready = new CountDownLatch(3);
    private long frames = 1000l, delay = 500l;
    private File config, input, output;
    private List<Change> changes = Lists.newArrayList();

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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Start application
                explorer = new Explorer();
                explorer.start();
            }
        });

        long spin = 0;
        while (explorer == null) {
            if (++spin % 10_000_000 == 0) System.err.print(".");
        }
        System.err.println("explorer");
        while (bus == null) {
            if (++spin % 10_000_000 == 0) System.err.print(".");
            bus = explorer.getEventBus();
        }
        System.err.println("bus");
        bus.register(this);
    }

    private class Change {
        public int transform;
        public double start, end;
        public char field;
    }
    
    private void parse() throws IOException {
        Files.readLines(config, Charsets.UTF_8, new LineProcessor<Void>() {
            @Override
            public boolean processLine(String line) throws IOException {
                Iterable<String> tokens = Splitter.on(' ').split(line);
                String type = Iterables.get(tokens, 0);
                if (type.equalsIgnoreCase("ifs")) {
                    if (Iterables.size(tokens) != 2) {
                        throw new IllegalStateException("ifs");
                    }
                    input = new File(Iterables.get(tokens, 1).replace("~", System.getProperty("user.home")));
                } else if (type.equalsIgnoreCase("save")) {
                    if (Iterables.size(tokens) != 2) {
                        throw new IllegalStateException("save");
                    }
                    output = new File(Iterables.get(tokens, 1).replace("~", System.getProperty("user.home")));
                } else if (type.equalsIgnoreCase("frames")) {
                    if (Iterables.size(tokens) != 2) {
                        throw new IllegalStateException("frames");
                    }
                    frames = Long.valueOf(Iterables.get(tokens, 1));
                } else if (type.equalsIgnoreCase("delay")) {
                    if (Iterables.size(tokens) != 2) {
                        throw new IllegalStateException("delay");
                    }
                    delay = Long.valueOf(Iterables.get(tokens, 1));
                } else if (type.equalsIgnoreCase("transform")) {
                    if (Iterables.size(tokens) != 5) {
                        throw new IllegalStateException("transform");
                    }
                    Change change = new Change();
                    change.transform = Integer.valueOf(Iterables.get(tokens, 1));
                    String field = Iterables.get(tokens, 2);
                    if (field.length() == 1 && CharMatcher.anyOf("xywhr").matches(field.charAt(0))) {
                        change.field = field.charAt(0);
                    } else {
                        throw new IllegalStateException("transform field");
                    }
                    change.start = Double.valueOf(Iterables.get(tokens, 3));
                    change.end = Double.valueOf(Iterables.get(tokens, 4));
                    changes.add(change);
                }
                return true;
            }

            @Override
            public Void getResult() { return null; }
        });
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
        ifs = explorer.load(input);

        for (int i = 0; i < frames; i++) {
            double fraction = (double) i / (double) frames;
            for (Change change : changes) {
                Transform transform = ifs.get(change.transform);
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
            Thread.sleep(delay);
            explorer.getViewer().save(new File(output, String.format("%04d.png", i)));
        }

        System.exit(0);
    }

    /**
     * Explorer.
     */
    public static void main(final String...argv) throws Exception {
        Animator animator = new Animator(argv);
        animator.start();
    }

}
