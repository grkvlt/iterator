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
import static iterator.Utils.NEWLINE;
import static iterator.Utils.saveImage;
import static iterator.util.Config.CONFIG_OPTION;
import static iterator.util.Config.CONFIG_OPTION_LONG;
import static iterator.util.Config.MIN_WINDOW_SIZE;
import static iterator.util.Config.PALETTE_OPTION;
import static iterator.util.Config.PALETTE_OPTION_LONG;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import iterator.model.IFS;
import iterator.model.Transform;
import iterator.util.Config;
import iterator.util.Version;
import iterator.view.Iterator;

/**
 * IFS Animator.
 */
public class Animator implements BiConsumer<Throwable, String> {

    public static final List<String> BANNER = Arrays.asList(
            "  ___ _____ ____       _          _                 _",
            "  |_ _|  ___/ ___|     / \\   _ __ (_)_ __ ___   __ _| |_ ___  _ __",
            "   | || |_  \\___ \\    / _ \\ | '_ \\| | '_ ` _ \\ / _` | __/ _ \\| '__|",
            "   | ||  _|  ___) |  / ___ \\| | | | | | | | | | (_| | || (_) | |",
            "  |___|_|   |____/  /_/   \\_\\_| |_|_|_| |_| |_|\\__,_|\\__\\___/|_|",
            "",
            "    Iterated Function System Animator %s",
            "",
            "    Copyright 2012-2017 by Andrew Donald Kennedy",
            "    Licensed under the Apache Software License, Version 2.0",
            "    Documentation at https://grkvlt.github.io/iterator/",
            "");

    public static final Version version = Version.instance();

    /**
     * Data objects holding the changes made to a {@link Transform} during a segment.
     */

    public static class Change {
        public int transform;
        public double start, end;
        public String field;
    }

    public static class Segment {
        public List<Change> changes;
        public Map<String,String> config;
        public long frames;
    }

    private static final long DEFAULT_FRAMES = 1000l;
    
    private IFS ifs;
    private Config config;
    private Dimension size;
    private String paletteFile;
    private Path override;
    private long iterations = -1l;
    private long frames = DEFAULT_FRAMES;
    private float scale = 1f;
    private Point2D centre = null;
    private File input, output;
    private List<Segment> segments = Lists.newArrayList();

    public Animator(String...argv) throws Exception {
        // Parse arguments
        if (argv.length < 1) {
            throw new IllegalArgumentException("Must have at least one argument");
        }
        for (int i = 0; i < argv.length - 2; i++) {
            if (argv[i].charAt(0) == '-') {
                // Argument is a program option
                if (argv[i].equalsIgnoreCase(PALETTE_OPTION) ||
                        argv[i].equalsIgnoreCase(PALETTE_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        paletteFile = argv[++i];
                    } else throw new IllegalArgumentException("Palette argument not provided");
                } else if (argv[i].equalsIgnoreCase(CONFIG_OPTION) ||
                        argv[i].equalsIgnoreCase(CONFIG_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        override = Paths.get(argv[++i]);
                        if (Files.notExists(override)) {
                            throw new IllegalArgumentException(String.format("Configuration file does not exist: %s", override));
                        }
                    } else throw new IllegalArgumentException("Configuration file argument not provided");
                } else {
                    throw new IllegalArgumentException(String.format("Cannot parse option: %s", argv[i]));
                }
            }
        }

        // Load configuration
        config = Config.loadProperties(override);
        if (!Strings.isNullOrEmpty(paletteFile)) {
            config.setPaletteFile(paletteFile);
        }

        // Load colour palette if required
        config.loadColours();

        // Get window size configuration
        int w = Math.max(MIN_WINDOW_SIZE, config.getWindowWidth());
        int h = Math.max(MIN_WINDOW_SIZE, config.getWindowHeight());
        size = new Dimension(w, h);

        // Animation file argument
        Path animation = Paths.get(argv[argv.length - 1]);
        checkArgument(Files.exists(animation), "Animation file '%s' not found", animation);
        parse(animation);

        // Check input and output settings
        checkNotNull(input, "Input file must be set");
        checkState(input.exists(), "Input file '%s' not found", input);
        checkNotNull(output, "Output location must be set");
        if (output.exists()) {
            checkState(output.isDirectory(), "Output location '%s' not a direcotry", output);
        } else {
            output.mkdirs();
        }
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
    public void parse(Path animation) throws IOException {
        List<Change> changes = Lists.newArrayList();
        Map<String,String> configuration = Maps.newHashMap();
        long length = frames;
        List<String> lines = Files.readAllLines(animation, Charsets.UTF_8);
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
                    changes.add(change);
                    break;
                case "config": // key value
                    String key = tokens.get(1);
                    String value = tokens.get(2);
                    configuration.put(key,  value);
                    break;
                case "segment": // frames?
                    if (changes.size() > 0) {
                        throw new IllegalStateException(String.format("Parse error: Segments cannot be nested at line %d", l));
                    }
                    if (args == 1) {
                        length = Long.valueOf(tokens.get(1));
                    } else {
                        length = frames;
                    }
                    break;
                case "end":
                    if (changes.isEmpty()) {
                        throw new IllegalStateException(String.format("Parse error: Cannot end an empty segment at line %d", l));
                    }
                    Segment segment = new Segment();
                    segment.changes = ImmutableList.copyOf(changes);
                    segment.config = ImmutableMap.copyOf(configuration);
                    segment.frames = length;
                    segments.add(segment);

                    changes.clear();
                    configuration.clear();
                    break;
                default:
                    throw new IllegalStateException(String.format("Parse error: Unknown directive '%s' at line %d", type, l));
            }
        }

