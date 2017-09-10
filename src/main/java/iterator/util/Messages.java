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
    public static final String MENU_FILE_SAVE_AS = "explorer.menu.file.save-as";
    public static final String MENU_FILE_EXPORT = "explorer.menu.file.export";
    public static final String MENU_FILE_PRINT = "explorer.menu.file.print";
    public static final String MENU_FILE_PREFERENCES = "explorer.menu.file.preferences";
    public static final String MENU_FILE_PREFERENCES_SAVE = "explorer.menu.file.preferences.save";
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

    public static final String MENU_REFLECTION_PROPERTIES = "explorer.menu.reflection.properties";
    public static final String MENU_REFLECTION_DELETE = "explorer.menu.reflection.delete";

    public static final String MENU_EDITOR_NEW_IFS = "explorer.menu.editor.new.ifs";
    public static final String MENU_EDITOR_NEW_TRANSFORM = "explorer.menu.editor.new.transform";
    public static final String MENU_EDITOR_NEW_REFLECTION = "explorer.menu.editor.new.reflection";

    public static final String MENU_VIEWER_ZOOM = "explorer.menu.viewer.zoom";
    public static final String MENU_VIEWER_PAUSE = "explorer.menu.viewer.pause";
    public static final String MENU_VIEWER_RESUME = "explorer.menu.viewer.resume";
    public static final String MENU_VIEWER_GRID = "explorer.menu.viewer.grid";
    public static final String MENU_VIEWER_OVERLAY = "explorer.menu.viewer.overlay";
    public static final String MENU_VIEWER_INFO = "explorer.menu.viewer.info";

    public static final String DIALOG_FILES_XML = "explorer.dialog.files.xml";
    public static final String DIALOG_FILES_PNG = "explorer.dialog.files.png";
    public static final String DIALOG_FILES_PROPERTIES = "explorer.dialog.files.properties";

    public static final String DIALOG_PROPERTIES_TITLE = "explorer.dialog.properties.title";
    public static final String DIALOG_PROPERTIES_X = "explorer.dialog.properties.x";
    public static final String DIALOG_PROPERTIES_Y = "explorer.dialog.properties.y";
    public static final String DIALOG_PROPERTIES_W = "explorer.dialog.properties.w";
    public static final String DIALOG_PROPERTIES_H = "explorer.dialog.properties.h";
    public static final String DIALOG_PROPERTIES_R = "explorer.dialog.properties.r";
    public static final String DIALOG_PROPERTIES_SHX = "explorer.dialog.properties.shx";
    public static final String DIALOG_PROPERTIES_SHY = "explorer.dialog.properties.shy";
    public static final String DIALOG_PROPERTIES_WEIGHT = "explorer.dialog.properties.weight";
    public static final String DIALOG_PROPERTIES_DETERMINANT = "explorer.dialog.properties.determinant";
    public static final String DIALOG_PROPERTIES_BUTTON_UPDATE = "explorer.dialog.properties.button.update";
    public static final String DIALOG_PROPERTIES_BUTTON_CANCEL = "explorer.dialog.properties.button.cancel";

    public static final String DIALOG_MATRIX_TITLE = "explorer.dialog.matrix.title";
    public static final String DIALOG_MATRIX_BUTTON_UPDATE = "explorer.dialog.matrix.button.update";
    public static final String DIALOG_MATRIX_BUTTON_CANCEL = "explorer.dialog.matrix.button.cancel";

    public static final String DIALOG_PREFERENCES_TITLE = "explorer.dialog.preferences.title";
    public static final String DIALOG_PREFERENCES_MODE = "explorer.dialog.preferences.mode";
    public static final String DIALOG_PREFERENCES_RENDER = "explorer.dialog.preferences.render";
    public static final String DIALOG_PREFERENCES_TRANSFORM = "explorer.dialog.preferences.transform";
    public static final String DIALOG_PREFERENCES_PALETTE_FILE = "explorer.dialog.preferences.palette.file";
    public static final String DIALOG_PREFERENCES_PALETTE_SIZE = "explorer.dialog.preferences.palette.size";
    public static final String DIALOG_PREFERENCES_PALETTE_SEED = "explorer.dialog.preferences.palette.seed";
    public static final String DIALOG_PREFERENCES_THREADS = "explorer.dialog.preferences.threads";
    public static final String DIALOG_PREFERENCES_DEBUG = "explorer.dialog.preferences.debug";
    public static final String DIALOG_PREFERENCES_GAMMA = "explorer.dialog.preferences.gamma";
    public static final String DIALOG_PREFERENCES_VIBRANCY = "explorer.dialog.preferences.vibrancy";
    public static final String DIALOG_PREFERENCES_BLUR = "explorer.dialog.preferences.blur";
    public static final String DIALOG_PREFERENCES_ITERATIONS_LIMIT = "explorer.dialog.preferences.iterations.limit";
    public static final String DIALOG_PREFERENCES_BUTTON_UPDATE = "explorer.dialog.preferences.button.update";
    public static final String DIALOG_PREFERENCES_BUTTON_CANCEL = "explorer.dialog.preferences.button.cancel";

    public static final String DIALOG_ZOOM_TITLE = "explorer.dialog.zoom.title";
    public static final String DIALOG_ZOOM_X = "explorer.dialog.zoom.x";
    public static final String DIALOG_ZOOM_Y = "explorer.dialog.zoom.y";
    public static final String DIALOG_ZOOM_SCALE = "explorer.dialog.zoom.scale";
    public static final String DIALOG_ZOOM_BUTTON_UPDATE = "explorer.dialog.zoom.button.update";
    public static final String DIALOG_ZOOM_BUTTON_CANCEL = "explorer.dialog.zoom.button.cancel";

    private ResourceBundle resources;

    public Messages(final Explorer controller) {
        super();

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
