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

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.io.Resources;

public class Version implements Supplier<String> {

    private static final String VERSION_RESOURCE_FILE = "iterator.properties";
    private static final String VERSION_PROPERTY_NAME = "version";

    private static final Version INSTANCE = new Version();

    private final String version;

    private Version() {
        URL resource = Resources.getResource(VERSION_RESOURCE_FILE);
        version = readVersion(resource);
    }

    public static Version instance() {
        return INSTANCE;
    }

    /** @see Supplier#get() */
    @Override
    public String get() {
        return version;
    }

    private String readVersion(URL resource) {
        if (resource == null) return null;
        Properties versionProperties = new Properties();
        try (Reader versionStream = Resources.asCharSource(resource, Charsets.UTF_8).openStream()) {
            versionProperties.load(versionStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load version resource file", exception);
        }
        return Preconditions.checkNotNull(versionProperties.getProperty(VERSION_PROPERTY_NAME), VERSION_PROPERTY_NAME);
    }

}
