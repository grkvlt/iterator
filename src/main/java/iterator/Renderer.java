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
package iterator;

import static iterator.Utils.NEWLINE;
import static iterator.Utils.version;
import static iterator.util.Config.CONFIG_OPTION;
import static iterator.util.Config.CONFIG_OPTION_LONG;
import static iterator.util.Config.MIN_WINDOW_SIZE;
import static iterator.util.Config.PALETTE_OPTION;
import static iterator.util.Config.PALETTE_OPTION_LONG;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import iterator.model.IFS;
import iterator.util.Config;
import iterator.util.Output;
import iterator.view.Iterator;

/**
 * IFS Renderer main class.
 */
public class Renderer implements BiConsumer<Throwable, String> {

    public static final List<String> BANNER = Arrays.asList(
            "   ___ _____ ____    ____                _",
            "  |_ _|  ___/ ___|  |  _ \\ ___ _ __   __| | ___ _ __ ___ _ __",
            "   | || |_  \\___ \\  | |_) / _ \\ '_ \\ / _` |/ _ \\ '__/ _ \\ '__|",
            "   | ||  _|  ___) | |  _ <  __/ | | | (_| |  __/ | |  __/ |",
            "  |___|_|   |____/  |_| \\_\\___|_| |_|\\__,_|\\___|_|  \\___|_|",
            "",
            "    Iterated Function System Renderer %s",
            "",
            "    Copyright 2012-2017 by Andrew Donald Kennedy",
            "    Licensed under the Apache Software License, Version 2.0",
            "    Documentation at https://grkvlt.github.io/iterator/",
            "");

    private Config config;
    private Output out = new Output();
    private Path override;
    private String paletteFile;
    private Iterator iterator;
    private IFS ifs;
    private Dimension size;
    private Path picture;

    public Renderer(String...argv) {
        // Parse arguments
        if (argv.length < 2) {
            out.error("Must have at least two arguments");
        }
        for (int i = 0; i < argv.length - 2; i++) {
            // Argument is a program option
            if (argv[i].charAt(0) == '-') {
                if (argv[i].equalsIgnoreCase(PALETTE_OPTION) ||
                        argv[i].equalsIgnoreCase(PALETTE_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        paletteFile = argv[++i];
                    } else {
                        out.error("Palette argument not provided");
                    }
                } else if (argv[i].equalsIgnoreCase(CONFIG_OPTION) ||
                        argv[i].equalsIgnoreCase(CONFIG_OPTION_LONG)) {
                    if (argv.length >= i + 1) {
                        override = Paths.get(argv[++i]);
                        if (Files.notExists(override)) {
                            out.error("Configuration file does not exist: %s", override.getFileName());
                        }
                    } else {
                        out.error("Configuration file argument not provided");
                    }
                } else {
                    out.error("Cannot parse option: %s", argv[i]);
                }
            }
        }

        // IFS file argument
        Path file = Paths.get(argv[argv.length - 2]);
        if (Files.isReadable(file)) {
            ifs = IFS.load(file.toFile());
        } else {
            out.error("Cannot load XML data file: %s", file.getFileName());
        }

        // Picture file name
        String pictureFile = argv[argv.length - 1];
        if (!pictureFile.endsWith(".png")) {
            pictureFile += ".png";
        }
        picture = Paths.get(pictureFile);

        // Load configuration
        config = Config.loadProperties(override);
        if (!Strings.isNullOrEmpty(paletteFile)) {
            config.setPaletteFile(paletteFile);
        }

        // Load colour palette if required
        config.loadColours();

        // Get window size configuration
        int w = Math.max(MIN_WINDOW_SIZE, config.getWindowWidth());
        int h = Math.max(MIN_WINDOW_SIZE, config.getWindowHeight());
        size = new Dimension(w, h);
        ifs.setSize(size);
    }

    public void start() {
        out.timestamp("Started");

        if (config.isIterationsUnlimited()) {
            config.setIterationsUnimited(false);
        }
        long limit = config.getIterationsLimit() / 1000l;

        // Create iterator
        iterator = new Iterator(this, config, size);
        iterator.reset(size);
        iterator.setTransforms(ifs);

        // Print details
        String infoText = iterator.getInfo();
        out.print(infoText);
        String limitText = String.format("%,dK", limit).replaceAll("[^0-9K+]", " ");
        out.print("%s Iterations", limitText);

        // Render IFS
        iterator.start();
        while (iterator.getCount() <= limit) {
            Utils.sleep(100, TimeUnit.MILLISECONDS);
            String countText = String.format("%,dK", Math.min(iterator.getCount(), limit)).replaceAll("[^0-9K+]", " ");
            out.pause(countText);
        }
        out.println();
        iterator.stop();

        // Save PNG image
        out.stack("Saving %s", picture.getFileName());
        Utils.saveImage(iterator.getImage(), picture.toFile());

        System.exit(0);
    }

    @Override
    public void accept(Throwable t, String message) {
        out.accept(t,  message);
    }

    /**
     * Renderer.
     */
    public static void main(final String...argv) throws Exception {
        String banner = Joiner.on(NEWLINE).join(BANNER);
        System.out.printf(banner, version());
        System.out.println();

        Renderer renderer = new Renderer(argv);
        renderer.start();
    }

}
