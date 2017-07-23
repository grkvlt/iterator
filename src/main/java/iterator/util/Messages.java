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
import java.util.Locale.Category;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.google.common.eventbus.EventBus;

import iterator.Explorer;

/**
 * Internationalized messaages.
 */
public class Messages {

    public static final String TITLE = "explorer.title";
    public static final String VIEW_DETAILS_TITLE = "explorer.view.details.title";

    public static final String MENU_FILE = "explorer.menu.file";
    public static final String MENU_FILE_NEW = "explorer.menu.file.new";
    public static final String MENU_FILE_ABOUT = "explorer.menu.file.about";
    public static final String MENU_FILE_OPEN = "explorer.menu.file.open";
    public static final String MENU_FILE_SAVE = "explorer.menu.file.save";
    public static final String MENU_FILE_SAVE_AS = "explorer.menu.file.save_as";
    public static final String MENU_FILE_EXPORT = "explorer.menu.file.export";
    public static final String MENU_FILE_PRINT = "explorer.menu.file.print";
    public static final String MENU_FILE_PREFERENCES = "explorer.menu.file.preferences";
    public static final String MENU_FILE_QUIT = "explorer.menu.file.quit";

    public static final String MENU_DISPLAY = "explorer.menu.display";
    public static final String MENU_DISPLAY_EDITOR = "explorer.menu.display.editor";
    public static final String MENU_DISPLAY_VIEWER = "explorer.menu.display.viewer";
    public static final String MENU_DISPLAY_DETAILS = "explorer.menu.display.details";

    public static final String MENU_TRANSFORM_PROPERTIES = "explorer.menu.transform.properties";
    public static final String MENU_TRANSFORM_MATRIX = "explorer.menu.transform.matrix";
    public static final String MENU_TRANSFORM_DELETE = "explorer.menu.transform.delete";
    public static final String MENU_TRANSFORM_DUPLICATE = "explorer.menu.transform.duplicate";
    public static final String MENU_TRANSFORM_RAISE = "explorer.menu.transform.raise";
    public static final String MENU_TRANSFORM_LOWER = "explorer.menu.transform.lower";
    public static final String MENU_TRANSFORM_FRONT = "explorer.menu.transform.front";
    public static final String MENU_TRANSFORM_BACK = "explorer.menu.transform.back";

    public static final String MENU_EDITOR_NEW = "explorer.menu.editor.new";

    public static final String DIALOG_FILES_XML = "explorer.dialog.files.xml";
    public static final String DIALOG_FILES_PNG = "explorer.dialog.files.png";

    public static final String DIALOG_PROPERTIES_TITLE = "explorer.dialog.properties.title";
    public static final String DIALOG_PROPERTIES_X = "explorer.dialog.properties.x";
    public static final String DIALOG_PROPERTIES_Y = "explorer.dialog.properties.y";
    public static final String DIALOG_PROPERTIES_W = "explorer.dialog.properties.w";
    public static final String DIALOG_PROPERTIES_H = "explorer.dialog.properties.h";
    public static final String DIALOG_PROPERTIES_R = "explorer.dialog.properties.r";
    public static final String DIALOG_PROPERTIES_BUTTON_UPDATE = "explorer.dialog.properties.button.update";
    public static final String DIALOG_PROPERTIES_BUTTON_CANCEL = "explorer.dialog.properties.button.cancel";

    public static final String DIALOG_MATRIX_TITLE = "explorer.dialog.matrix.title";
    public static final String DIALOG_MATRIX_BUTTON_UPDATE = "explorer.dialog.matrix.button.update";
    public static final String DIALOG_MATRIX_BUTTON_CANCEL = "explorer.dialog.matrix.button.cancel";

    private ResourceBundle resources;

    private final Explorer controller;
    private final EventBus bus;

    public Messages(final EventBus bus, final Explorer controller) {
        super();

        this.bus = bus;
        this.controller = controller;

        try {
            resources = ResourceBundle.getBundle("iterator.Explorer", Locale.getDefault(Category.DISPLAY));
        } catch (MissingResourceException mre) {
            controller.error("Cannot load resources for %s", Locale.getDefault(Category.DISPLAY).getDisplayName());
        }
    }

    public String getText(String key) {
        return resources.getString(key);
    }

}
