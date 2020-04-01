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

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * Runtime platform enumeration.
 */
public enum Platform {
    LINUX,
    MAC,
    WINDOWS,
    UNKNOWN;

    public String getName() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
    }

    public static Platform getPlatform() {
        String os = Strings.nullToEmpty(StandardSystemProperty.OS_NAME.value()).toUpperCase(Locale.ROOT);
        String name = Iterables.getFirst(Splitter.on(' ').split(os), "UNKNOWN");
        try {
            return Platform.valueOf(name);
        } catch (IllegalArgumentException iee) {
            return UNKNOWN;
        }
    }
}
    