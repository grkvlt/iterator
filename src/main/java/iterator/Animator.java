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

import static iterator.Utils.NEWLINE;
import static iterator.Utils.saveImage;
import static iterator.Utils.version;
import static iterator.util.Config.CONFIG_OPTION;
import static iterator.util.Config.CONFIG_OPTION_LONG;
import static iterator.util.Config.EXPLORER_PROPERTY;
import static iterator.util.Config.MIN_WINDOW_SIZE;
import static iterator.util.Config.OUTPUT_OPTION;
import static iterator.util.Config.OUTPUT_OPTION_LONG;
import static iterator.util.Config.PALETTE_OPTION;
import static iterator.util.Config.PALETTE_OPTION_LONG;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.Config;
import iterator.util.Output;
import iterator.view.Iterator;

/**
 * IFS Animator main class.
 */
public class Animator implements BiConsumer<Throwable, String> {

    public static final List<String> BANNER = Arrays.asList(
            "   ___ _____ ____       _          _                 _",
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

    /**
     * Tokens for the {@code .animation} file.
     */

    public static final String SEGMENT = "segment";
    public static final String SOURCE = "source";
    public static final String FRAMES = "frames";
    public static final String TRANSFORM = "transform";
    public static final String REFLECTION = "reflection";
    public static final String CONFIG = "config";
    public static final String END = "end";

    public static final String X = "x";
    public static final String Y = "y";
    public static final String W = "w";
    public static final String H = "h";
    public static final String R = "r";
    public static final String SHX = "shx";
    public static final String SHY = "shy";

    public static final Map<String, Boolean> OPTIONAL_ARGUMENTS = ImmutableMap.<String, Boolean>builder()
            .put(SEGMENT, true)
            .build();

    public static final Map<String, Integer> NUMBER_ARGUMENTS = ImmutableMap.<String, Integer>builder()
            .put(SOURCE, 1)
            .put(FRAMES, 1)
            .put(TRANSFORM, 4)
            .put(REFLECTION, 4)
            .put(SEGMENT, 1)
            .put(CONFIG, 2)
            .build();

    public static final Function<String, Boolean> OPTIONAL = s -> OPTIONAL_ARGUMENTS.getOrDefault(s, false);
    public static final Function<String, Integer> NUMBER = s -> NUMBER_ARGUMENTS.getOrDefault(s, 0);

    public static final Map<String, List<String>> FIELDS = ImmutableMap.<String, List<String>>builder()
            .put(TRANSFORM, Arrays.asList(X, Y, W, H, R, SHX, SHY))
            .put(REFLECTION, Arrays.asList(X, Y, R))
            .build();

    /**
     * Data objects holding the changes made to a {@link Transform} during a segment.
     */

    public static class Change {
        public String type;
        public int function;
        public double start, end;
        public String field;
    }

    public static class Segment {
        public List<Change> changes;
        public Map<String,String> config;
        public long frames;
    }

    private static final long DEFAULT_FRAMES = 1000L;

    private Config config;
    private Output out = new Output();
    private Dimension size;
    private String paletteFile;
    private Path override, input, output;
    private long frames = DEFAULT_FRAMES;
    private List<Segment> segments = Lists.newArrayList();

    public Animator(String...argv) throws Exception {
        // Parse arguments
        if (argv.length < 1) {
            out.error("Must have at least one argument");
        }
        for (int i = 0; i < argv.length - 1; i++) {
            // Argument is a program option
            if (argv[i].charAt(0) == '-') {
                if (argv[i].equalsIgnoreCase(PALETTE_OPTION) ||
                        argv[i].equalsIgnoreCase(PALETTE_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        paletteFile = argv[++i];
                    } else {
                        out.error("Palette argument not provided");
                    }
                } else if (argv[i].equalsIgnoreCase(CONFIG_OPTION) ||
                        argv[i].equalsIgnoreCase(CONFIG_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        override = Paths.get(argv[++i]);
                        if (Files.notExists(override)) {
                            out.error("Configuration file does not exist: %s", override);
                        }
                    } else {
                        out.error("Configuration file argument not provided");
                    }
                } else if (argv[i].equalsIgnoreCase(OUTPUT_OPTION) ||
                        argv[i].equalsIgnoreCase(OUTPUT_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        output = Paths.get(argv[++i]);
                    } else {
                        out.error("Output directory argument not provided");
                    }
                } else {
                    out.error("Cannot parse option: %s", argv[i]);
                }
            }
        }

        // Verify output location
        if (output == null) {
            out.error("Output location must be set");
        } else {
            if (Files.exists(output)) {
                if (!Files.isDirectory(output)) {
                    out.error("Output location '%s' not a direcotry", output);
                }
            } else {
                Files.createDirectories(output);
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
        if (!Files.exists(animation)) {
            out.error("Animation file '%s' not found", animation);
        }
        parse(animation);

        // Check input file settings
        if (input == null) {
            out.error("IFS input file must be set");
        } else {
            if (!Files.exists(input)) {
                out.error("IFS input file '%s' not found", input);
            }
        }
    }

    /**
     * Parse the animation configuration file.
     * <p>
     * See the online documentation for more details. The format is generally as shown below:
     * <pre>
     * {@code # comment
     * source file
     * frames count
     * segment frames
     *     config key value
     *     transform id field start finish
     *     reflection id field start finish
     * end}
     * </pre>
     *
     * @throws IOException on IO errors
     * @throws IllegalStateException on unknown state
     * @throws NumberFormatException on incorrect number format
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
            String type = tokens.get(0).toLowerCase(Locale.ROOT);
            int args = tokens.size() - 1;
            if (type.startsWith("#")) {
                continue; // comment
            }
            checkArgs(type, args, l);
            switch (type) {
                case SOURCE: // file
                    input = Paths.get(tokens.get(1).replace("~", Objects.requireNonNull(StandardSystemProperty.USER_HOME.value())));
                    break;
                case FRAMES: // count
                    frames = Long.parseLong(tokens.get(1));
                    break;
                case TRANSFORM: // id field start finish
                case REFLECTION:
                    Change change = new Change();
                    change.type = tokens.get(0);
                    change.function = Integer.parseInt(tokens.get(1));
                    String f = tokens.get(2).toLowerCase(Locale.ROOT);
                    if (FIELDS.get(change.type).contains(f)) {
                        change.field = f;
                    } else {
                        out.error("Parse error: Invalid function field %s at line %d", f, l);
                    }
                    change.start = Double.parseDouble(tokens.get(3));
                    change.end = Double.parseDouble(tokens.get(4));
                    changes.add(change);
                    break;
                case CONFIG: // key value
                    String key = tokens.get(1);
                    if (!key.startsWith(EXPLORER_PROPERTY)) {
                        key = EXPLORER_PROPERTY + "." + key;
                    }
                    String value = tokens.get(2);
                    configuration.put(key,  value);
                    break;
                case SEGMENT: // frames?
                    if (changes.size() > 0) {
                        out.error("Parse error: Segments cannot be nested at line %d", l);
                    }
                    if (args == 1) {
                        length = Long.parseLong(tokens.get(1));
                    } else {
                        length = frames;
                    }
                    break;
                case END:
                    if (changes.isEmpty()) {
                        out.error("Parse error: Cannot end an empty segment at line %d", l);
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
                    out.error("Parse error: Unknown directive '%s' at line %d", type, l);
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

    private void checkArgs(String type, int args, int l) {
        int n = NUMBER.apply(type);
        boolean optional = OPTIONAL.apply(type);
        if ((optional && args != 0) && args != n) {
            out.error("Parse error: Directive '%s' requires %d arguments, found %d at line %d", type, n, args, l);
        }
    }

    /**
     * Generate the set of animation frames.
     */
    public void start() {
        out.timestamp("Started");

        // Update config
        if (config.isIterationsUnlimited()) {
            config.setIterationsUnimited(false);
        }

        // Load the IFS
        IFS ifs = IFS.load(input.toFile());
        ifs.setSize(size);

        // Initialize iterator
        Iterator iterator = new Iterator(this, config, size);

        long total = segments.stream()
                .map(s -> s.frames)
                .reduce(Long::sum)
                .orElse(0L);
        out.print("Generating %d frames", total);

        // Run the animation segments
        long frame = 0;
        int id = 0;
        for (Segment segment : segments) {
            long length = segment.frames;
            id++;

            // Config for this segment
            config.putAll(segment.config);
            out.print("Segment %d", id);
            out.print(iterator.getInfo());

            // Frame sequence for a segment
            for (int i = 0; i < length; i++) {
                double fraction = (double) i / (double) length;

                // Set of changes for a single frame
                for (Change change : segment.changes) {
                    double delta = (change.end - change.start) * fraction;
                    switch (change.type) {
                        case REFLECTION:
                            Reflection reflection = ifs.getReflections().get(change.function);
                            switch (change.field) {
                                case X: reflection.x = (int) (change.start + delta); break;
                                case Y: reflection.y = (int) (change.start + delta); break;
                                case R: reflection.r = Math.toRadians(change.start + delta); break;
                            }
                            break;
                        case TRANSFORM:
                            Transform transform = ifs.getTransforms().get(change.function);
                            if (transform.isMatrix()) {
                                out.error("Cannot modify transform %d (matrix)", change.function);
                            }
                            switch (change.field) {
                                case X: transform.x = (int) (change.start + delta); break;
                                case Y: transform.y = (int) (change.start + delta); break;
                                case W: transform.w = change.start + delta; break;
                                case H: transform.h = change.start + delta; break;
                                case R: transform.r = Math.toRadians(change.start + delta); break;
                                case SHX: transform.shx = change.start + delta; break;
                                case SHY: transform.shy = change.start + delta; break;
                            }
                            break;
                    }
                }

                // Render for required iterations
                long limit = config.getIterationsLimit() / 1000L;
                iterator.reset(size);
                iterator.setTransforms(ifs);
                iterator.start();
                while (iterator.getCount() <= limit) {
                    Utils.sleep(100, TimeUnit.MILLISECONDS);
                    String countText = String.format("%,dK", Math.min(iterator.getCount(), limit)).replaceAll("[^0-9K+]", " ");
                    out.pause(countText);
                }
                iterator.stop();
                out.blank();

                // Save the image
                String image = String.format("%04d.png", frame++);
                saveImage(iterator.getImage(), output.resolve(image).toFile());
                out.stack("Saved %s", image);
            }
        }

        System.exit(0);
    }

    @Override
    public void accept(Throwable t, String message) {
        out.accept(t, message);
    }

    /**
     * Animator.
     */
    public static void main(final String...argv) throws Exception {
        String banner = Joiner.on(NEWLINE).join(BANNER);
        System.out.printf(banner, version());
        System.out.println();

        Animator animator = new Animator(argv);
        animator.start();
    }

}
