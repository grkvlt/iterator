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
package iterator.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * Configuration {@link Map}.
 * <p>
 * Global cobfiguration values for use in preferences dialog.
 */
public class Config extends ForwardingSortedMap<String, String> {

    public static final String USER_HOME_PROPERTY = "user.home";

    public static final String PROPERTIES_FILE = "explorer.properties";

    public static final String EXPLORER_PROPERTY = "explorer";
    public static final String PALETTE_PROPERTY = EXPLORER_PROPERTY + ".palette";
    public static final String GRID_PROPERTY =  EXPLORER_PROPERTY + ".grid";
    public static final String WINDOW_PROPERTY =  EXPLORER_PROPERTY + ".window";
    public static final String DEBUG_PROPERTY = EXPLORER_PROPERTY + ".debug";
    public static final String MODE_PROPERTY = EXPLORER_PROPERTY + ".mode";

    public static final String DEFAULT_PALETTE_FILE = "abstract";
    public static final Integer DEFAULT_PALETTE_SIZE = 64;
    public static final Long DEFAULT_PALETTE_SEED = 0L;
    public static final Integer DEFAULT_GRID_MIN = 10;
    public static final Integer DEFAULT_GRID_MAX = 50;
    public static final Integer DEFAULT_GRID_SNAP = 5;
    public static final Integer DEFAULT_WINDOW_SIZE = 600;
    public static final Integer MIN_WINDOW_SIZE = 400; // Details view requires 350px

    public static final String MODE_COLOUR = "colour";
    public static final String MODE_PALETTE = "palette";
    public static final String MODE_GRAY = "gray";

    private static final SortedMap<String, String> config = Maps.newTreeMap();

    public Config() { }
    
    public void loadProperties(File override) {
        try {
            // Defaults from classpath
            load(Resources.newReaderSupplier(Resources.getResource(PROPERTIES_FILE), Charsets.UTF_8).getInput());

            // Configuration from home directory
            File home = new File(System.getProperty(USER_HOME_PROPERTY), "." + PROPERTIES_FILE);
            if (home.exists()) {
                load(Files.newReaderSupplier(home, Charsets.UTF_8).getInput());
            }

            // Configuration from current directory
            File current = new File(PROPERTIES_FILE);
            if (current.exists()) {
                load(Files.newReaderSupplier(current, Charsets.UTF_8).getInput());
            }

            // Override file from command line
            if (override != null) {
                load(Files.newReaderSupplier(override, Charsets.UTF_8).getInput());
            }

            // Finally load system properties (JAVA_OPTS)
            config.putAll(Maps.fromProperties(System.getProperties()));
        } catch (IOException ioe) {
            Throwables.propagate(ioe);
        }
    }

    private void load(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        config.putAll(Maps.fromProperties(properties));
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
}
