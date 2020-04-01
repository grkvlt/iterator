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

import java.util.Optional;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;

import com.google.common.base.Supplier;

/**
 * Dialog box property.
 * <p>
 * Includes helper methods to create properties for different types of Swing component.
 */
public interface Property<T> extends Supplier<T> {

    /** Set the property. */
    void set(T value);

    interface OptionalProperty<T> extends Property<Optional<T>> {

        /** Check if the value is present. */
        default boolean isPresent() {
            return get().isPresent();
        }

        /** Return either the value or {@literal null}. */
        default T getNullable() {
            return get().orElse(null);
        }

        static <T> OptionalProperty<T> attach(JFormattedTextField field) {
            return new OptionalProperty<T>() {
                @SuppressWarnings("unchecked")
                @Override
                public Optional<T> get() {
                    return (Optional<T>) field.getValue();
                }
                @Override
                public void set(Optional<T> value) {
                    field.setValue(value);
                }
            };
        }
    }

    static <T> Property<T> attach(JFormattedTextField field) {
        return new Property<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                return (T) field.getValue();
            }
            @Override
            public void set(T value) {
                field.setValue(value);
            }
        };
    }

    static <T> Property<T> attach(JComboBox<T> field) {
        return new Property<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T get() {
                return (T) field.getSelectedItem();
            }
            @Override
            public void set(T value) {
                field.setSelectedItem(value);
            }
        };
    }

    static Property<Integer> attach(JSpinner field) {
        return new Property<Integer>() {
            @Override
            public Integer get() {
                return (Integer) field.getValue();
            }
            @Override
            public void set(Integer value) {
                field.setValue(value);
            }
        };
    }

    static Property<Boolean> attach(JCheckBox field) {
        return new Property<Boolean>() {
            @Override
            public Boolean get() {
                return field.isSelected();
            }
            @Override
            public void set(Boolean value) {
                field.setSelected(value);
            }
        };
    }
}
