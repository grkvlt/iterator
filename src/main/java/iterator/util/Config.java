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

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * Configuration {@link Map}.
 * <p>
 * Global cobfiguration values for use in preferences dialog.
 */
public class Config extends ForwardingSortedMap<String, String> {

    public static final String PROPERTIES_FILE = "explorer.properties";

    public static final String EXPLORER_PROPERTY = "explorer";
    public static final String PALETTE_PROPERTY = EXPLORER_PROPERTY + ".palette";
    public static final String GRID_PROPERTY =  EXPLORER_PROPERTY + ".grid";
    public static final String WINDOW_PROPERTY =  EXPLORER_PROPERTY + ".window";
    public static final String DEBUG_PROPERTY = EXPLORER_PROPERTY + ".debug";
    public static final String MODE_PROPERTY = EXPLORER_PROPERTY + ".mode";
    public static final String THREADS_PROPERTY = EXPLORER_PROPERTY + ".threads";
    public static final String RENDER_PROPERTY = EXPLORER_PROPERTY + ".render";

    public static final String[] PALETTE_FILES = { "abstract", "autumn", "car", "car2", "forest", "lego", "night", "trees", "wave" };
    public static final String DEFAULT_PALETTE_FILE = "abstract";
    public static final Integer DEFAULT_PALETTE_SIZE = 64;
    public static final Long DEFAULT_PALETTE_SEED = 0L;
    public static final Integer DEFAULT_GRID_MIN = 10;
    public static final Integer DEFAULT_GRID_MAX = 50;
    public static final Integer DEFAULT_GRID_SNAP = 5;
    public static final Integer DEFAULT_WINDOW_SIZE = 600;
    public static final Integer MIN_WINDOW_SIZE = 400; // Details view requires 350px
    public static final Integer MIN_THREADS = 2;

    public static enum Mode {
        COLOUR, PALETTE, STEALING, IFS_COLOUR, GRAY;

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name()).toLowerCase(Locale.UK);
        }
    }

    public static final Mode DEFAULT_MODE = Mode.GRAY;

    public static enum Render {
        STANDARD, TOP, MEASURE, IFS;

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name()).toLowerCase(Locale.UK);
        }
    }

    public static final Render DEFAULT_RENDER = Render.STANDARD;

    public static final Predicate<CharSequence> EXPLORER_KEYS = Predicates.containsPattern("^" + EXPLORER_PROPERTY + ".");

    private final File override;
    private final SortedMap<String, String> config;

    private Config(File override) {
        this.override = override;
        this.config = Maps.newTreeMap();
    }

    public static Config loadProperties(File override) {
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
        load(Resources.asCharSource(Resources.getResource(PROPERTIES_FILE), Charsets.UTF_8));

        // Configuration from home directory
        File home = new File(StandardSystemProperty.USER_HOME.value(), "." + PROPERTIES_FILE);
        if (home.exists()) {
            load(Files.asCharSource(home, Charsets.UTF_8));
        }

        // Configuration from current directory
        File current = new File(PROPERTIES_FILE);
        if (current.exists()) {
            load(Files.asCharSource(current, Charsets.UTF_8));
        }

        // Override file from command line
        if (override != null) {
            load(Files.asCharSource(override, Charsets.UTF_8));
        }

        // Finally load system properties (JAVA_OPTS)
        load(System.getProperties());
    }

    public void load(CharSource source) {
        try {
            Properties properties = new Properties();
            properties.load(source.openStream());
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

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return (T) cast(value, defaultValue.getClass());
        }
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
                throw new IllegalArgumentException("Bad Character value: " + value);
            }
            return (T) Character.valueOf(value.charAt(0));
        } else if (Boolean.class == type || Boolean.TYPE == type) {
            return (T) Boolean.valueOf(value);
        } else if (String.class == type) {
            return (T) value;
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
