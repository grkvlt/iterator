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

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Useful static methods.
 */
public class Utils {
    /**
     * Waits until the {@code input} {@link Supplier#get() supplies} a non-null object.
     *
     * This does not have a timeout, and will wait forever.
     *
     * <pre>
     * {@code bus = waitFor(new Supplier<EventBus>() {
     *     public EventBus get() { return explorer.getEventBus(); }
     * });}
     * </pre>
     *
     * @param input a {@link Supplier} for the required object
     * @return the supplied object
     * @see Supplier#get()
     * @see Predicates#notNull()
     */
    public static <T> T waitFor(final Supplier<T> input) {
        return waitFor(Predicates.<T>notNull(), input);
    }
    public static <T> T waitFor(final Predicate<T> predicate, final Supplier<T> input) {
        for (int spin = 0; Predicates.not(predicate).apply(input.get());) {
            if (++spin % 10_000_000 == 0) System.err.print('.');
        }
        T result = input.get();
        System.err.println(result.getClass().getName());
        return result;
    }

    /**
     * Concatenates a series of optional elements onto an initial {@link List}.
     *
     * <pre>
     * {@code List<Transform> all = concatenate(ifs, selected);}
     * </pre>
     *
     * @param initial the initial {@link List}
     * @param optional a series of optional elements that may be null
     * @return a new {@link List} including the non-null optional elements
     * @see Iterables#concat(Iterable)
     * @see Optional#asSet()
     */
    @SafeVarargs
    public static <T> List<T> concatenate(List<T> initial, T...optional) {
        List<Set<T>> extra = Lists.newArrayList();
        for (T nullable : optional) {
            extra.add(Optional.fromNullable(nullable).asSet());
        }
        Iterable<T> joined = Iterables.concat(initial, Iterables.concat(extra));
        return Lists.newArrayList(joined);
    }
}
    