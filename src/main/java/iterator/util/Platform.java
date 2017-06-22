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

import java.util.Locale;

import com.google.common.base.Strings;

/**
 * Runtime platform enumeration.
 */
public enum Platform {

    LINUX,
    MAC_OS_X,
    WINDOWS,
    UNKNOWN;

    public static final String OS_NAME_PROPERTY = "os.name";

    public static Platform getPlatform() {
        String osName = Strings.nullToEmpty(System.getProperty(OS_NAME_PROPERTY)).toUpperCase(Locale.UK).replace(' ', '_');
        try {
            // TODO Check behaviour on Windows variants
            return Platform.valueOf(osName);
        } catch (IllegalArgumentException iee) {
            // TODO Add other operating systems
            return UNKNOWN;
        }
    }
}
    