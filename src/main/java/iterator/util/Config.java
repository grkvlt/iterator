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

import static iterator.Utils.NEWLINE;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import iterator.Explorer;
import iterator.dialog.Preferences;
import iterator.model.functions.CoordinateTransform;

/**
 * A global {@link Map} for preferences and configuration.
 * <p>
 * Preferences are loaded from configuration files and the JVM system
 * properties, using the values here as defaults. The configuration can also
 * be saved to a {@code .properties} file containing all the non-default
 * values that have been set during program operation.
 * <p>
 * The {@link Explorer controller} mediates access to the configuration
 * data, and it should not accessed directly.
 *
 * @see Explorer#start()
 * @see Preferences
 * @see Preferences#onSuccess()
 */
public class Config extends ForwardingSortedMap<String, String> {

    public static final String PROPERTIES_FILE = "explorer.properties";

    public static final String EXPLORER_PROPERTY = "explorer";
    public static final String MODE_PROPERTY = EXPLORER_PROPERTY + ".mode";
    public static final String RENDER_PROPERTY = EXPLORER_PROPERTY + ".render";
    public static final String TRANSFORM_PROPERTY = EXPLORER_PROPERTY + ".transform";
    public static final String GAMMA_PROPERTY = EXPLORER_PROPERTY + ".gamma";
    public static final String VIBRANCY_PROPERTY = EXPLORER_PROPERTY + ".vibrancy";
    public static final String BLUR_KERNEL_PROPERTY = EXPLORER_PROPERTY + ".blur";
    public static final String PALETTE_PROPERTY = EXPLORER_PROPERTY + ".palette";
    public static final String PALETTE_SEED_PROPERTY = PALETTE_PROPERTY + ".seed";
    public static final String PALETTE_FILE_PROPERTY = PALETTE_PROPERTY + ".file";
    public static final String PALETTE_SIZE_PROPERTY = PALETTE_PROPERTY + ".size";
    public static final String GRID_PROPERTY =  EXPLORER_PROPERTY + ".grid";
    public static final String GRID_MIN_PROPERTY = GRID_PROPERTY + ".min";
    public static final String GRID_MAX_PROPERTY = GRID_PROPERTY + ".max";
    public static final String GRID_SNAP_PROPERTY = GRID_PROPERTY + ".snap";
    public static final String WINDOW_PROPERTY = EXPLORER_PROPERTY + ".window";
    public static final String WINDOW_WIDTH_PROPERTY = WINDOW_PROPERTY + ".width";
    public static final String WINDOW_HEIGHT_PROPERTY = WINDOW_PROPERTY + ".height";
    public static final String DEBUG_PROPERTY = EXPLORER_PROPERTY + ".debug";
    public static final String THREADS_PROPERTY = EXPLORER_PROPERTY + ".threads";
    public static final String ITERATIONS_PROPERTY = EXPLORER_PROPERTY + ".iterations";
    public static final String ITERATIONS_LIMIT_PROPERTY = ITERATIONS_PROPERTY + ".limit";
    public static final String ITERATIONS_UNLIMITED_PROPERTY = ITERATIONS_PROPERTY + ".unlimited";

    public static final Mode DEFAULT_MODE = Mode.GRAY;
    public static final Render DEFAULT_RENDER = Render.STANDARD;
    public static final CoordinateTransform.Type DEFAULT_TRANSFORM = CoordinateTransform.Type.IDENTITY;
    public static final Float DEFAULT_GAMMA = 1.8f;
    public static final Float DEFAULT_VIBRANCY = 0.9f;
    public static final Integer DEFAULT_BLUR_KERNEL = 4;
    public static final String[] PALETTE_FILES = { "abstract", "autumn", "car", "car2", "forest", "lego", "night", "trees", "wave" };
    public static final String DEFAULT_PALETTE_FILE = "abstract";
    public static final Integer DEFAULT_PALETTE_SIZE = 64;
    public static final Integer MIN_PALETTE_SIZE = 16;
    public static final Integer MAX_PALETTE_SIZE = 255;
    public static final Long DEFAULT_PALETTE_SEED = 0l;
    public static final Integer DEFAULT_GRID_MIN = 10;
    public static final Integer DEFAULT_GRID_MAX = 50;
    public static final Integer DEFAULT_GRID_SNAP = 5;
    public static final Integer DEFAULT_WINDOW_SIZE = 600;
    public static final Long DEFAULT_ITERATIONS = 10_000l;
    public static final Long DEFAULT_ITERATIONS_LIMIT = 10_000_000l;
    public static final Integer MIN_WINDOW_SIZE = 400; // Details view requires 350px
    public static final Integer MIN_THREADS = 2;
    public static final Boolean DEFAULT_DEBUG = false;
    public static final Boolean DEFAULT_ITERATIONS_UNLIMITED = true;

    public static final List<String> FOOTER = Arrays.asList(
            "#",
            "# Generated on %s by %s",
            "##",
            "");

    public static enum Mode {
        COLOUR(true, false, false, false),
        PALETTE(true, true, false, false),
        STEALING(true, true, true, false),
        IFS_COLOUR(true, false, false, true),
        GRAY(false, false, false, false);

        private final boolean colour, palette, stealing, ifscolour;

        public boolean isColour() { return colour; }

        public boolean isPalette() { return palette; }

        public boolean isStealing() { return stealing; }

        public boolean isIFSColour() { return ifscolour; }

