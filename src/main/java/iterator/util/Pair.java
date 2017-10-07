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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

/**
 * A simple pair of objects.
 */
public class Pair<T> {
    private T left, right;

    public static <T> Pair<T> of(T left, T right) {
        return new Pair(left, right);
    }

    public static <T> Pair<T> of() {
        return new Pair<T>();
    }

    public static <T> Pair<T> fromEntry(Map.Entry<T, T> entry) {
        return Pair.of(entry.getKey(), entry.getValue());
    }

    public static <T> Map.Entry<T,T> toEntry(Pair<T> pair) {
        return pair.asEntry();
    }

    public Pair() { }

    public Pair(T left, T right) {
        this.left = left;
        this.right = right;
    }

    public T getLeft() { return left; }

    public void setLeft(T left) { this.left = left; }

    public T getRight() { return right; }

    public void setRight(T right) { this.right = right; }

    public Map.Entry<T,T> asEntry() {
        return new AbstractMap.SimpleEntry(left, right);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", Objects.toString(left, "null"), Objects.toString(left, "null"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            Pair that = (Pair) obj;
            return (Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right));
        } else {
            return false;
        }
    }
}