        // Deal with single segment case (no 'segment' or 'end' token)
        if (segments.isEmpty() && changes.size() > 0) {
            Segment segment = new Segment();
            segment.changes = ImmutableList.copyOf(changes);
            segment.config = ImmutableMap.copyOf(configuration);
            segment.frames = frames;
            segments.add(segment);
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
            .put("iterations", 1)
            .put("zoom", 3)
            .put("transform", 4)
            .put("segment", 1)
            .put("config", 2)
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

    /**
     * Generate the set of animation frames.
     */
    public void start() throws Exception {
        // Update config
        if (config.isIterationsUnlimited()) {
            config.setIterationsUnimited(false);
        }
        config.setIterationsLimit(iterations);

        // Load the IFS
        ifs = IFS.load(input);
        ifs.setSize(size);

        // Initialize iterator
        Iterator iterator = new Iterator(this, config, size);

        // Run the animation segments
        long frame = 0;
        for (Segment segment : segments) {
            long length = segment.frames;

            // Config for this segment
            config.putAll(segment.config);

            // Frame sequence for a segment
            for (int i = 0; i < length; i++) {
                double fraction = (double) i / (double) length;

                // Set of changes for a single frame
                for (Change change : segment.changes) {
                    Transform transform = ifs.getTransforms().get(change.transform);
                    if (transform.isMatrix()) {
                        throw new UnsupportedOperationException("Cannot animate matrix transforms currently");
                    }
                    double delta = (change.end - change.start) * fraction;
                    switch (change.field) {
                        case "x": transform.x = (int) (change.start + delta); break;
                        case "y": transform.y = (int) (change.start + delta); break;
                        case "w": transform.w = change.start + delta; break;
                        case "h": transform.h = change.start + delta; break;
                        case "r": transform.r = Math.toRadians(change.start + delta); break;
                        case "shx": transform.shx = change.start + delta; break;
                        case "shy": transform.shy = change.start + delta; break;
                    }
                }

                // Rescale
                if (centre != null) {
                    config.setDisplayScale(scale);
                    config.setDisplayCentreX(centre.getX() / size.getWidth());
                    config.setDisplayCentreY(centre.getY() / size.getHeight());
                    iterator.rescale();
                }

                // Render for required iterations
                iterator.reset(size);
                iterator.setTransforms(ifs);
                iterator.start();
                while (iterator.getCount() <= iterations) {
                    Utils.sleep(100, TimeUnit.MILLISECONDS);
                    String countText = String.format("%,dK", iterator.getCount()).replaceAll("[^0-9K+]", " ");
                    System.out.printf("\r%s%s", Utils.PAUSE, countText);
                }
                iterator.stop();

                // Save the image
                String image = String.format("%04d.png", frame++);
                saveImage(iterator.getImage(), new File(output, image));
                System.out.printf("\r%sSaved %s\n", Utils.STACK, image);
            }
        }

        System.exit(0);
    }

    @Override
    public void accept(Throwable t, String message) {
        System.err.printf("%s%s: %s\n", Utils.ERROR, message, t);
        System.exit(1);
    }

    /**
     * Animator.
     */
    public static void main(final String...argv) throws Exception {
        String banner = Joiner.on(NEWLINE).join(BANNER);
        System.out.printf(banner, version.get());
        System.out.println();

        Animator animator = new Animator(argv);
        animator.start();
    }

}