        private Mode(boolean colour, boolean palette, boolean stealing, boolean ifscolour) {
            this.colour = colour;
            this.palette = palette;
            this.stealing = stealing;
            this.ifscolour = ifscolour;
        }

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
        }
    }

    public static enum Render {
        STANDARD(false, false, false),
        TOP(false, false, false),
        MEASURE(true, false, false),
        IFS(false, false, false),
        DENSITY(false, true, false),
        DENSITY_POWER(false, true, false),
        LOG_DENSITY(false, true, true),
        LOG_DENSITY_POWER(false, true, true),
        LOG_DENSITY_POWER_INVERSE(true, true, true),
        LOG_DENSITY_INVERSE(true, true, true),
        LOG_DENSITY_BLUR(false, true, true),
        LOG_DENSITY_BLUR_INVERSE(true, true, true),
        LOG_DENSITY_FLAME(false, true, true),
        LOG_DENSITY_FLAME_INVERSE(true, true, true);

        private final boolean inverse;
        private final boolean density;
        private final boolean log;

        public Color getBackground() { return inverse ? Color.BLACK : Color.WHITE; }

        public Color getForeground() { return inverse ? Color.WHITE : Color.BLACK; }

        public boolean isInverse() { return inverse; }

        public boolean isDensity() { return density; }

        public boolean isLog() { return log; }

        private Render(boolean inverse, boolean density, boolean log) {
            this.inverse = inverse;
            this.density = density;
            this.log = log;
        }

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
        }
    }

    public static final Predicate<CharSequence> EXPLORER_KEYS = Predicates.containsPattern("^" + EXPLORER_PROPERTY + ".");

    private final Optional<Path> override;
    private final SortedMap<String, String> config;

    private Config(Path override) {
        this.override = Optional.fromNullable(override);
        this.config = Maps.newTreeMap();
    }

    public static Config loadProperties(Path override) {
        Config instance = new Config(override);
        instance.load();
        return instance;
    }

    public SortedMap<String, String> copyOf() {
        return ImmutableSortedMap.copyOfSorted(this);
    }

    public void load() {
        clear();

        // Defaults from classpath
        load(Resources.getResource(PROPERTIES_FILE));

        // Configuration from home directory
        load(Paths.get(StandardSystemProperty.USER_HOME.value(), "." + PROPERTIES_FILE));

        // Configuration from current directory
        load(Paths.get(PROPERTIES_FILE));

        // Override file from command line
        if (override.isPresent()) {
            load(override.get());
        }

        // Finally load system properties (JAVA_OPTS)
        load(System.getProperties());
    }

    public void load(Path path) {
        if (Files.isReadable(path)) {
            try (Reader reader = Files.newBufferedReader(path, Charsets.UTF_8)) {
                load(reader);
            } catch (IOException ioe) {
                throw Throwables.propagate(ioe);
            }
        }
    }

    public void load(URL url) {
        try (Reader reader = new InputStreamReader(url.openStream())) {
            load(reader);
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }

    public void load(Reader reader) {
        try {
            Properties properties = new Properties();
            properties.load(reader);
            load(properties);
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }

    public void load(Properties properties) {
        load(Maps.fromProperties(properties));
    }

    public void load(Map<String, String> data) {
        putAll(Maps.filterKeys(data, EXPLORER_KEYS));
    }

    public void save(OutputStream stream) {
        String header = Explorer.BANNER.stream()
                .map("# "::concat)
                .map(String::trim)
                .collect(Collectors.joining(NEWLINE));
        String version = Version.instance().get();
        String footer = Joiner.on(NEWLINE).join(FOOTER);
        String timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        String user = StandardSystemProperty.USER_NAME.value();

        try (Writer writer = new OutputStreamWriter(stream, Charsets.UTF_8)) {
            writer.append("##")
                  .append(NEWLINE)
                  .append(String.format(header, version))
                  .append(NEWLINE)
                  .append(Joiner.on(NEWLINE).withKeyValueSeparator(" = ").join(this))
                  .append(NEWLINE)
                  .append(String.format(footer, timestamp, user));
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }

    public void save(File file) {
        Path path = Paths.get(file.getAbsolutePath());
        if (Files.exists(path) && !Files.isWritable(path)) {
            throw new IllegalStateException(String.format("Cannot write file %s", path));
        }
        try {
            save(Files.newOutputStream(path));
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return (T) cast(value, defaultValue.getClass());
        }
    }

    public void set(String key, Object value) {
        put(key, Objects.toString(value));
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(String value, Class<T> type) {
        if (Integer.class == type || Integer.TYPE == type) {
            return (T) Integer.valueOf(value);
        } else if (Long.class == type || Long.TYPE == type) {
            return (T) Long.valueOf(value);
        } else if (Float.class == type || Float.TYPE == type) {
            return (T) Float.valueOf(value);
        } else if (Double.class == type || Double.TYPE == type) {
            return (T) Double.valueOf(value);
        } else if (Short.class == type || Short.TYPE == type) {
            return (T) Short.valueOf(value);
        } else if (Byte.class == type || Byte.TYPE == type) {
            return (T) Byte.valueOf(value);
        } else if (Character.class == type || Character.TYPE == type) {
            if (value.length() != 1) {
                throw new IllegalArgumentException(String.format("Bad Character %s", value));
            }
            return (T) Character.valueOf(value.charAt(0));
        } else if (Boolean.class == type || Boolean.TYPE == type) {
            return (T) Boolean.valueOf(value);
        } else if (String.class == type) {
            return (T) value;
        } else if (Enum.class.isAssignableFrom(type)) {
            return (T) Enum.valueOf((Class<Enum>) type, CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, value));
        } else {
            throw new IllegalArgumentException(String.format("Cannot cast %s to %s", value, type.getName()));
        }
    }

    @Override
    protected SortedMap<String, String> delegate() {
        return config;
    }

    @Override
    public String toString() {
        return Joiner.on(",").useForNull("").withKeyValueSeparator("=").join(this);
    }

}
