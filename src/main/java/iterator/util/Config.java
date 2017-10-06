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
import static iterator.Utils.clamp;
import static iterator.Utils.initFileSystem;
import static iterator.Utils.loadImage;
import static iterator.Utils.threads;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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

    public static final String FULLSCREEN_OPTION = "-f";
    public static final String FULLSCREEN_OPTION_LONG = "--fullscreen";
    public static final String PALETTE_OPTION = "-p";
    public static final String PALETTE_OPTION_LONG = "--palette";
    public static final String CONFIG_OPTION = "-c";
    public static final String CONFIG_OPTION_LONG = "--config";
    public static final String OUTPUT_OPTION = "-o";
    public static final String OUTPUT_OPTION_LONG = "--output";

    public static final String PROPERTIES_FILE = "explorer.properties";

    public static final String EXPLORER_PROPERTY = "explorer";
    public static final String MODE_PROPERTY = EXPLORER_PROPERTY + ".mode";
    public static final String RENDER_PROPERTY = EXPLORER_PROPERTY + ".render";
    public static final String TRANSFORM_PROPERTY = EXPLORER_PROPERTY + ".transform";
    public static final String GAMMA_PROPERTY = EXPLORER_PROPERTY + ".gamma";
    public static final String VIBRANCY_PROPERTY = EXPLORER_PROPERTY + ".vibrancy";
    public static final String BLUR_KERNEL_PROPERTY = EXPLORER_PROPERTY + ".blur";
    public static final String GRADIENT_PROPERTY = EXPLORER_PROPERTY + ".gradient";
    public static final String GRADIENT_COLOUR_PROPERTY = GRADIENT_PROPERTY + ".colour";
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
    public static final String DISPLAY_PROPERTY = EXPLORER_PROPERTY + ".display";
    public static final String DISPLAY_SCALE_PROPERTY = DISPLAY_PROPERTY + ".scale";
    public static final String DISPLAY_CENTRE_PROPERTY = DISPLAY_PROPERTY + ".centre";
    public static final String DISPLAY_CENTRE_X_PROPERTY = DISPLAY_CENTRE_PROPERTY + ".x";
    public static final String DISPLAY_CENTRE_Y_PROPERTY = DISPLAY_CENTRE_PROPERTY + ".y";
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
    public static final Color DEFAULT_GRADIENT_COLOUR = Color.RED;
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
    public static final Float DEFAULT_DISPLAY_SCALE = 1f;
    public static final Double DEFAULT_DISPLAY_CENTRE_X = 0.5d;
    public static final Double DEFAULT_DISPLAY_CENTRE_Y = 0.5d;
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
        GRADIENT(true, true, false, false),
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

    private BufferedImage source;
    private Set<Color> colours;

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
                throw new UncheckedIOException(ioe);
            }
        }
    }

    public void load(URL url) {
        try {
            URI uri = url.toURI();
            initFileSystem(uri);
            load(Paths.get(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(Reader reader) {
        try {
            Properties properties = new Properties();
            properties.load(reader);
            load(properties);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
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
            throw new UncheckedIOException(ioe);
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
            throw new UncheckedIOException(ioe);
        }
    }

    public Set<Color> getColours() { return colours; }

    public BufferedImage getSourceImage() { return source; }

    public void loadColours() {
        colours = Sets.newHashSet();
        if (getMode() == Mode.GRADIENT) {
            Color start = getRender().isInverse() ? Color.WHITE : Color.BLACK;
            Color end = getGradientColour();
            float startHSB[] = new float[3], endHSB[] = new float[3], deltaHSB[] = new float[3];
            Color.RGBtoHSB(start.getRed(), start.getBlue(), start.getGreen(), startHSB);
            Color.RGBtoHSB(end.getRed(), end.getBlue(), end.getGreen(), startHSB);
            deltaHSB[0] = (endHSB[0] - startHSB[0]) / (float) getPaletteSize();
            deltaHSB[1] = (endHSB[1] - startHSB[1]) / (float) getPaletteSize();
            deltaHSB[2] = (endHSB[2] - startHSB[2]) / (float) getPaletteSize();
            for (int i = 0; i < getPaletteSize(); i++) {
                Color c = Color.getHSBColor(startHSB[0] + i * deltaHSB[0], startHSB[1] + i * deltaHSB[1], startHSB[2] + i * deltaHSB[2]);
                colours.add(c);
            }
        } else {
            try {
                String file = getPaletteFile();
                if (file.contains(".")) {
                    source = loadImage(URI.create(file).toURL());
                } else {
                    source = loadImage(Resources.getResource("palette/" + file + ".png"));
                }
            } catch (MalformedURLException | RuntimeException e) {
                throw new IllegalStateException(String.format("Cannot load colour palette %s", getPaletteFile()));
            }
            Random random = new Random(getSeed());
            while (colours.size() < getPaletteSize()) {
                int x = random.nextInt(source.getWidth());
                int y = random.nextInt(source.getHeight());
                Color c = new Color(source.getRGB(x, y));
                colours.add(c);
            }
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
        } else if (Color.class == type) {
            return (T) Color.decode(value);
        } else if (Enum.class.isAssignableFrom(type)) {
            return (T) Enum.valueOf((Class<Enum>) type, CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, value));
        } else {
            throw new IllegalArgumentException(String.format("Cannot cast %s to %s", value, type.getName()));
        }
    }

    public void setThreads(int value) { set(THREADS_PROPERTY, threads().apply(value)); }

    public int getThreads() { return get(THREADS_PROPERTY, Math.max(Runtime.getRuntime().availableProcessors() / 2, MIN_THREADS)); }

    public void setDebug(boolean value) { set(DEBUG_PROPERTY, value); }

    public boolean isDebug() { return get(DEBUG_PROPERTY, DEFAULT_DEBUG); }

    public void setSeed(long value) { set(PALETTE_SEED_PROPERTY, value); }

    public long getSeed() { return get(PALETTE_SEED_PROPERTY, DEFAULT_PALETTE_SEED); }

    public void setPaletteFile(String value) { set(PALETTE_FILE_PROPERTY, value); }

    public String getPaletteFile() { return get(PALETTE_FILE_PROPERTY, DEFAULT_PALETTE_FILE); }

    public void setPaletteSize(int value) { set(PALETTE_SIZE_PROPERTY, clamp(MIN_PALETTE_SIZE, MAX_PALETTE_SIZE).apply(value)); }

    public int getPaletteSize() { return get(PALETTE_SIZE_PROPERTY, DEFAULT_PALETTE_SIZE); }

    public void setRender(Render value) { set(RENDER_PROPERTY, value); }

    public Render getRender() { return get(RENDER_PROPERTY, DEFAULT_RENDER); }

    public void setMode(Mode value) { set(MODE_PROPERTY, value); }

    public Mode getMode() { return get(MODE_PROPERTY, DEFAULT_MODE); }

    public void setCoordinateTransformType(CoordinateTransform.Type value) { set(TRANSFORM_PROPERTY, value); }

    public CoordinateTransform.Type getCoordinateTransformType() { return get(TRANSFORM_PROPERTY, DEFAULT_TRANSFORM); }

    public CoordinateTransform getCoordinateTransform() { return getCoordinateTransformType().getFunction(); }

    public void setGamma(float value) { set(GAMMA_PROPERTY, value); }

    public float getGamma() { return get(GAMMA_PROPERTY, DEFAULT_GAMMA); }

    public void setVibrancy(float value) { set(VIBRANCY_PROPERTY, value); }

    public float getVibrancy() { return get(VIBRANCY_PROPERTY, DEFAULT_VIBRANCY); }

    public void setGradientColour(Color value) { set(GRADIENT_COLOUR_PROPERTY, value.getRGB()); }

    public Color getGradientColour() { return get(GRADIENT_COLOUR_PROPERTY, DEFAULT_GRADIENT_COLOUR); }

    public void setBlurKernel(int value) { set(BLUR_KERNEL_PROPERTY, value); }

    public int getBlurKernel() { return get(BLUR_KERNEL_PROPERTY, DEFAULT_BLUR_KERNEL); }

    public void setIterationsLimit(long value) { set(ITERATIONS_LIMIT_PROPERTY, value); }

    public long getIterationsLimit() { return get(ITERATIONS_LIMIT_PROPERTY, DEFAULT_ITERATIONS_LIMIT); }

    public void setIterationsUnimited(boolean value) { set(ITERATIONS_UNLIMITED_PROPERTY, value); }

    public boolean isIterationsUnlimited() { return get(ITERATIONS_UNLIMITED_PROPERTY, DEFAULT_ITERATIONS_UNLIMITED); }

    public int getMinGrid() { return get(GRID_MIN_PROPERTY, DEFAULT_GRID_MIN); }

    public int getMaxGrid() { return get(GRID_MAX_PROPERTY, DEFAULT_GRID_MAX); }

    public int getSnapGrid() { return get(GRID_SNAP_PROPERTY, DEFAULT_GRID_SNAP); }

    public long getIterations() { return get(ITERATIONS_PROPERTY, DEFAULT_ITERATIONS); }

    public int getWindowWidth() { return get(WINDOW_WIDTH_PROPERTY, DEFAULT_WINDOW_SIZE); }

    public int getWindowHeight() { return get(WINDOW_HEIGHT_PROPERTY, DEFAULT_WINDOW_SIZE); }

    public Dimension getWidndowSize() { return new Dimension(getWindowWidth(), getWindowHeight()); }

    public void setDisplayScale(float value) { set(DISPLAY_SCALE_PROPERTY, value); }

    public float getDisplayScale() { return get(DISPLAY_SCALE_PROPERTY, DEFAULT_DISPLAY_SCALE); }

    public void setDisplayCentreX(double value) { set(DISPLAY_CENTRE_X_PROPERTY, value); }

    public double getDisplayCentreX() { return get(DISPLAY_CENTRE_X_PROPERTY, DEFAULT_DISPLAY_CENTRE_X); }

    public void setDisplayCentreY(double value) { set(DISPLAY_CENTRE_Y_PROPERTY, value); }

    public double getDisplayCentreY() { return get(DISPLAY_CENTRE_Y_PROPERTY, DEFAULT_DISPLAY_CENTRE_Y); }

    @Override
    protected SortedMap<String, String> delegate() {
        return config;
    }

    @Override
    public String toString() {
        return Joiner.on(",").useForNull("").withKeyValueSeparator("=").join(this);
    }

}
