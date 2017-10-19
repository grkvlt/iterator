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

import static iterator.Utils.calibri;

import java.awt.Font;
import java.util.function.BiConsumer;

import com.google.common.base.Supplier;

/**
 * Dialog box interface.
 */
public interface Dialog<T extends Dialog<T>> extends AutoCloseable, Supplier<T> {

    public static final Font CALIBRI_PLAIN_14 = calibri(Font.PLAIN, 14);
    public static final Font CALIBRI_ITALIC_14 = calibri(Font.ITALIC, 14);
    public static final Font CALIBRI_BOLD_14 = calibri(Font.BOLD, 14);
    public static final Font CALIBRI_BOLD_ITALIC_14 = calibri(Font.BOLD | Font.ITALIC, 14);
    public static final Font CALIBRI_BOLD_16 = calibri(Font.BOLD, 16);

    /** Method to display the dialog box. */
    public void showDialog();

    @Override
    public default T get() {
        return (T) this;
    }

    public static <T extends Dialog<T>> void show(Supplier<T> supplier, BiConsumer<Throwable, String>...exceptionHandlers) {
        try (Dialog dialog = supplier.get()) {
            dialog.showDialog();
        } catch (Throwable t) {
            for (BiConsumer<Throwable, String> handler : exceptionHandlers) {
                handler.accept(t, "Error displaying dialog");
            }
        }
    }

}
