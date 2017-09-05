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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static iterator.Utils.saveImage;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;

import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Subscriber;
import iterator.view.Viewer;

/**
 * IFS Animator.
 */
public class Animator implements Subscriber {

    private Explorer explorer;
    private IFS ifs;
    private EventBus bus;
    private Dimension size;
    private CountDownLatch ready = new CountDownLatch(3);
    private long delay = 500l, iterations = -1l, frames = 1000l, segment;
    private float scale = 1f;
    private Point2D centre = null;
    private File input, output;
    private Map<List<Change>, Long> segments = Maps.newLinkedHashMap();

    public Animator(String configFile) throws Exception {
        File config = new File(configFile);
        checkArgument(config.exists(), "Config file '%s' not found", config);
        parse(config);

        // Check input and output settings
        checkNotNull(input, "Input file must be set");
        checkState(input.exists(), "Input file '%s' not found", input);
        checkNotNull(output, "Output location must be set");
        if (output.exists()) {
            checkState(output.isDirectory(), "Output location '%s' not a direcotry", output);
        } else {
            output.mkdirs();
        }

        // Start application
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                explorer = new Explorer();
                explorer.start();
                bus = explorer.getEventBus();
            }
        });
 
        // Register ourselves with the application bus
        bus.register(this);
    }

    /**
     * A data object holding the changes made to a {@link Transform} during a segment.
     */
    public static class Change {
        public int transform;
        public double start, end;
        public String field;
    }

    /**
     * Parse the animation configuration file.
     * <p>
     * See the online documentation for more details. The format is generally as shown below:
     * <pre>
     * {@code # comment
     * ifs file
     * save directory
     * frames count
     * delay ms
     * iterations thousands
     * zoom scale centrex centrey
     * segment frames
     *     transform id field start finish
     * end}
     * </pre>
     *
     * @see <a href="http://grkvlt.github.io/iterator/">online documentation</a>
     * @throws IOException
     * @throws IllegalStateException
     * @throws NumberFormatException
     */
    public void parse(File config) throws IOException {
        List<Change> list = Lists.newArrayList();
        List<String> lines = Files.readLines(config, Charsets.UTF_8);
        for (int l = 0; l < lines.size(); l++) {
            String line = lines.get(l);
            List<String> tokens = Splitter.on(' ').omitEmptyStrings().trimResults().splitToList(line);
            if (tokens.isEmpty()) continue;
            String type = tokens.get(0).toLowerCase(Locale.UK);
            int args = tokens.size() - 1;
            if (type.startsWith("#")) {
                continue; // comment
            }
            checkArgs(type, args, l);
            switch (type) {
                case "ifs": // file
                    input = new File(tokens.get(1).replace("~", StandardSystemProperty.USER_HOME.value()));
                    break;
                case "save": // directory
                    output = new File(tokens.get(1).replace("~", StandardSystemProperty.USER_HOME.value()));
                    break;
                case "frames": // count
                    frames = Long.valueOf(tokens.get(1));
                    break;
                case "delay": // ms
                    delay = Long.valueOf(tokens.get(1));
                    break;
                case "iterations": // thousands
                    iterations = Long.valueOf(tokens.get(1));
                    break;
                case "zoom": // scale centrex centrey
                    scale = Float.valueOf(tokens.get(1));
                    centre = new Point2D.Double(
                            Double.valueOf(tokens.get(2)),
                            Double.valueOf(tokens.get(3)));
                    break;
                case "transform": // id field start finish
                    Change change = new Change();
                    change.transform = Integer.valueOf(tokens.get(1));
                    String f = tokens.get(2).toLowerCase(Locale.UK);
                    if (FIELDS.contains(f)) {
                        change.field = f;
                    } else {
                        throw new IllegalStateException(String.format("Parse error: Invalid 'transform' field %s at line %d", f, l));
                    }
                    change.start = Double.valueOf(tokens.get(3));
                    change.end = Double.valueOf(tokens.get(4));
                    list.add(change);
                    break;
                case "segment": // frames?
                    if (list.size() > 0) {
                        throw new IllegalStateException(String.format("Parse error: Segments cannot be nested at line %d", l));
                    }
                    if (args == 1) {
                        segment = Long.valueOf(tokens.get(1));
                    } else {
                        segment = frames;
                    }
                    break;
                case "end":
                    if (list.isEmpty()) {
                        throw new IllegalStateException(String.format("Parse error: Cannot end an empty segment at line %d", l));
                    }
                    segments.put(ImmutableList.copyOf(list), segment);
                    list.clear();
                    break;
                default:
                    throw new IllegalStateException(String.format("Parse error: Unknown directive '%s' at line %d", type, l));
            }
        }

        // Deal with single segment case (no 'segment' or 'end' token)
        if (segments.isEmpty() && list.size() > 0) {
            segments.put(ImmutableList.copyOf(list), frames);
        }
    }

    private static final Map<String, Boolean> OPTIONAL_ARGUMENTS = ImmutableMap.<String, Boolean>builder()
            .put("segment", true)
            .build();
    private static final Function<String, Boolean> OPTIONAL = Functions.forMap(OPTIONAL_ARGUMENTS, false);

    private static final Map<String, Integer> NUMBER_ARGUMENTS = ImmutableMap.<String, Integer>builder()
            .put("ifs", 1)
            .put("save", 1)
            .put("frames", 1)
            .put("delay", 1)
            .put("iterations", 1)
            .put("zoom", 3)
            .put("transform", 4)
            .put("segment", 1)
            .build();
    private static final Function<String, Integer> NUMBER = Functions.forMap(NUMBER_ARGUMENTS, 0);

    private static final List<String> FIELDS = Arrays.asList("x", "y", "w", "h", "r", "shx", "shy");

    private void checkArgs(String type, int args, int l) {
        int n = NUMBER.apply(type);
        boolean optional = OPTIONAL.apply(type);
        if ((optional && args != 0) && args != n) {
            String message = String.format("Parse error: Directive '%s' requires %d arguments, found %d at line %d", type, n, args, l);
            throw new IllegalStateException(message);
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

    /**
     * Generate the set of animation frames.
     */
    public void start() throws Exception {
        ready.await();

        // Load the IFS
        ifs = explorer.load(input);
        ifs.setSize(size);

        // Show and reset the viewer
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                explorer.show(Explorer.VIEWER);
            }
        });
        final Viewer viewer = explorer.getViewer();

        // Run the animation segments
        long frame = 0;
        for (Map.Entry<List<Change>, Long> segment : segments.entrySet()) {
            long length = segment.getValue();

            // Frame sequence for a segment
            for (int i = 0; i < length; i++) {
                viewer.stop();
                double fraction = (double) i / (double) length;

                // Set of changes for a single frame
                for (Change change : segment.getKey()) {
                    Transform transform = ifs.getTransforms().get(change.transform);
                    if (transform.isMatrix()) {
                        throw new UnsupportedOperationException("Cannot animate matrix transforms currently");
                    }
                    double delta = (change.end - change.start) * fraction;
                    switch (change.field) {
                        case "x": transform.x = change.start + delta; break;
                        case "y": transform.y = change.start + delta; break;
                        case "w": transform.w = change.start + delta; break;
                        case "h": transform.h = change.start + delta; break;
                        case "r": transform.r = Math.toRadians(change.start + delta); break;
                        case "shx": transform.shx = change.start + delta; break;
                        case "shy": transform.shy = change.start + delta; break;
                    }
                }

                // Update the viewer
                bus.post(ifs);

                // Rescale
                if (centre != null) {
                    viewer.rescale(scale, centre);
                }

                // render for required time/iterations before saving frame
                viewer.start();
                if (iterations > 0) {
                    while (viewer.getCount() < iterations) {
                        Thread.sleep(10l);
                    }
                } else {
                    Thread.sleep(delay);
                }
                saveImage(viewer.getImage(), new File(output, String.format("%04d.png", frame++)));
            }
        }

        System.exit(0);
    }

    /**
     * Animator.
     */
    public static void main(final String...argv) throws Exception {
        if (argv.length != 1) {
            throw new IllegalArgumentException("Must provide animation configuration file as only argument");
        }
        Animator animator = new Animator(argv[0]);
        animator.start();
    }

}
