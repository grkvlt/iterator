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

import static iterator.Utils.DEBUG;
import static iterator.Utils.NEWLINE;
import static iterator.Utils.PRINT;
import static iterator.Utils.STACK;
import static iterator.Utils.checkBoxItem;
import static iterator.Utils.context;
import static iterator.Utils.loadImage;
import static iterator.Utils.menuItem;
import static iterator.Utils.printError;
import static iterator.Utils.saveImage;
import static iterator.util.Config.MIN_WINDOW_SIZE;
import static iterator.util.Messages.DIALOG_FILES_PNG;
import static iterator.util.Messages.DIALOG_FILES_PROPERTIES;
import static iterator.util.Messages.DIALOG_FILES_XML;
import static iterator.util.Messages.MENU_DISPLAY;
import static iterator.util.Messages.MENU_DISPLAY_DETAILS;
import static iterator.util.Messages.MENU_DISPLAY_EDITOR;
import static iterator.util.Messages.MENU_DISPLAY_VIEWER;
import static iterator.util.Messages.MENU_FILE;
import static iterator.util.Messages.MENU_FILE_ABOUT;
import static iterator.util.Messages.MENU_FILE_EXPORT;
import static iterator.util.Messages.MENU_FILE_NEW;
import static iterator.util.Messages.MENU_FILE_OPEN;
import static iterator.util.Messages.MENU_FILE_PREFERENCES;
import static iterator.util.Messages.MENU_FILE_PREFERENCES_SAVE;
import static iterator.util.Messages.MENU_FILE_PRINT;
import static iterator.util.Messages.MENU_FILE_QUIT;
import static iterator.util.Messages.MENU_FILE_SAVE;
import static iterator.util.Messages.MENU_FILE_SAVE_AS;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.common.io.Resources;

import iterator.dialog.About;
import iterator.dialog.Preferences;
import iterator.model.IFS;
import iterator.util.Config;
import iterator.util.Dialog;
import iterator.util.Messages;
import iterator.util.Platform;
import iterator.util.Subscriber;
import iterator.util.Version;
import iterator.view.Details;
import iterator.view.Editor;
import iterator.view.Iterator;
import iterator.view.Viewer;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer extends JFrame implements KeyListener, UncaughtExceptionHandler, SubscriberExceptionHandler, BiConsumer<Throwable, String>, Subscriber {

    public static final List<String> BANNER = Arrays.asList(
            "   ___ _____ ____    _____            _                     ",
            "  |_ _|  ___/ ___|  | ____|_  ___ __ | | ___  _ __ ___ _ __ ",
            "   | || |_  \\___ \\  |  _| \\ \\/ / '_ \\| |/ _ \\| '__/ _ \\ '__|",
            "   | ||  _|  ___) | | |___ >  <| |_) | | (_) | | |  __/ |   ",
            "  |___|_|   |____/  |_____/_/\\_\\ .__/|_|\\___/|_|  \\___|_|   ",
            "                               |_|                          ",
            "",
            "    Iterated Function System Explorer %s",
            "",
            "    Copyright 2012-2017 by Andrew Donald Kennedy",
            "    Licensed under the Apache Software License, Version 2.0",
            "    Documentation at https://grkvlt.github.io/iterator/",
            "");

    public static final List<String> HELP = Arrays.asList(
            "",
            "    Iterated Function System Explorer Help",
            "",
            "    As well as accelerators for menu items, the keyboard",
            "    can be used to perform the following actions:",
            "",
            "      tab/shift-tab: Cycle between screens",
            "      s/S: Change palette random seed",
            "",
            "    Editor",
            "      up/down/left/right: Move selcted transform",
            "      +/-: Rotate selected transform 90 degrees",
            "      delete: Delete selected transform",
            "",
            "    Viewer",
            "      up/down: Change number of threads",
            "      t: Print current thread state",
            "      space: Pause and resume iteration",
            "      +/-: Zoom in or out by a factor of two",
            "      =: Centre and reset zoom to original",
            "      i: Toggle information text display",
            "      o: Toggle transform overlay display",
            "      g: Toggle grid display",
            "",
            "    See https://grkvlt.github.io/iterator/ for more.",
            "");

    public static final String FULLSCREEN_OPTION = "-f";
    public static final String FULLSCREEN_OPTION_LONG = "--fullscreen";
    public static final String PALETTE_OPTION = "-p";
    public static final String PALETTE_OPTION_LONG = "--palette";
    public static final String CONFIG_OPTION = "-c";
    public static final String CONFIG_OPTION_LONG = "--config";

    public static final String EDITOR = "editor";
    public static final String VIEWER = "viewer";
    public static final String DETAILS = "details";

    public static final Version version = Version.instance();

    private Config config;
    private Path override;

    private Platform platform = Platform.getPlatform();
    private BufferedImage icon;
    private Preferences prefs;
    private About about;

    private boolean fullScreen = false;
    private String paletteFile;

    private Iterator iterator;
    private IFS ifs;
    private Dimension size;
    private Dimension min = new Dimension(MIN_WINDOW_SIZE, MIN_WINDOW_SIZE);
    private File cwd;
    private Messages messages;
    private EventBus bus;

    private JMenuBar menuBar;
    private Editor editor;
    private Viewer viewer;
    private Details details;
    private JScrollPane scroll;
    private JPanel view;
    private CardLayout cards;
    private String current;
    private JCheckBoxMenuItem showEditor, showViewer, showDetails;
    private JMenuItem export, print, save, saveAs;
    private Runnable postponed;

    public Explorer(String...argv) {
        super();

        // Setup event bus
        bus = new EventBus(this);
        bus.register(this);

        messages = new Messages(this);

        Thread.setDefaultUncaughtExceptionHandler(this);

        // Parse arguments
        if (argv.length != 0) {
            for (int i = 0; i < argv.length; i++) {
                if (argv[i].charAt(0) == '-') {
                    // Argument is a program option
                    if (argv[i].equalsIgnoreCase(FULLSCREEN_OPTION) ||
                            argv[i].equalsIgnoreCase(FULLSCREEN_OPTION_LONG)) {
                        fullScreen = true;
                    } else if (argv[i].equalsIgnoreCase(PALETTE_OPTION) ||
                            argv[i].equalsIgnoreCase(PALETTE_OPTION_LONG)) {
                        if (argv.length >= i + 1) {
                            paletteFile = argv[++i];
                        } else error("Palette argument not provided");
                    } else if (argv[i].equalsIgnoreCase(CONFIG_OPTION) ||
                            argv[i].equalsIgnoreCase(CONFIG_OPTION_LONG)) {
                        if (argv.length >= i + 1) {
                            override = Paths.get(argv[++i]);
                            if (Files.notExists(override)) {
                                error("Configuration file does not exist: %s", override);
                            }
                        } else error("Configuration file argument not provided");
                    } else {
                        error("Cannot parse option: %s", argv[i]);
                    }
                } else if (i == argv.length - 1) {
                    // Last argument is a file
                    File file = new File(argv[i]);
                    if (file.canRead()) {
                        // Add a task to load the file
                        postponed = () -> {
                            IFS loaded = load(file);
                            loaded.setSize(size);
                            bus.post(loaded);
                        };
                    } else {
                        error("Cannot load XML data file: %s", argv[i]);
                    }
                } else {
                    error("Unknown argument: %s", argv[i]);
                }
            }
        }

        // Load configuration
        config = Config.loadProperties(override);
        if (!Strings.isNullOrEmpty(paletteFile)) {
            config.setPaletteFile(paletteFile);
        }
        debug("Configured rendering as %s/%s %s", config.getRender(), config.getMode(), config.getMode().isPalette() ? config.getPaletteFile() : config.getMode().isColour() ? "hsb" : "black");

        // Load colour palette if required
        config.loadColours();

        // Get window size configuration
        int w = Math.max(MIN_WINDOW_SIZE, config.getWindowWidth());
        int h = Math.max(MIN_WINDOW_SIZE, config.getWindowHeight());
        size = new Dimension(w, h);

        // Load icon resources
        icon = loadImage(Resources.getResource("icon.png"));
        setIconImage(icon);

        // Setup full-screen mode if required
        if (fullScreen) {
            setUndecorated(true);
            setResizable(false);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            setBounds(insets.left, insets.top, screen.width - (insets.left + insets.right), screen.height - (insets.top + insets.bottom));
        }

        // Setup LAF
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            error(e, "Unable to configure UI support");
        }
    }

    public boolean isFullScreen() { return fullScreen; }

    @SuppressWarnings("serial")
    public void start() {
        iterator = new Iterator(this, config, size);

        prefs = Preferences.dialog(this);
        about = About.dialog(this);

        JPanel content = new JPanel(new BorderLayout());

        menuBar = new JMenuBar();
        JMenu file = new JMenu(messages.getText(MENU_FILE));
        if (platform != Platform.MAC_OS_X) {
            file.add(menuItem(messages.getText(MENU_FILE_ABOUT), e -> {
                Dialog.show(this::getAbout, this);
            }));
        }
        JMenuItem newIfs = menuItem(messages.getText(MENU_FILE_NEW), e -> {
            IFS untitled = new IFS();
            show(EDITOR);
            bus.post(untitled);
            getEditor().resetImage();
        });
        newIfs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(newIfs);
        JMenuItem open = menuItem(messages.getText(MENU_FILE_OPEN), e -> {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(messages.getText(DIALOG_FILES_XML), "xml");
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(cwd);
            chooser.setFileFilter(filter);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                IFS loaded = load(chooser.getSelectedFile());
                loaded.setSize(size);
                show(EDITOR);
                bus.post(loaded);
            }
        });
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(open);
        save = menuItem(messages.getText(MENU_FILE_SAVE), e -> {
            if (ifs.getName() == null) {
                saveAs.doClick();
            } else {
                File saveAs = new File(cwd, ifs.getName() + ".xml");
                save(saveAs);
                save.setEnabled(false);
            }
        });
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.setEnabled(false);
        file.add(save);
        saveAs = menuItem(messages.getText(MENU_FILE_SAVE_AS), e -> {
            File target = new File(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED) + ".xml");
            saveDialog(target, DIALOG_FILES_XML, "xml", f -> {
                String name = f.getName().replace(".xml", "");
                ifs.setName(name);
                save(f);
                updateName(name);
            });
        });
        saveAs.setEnabled(false);
        file.add(saveAs);
        export = menuItem(messages.getText(MENU_FILE_EXPORT), e -> {
            File target = new File(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED) + ".png");
            saveDialog(target, DIALOG_FILES_PNG, "png", f -> {
                print("Saving PNG image %s", f.getName());
                saveImage(viewer.getImage(), f);
            });
        });
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(export);
        print = menuItem(messages.getText(MENU_FILE_PRINT), e -> {
            pauseViewer(() -> {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setJobName(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED));
                PageFormat pf = job.defaultPage();
                if (getWidth() > getHeight()) {
                    pf.setOrientation(PageFormat.LANDSCAPE);
                } else {
                    pf.setOrientation(PageFormat.PORTRAIT);
                }
                switch (current) {
                    case VIEWER:
                        job.setPrintable(viewer, pf);
                        break;
                    case DETAILS:
                        job.setPrintable(details, pf);
                        break;
                }
                boolean ok = job.printDialog();
                if (ok) {
                    try {
                        job.print();
                    } catch (PrinterException pe) {
                        throw new RuntimeException(pe);
                    }
                }
            });
        });
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        print.setEnabled(false);
        file.add(print);
        if (platform != Platform.MAC_OS_X) {
            file.add(menuItem(messages.getText(MENU_FILE_PREFERENCES), e -> {
                Dialog.show(this::getPreferences, this);
            }));
        }
        file.add(menuItem(messages.getText(MENU_FILE_PREFERENCES_SAVE), e -> {
            File target = Optional.fromNullable(override).or(Paths.get(Config.PROPERTIES_FILE)).toFile();
            saveDialog(target, DIALOG_FILES_PROPERTIES, "properties", f -> {
                config.save(f);
            });
        }));
        if (platform != Platform.MAC_OS_X) {
            JMenuItem quit = menuItem(messages.getText(MENU_FILE_QUIT), e -> {
                System.exit(0);
            });
            quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            file.add(quit);
        }
        menuBar.add(file);

        JMenu system = new JMenu(messages.getText(MENU_DISPLAY));
        ButtonGroup displayGroup = new ButtonGroup();
        showEditor = checkBoxItem(messages.getText(MENU_DISPLAY_EDITOR), e -> {
            show(EDITOR);
        });
        system.add(showEditor);
        displayGroup.add(showEditor);
        showViewer = checkBoxItem(messages.getText(MENU_DISPLAY_VIEWER), e -> {
            show(VIEWER);
        });
        system.add(showViewer);
        displayGroup.add(showViewer);
        showDetails = checkBoxItem(messages.getText(MENU_DISPLAY_DETAILS), e -> {
            show(DETAILS);
        });
        system.add(showDetails);
        displayGroup.add(showDetails);
        menuBar.add(system);

        setJMenuBar(menuBar);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /** @see WindowListener#windowClosed(WindowEvent) */
            @Override
            public void windowClosing(WindowEvent e) {
                print("Exiting");
                System.exit(0);
            }
        });

        editor = new Editor(this);
        viewer = new Viewer(this);
        details = new Details(this);
        scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setViewportView(details);

        cards = new CardLayout();
        view = new JPanel(cards);
        view.add(editor, EDITOR);
        view.add(viewer, VIEWER);
        view.add(scroll, DETAILS);
        content.add(view, BorderLayout.CENTER);
        show(EDITOR);
        showEditor.setSelected(true);

        setContentPane(content);

        if (!fullScreen) {
            editor.setMinimumSize(min);
            editor.setSize(size);
            viewer.setMinimumSize(min);
            viewer.setSize(size);
            pack();
            final int top = getInsets().top + menuBar.getHeight();
            Dimension actual = new Dimension(size.width, size.height + top);
            setSize(actual);
            setPreferredSize(actual);
            setMinimumSize(new Dimension(min.width, min.height + top));
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screen.width / 2) - (actual.width / 2), (screen.height / 2) - (actual.height / 2));
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    Dimension suggested = getSize();
                    int side = Math.max(MIN_WINDOW_SIZE, Math.min(suggested.width, suggested.height - top));
                    setSize(side,  side + top);
                    bus.post(new Dimension(side, side));
                }
            });
        }

        setFocusable(true);
        requestFocusInWindow();
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        addKeyListener(editor);
        addKeyListener(viewer);

        // Platform specifics
        // TODO Windows specific UI configuration
        if (platform == Platform.MAC_OS_X) {
            try {
                Class<?> support = Class.forName("iterator.AppleSupport");
                Constructor<?> ctor = support.getConstructor(EventBus.class, Explorer.class);
                Method setup = support.getDeclaredMethod("setup");
                Object apple = ctor.newInstance(bus, this);
                setup.invoke(apple);
            } catch (InvocationTargetException ite) {
                error(ite.getCause(), "Error while configuring OSX support: %s", ite.getCause().getMessage());
            } catch (Exception e) {
                error(e, "Unable to configure OSX support");
            }
        }

        IFS untitled = new IFS();
        bus.post(untitled);

        // Check for post-startup task
        if (postponed != null) {
            SwingUtilities.invokeLater(postponed);
        }

        setVisible(true);
    }

    public void saveDialog(File file, String filterText, String extension, Consumer<File> action) {
        pauseViewer(() -> {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(messages.getText(filterText), extension);
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(cwd);
            chooser.setFileFilter(filter);
            chooser.setSelectedFile(file);
            int result = chooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                String name = selected.getName();
                if (!name.toLowerCase().endsWith("." + extension)) {
                    selected = new File(selected.getParent(), name + "." + extension);
                }
                try {
                    action.accept(selected);
                } catch (Exception e) {
                    error(e, "Error saving file %s", name);
                }
                cwd = selected.getParentFile();
            }
        });
    }

    public void pauseViewer(Runnable action) {
        boolean viewerStopped = viewer.stop();
        action.run();
        if (current.equals(VIEWER) && viewerStopped) {
            viewer.start();
        }
    }

    public void show(String name) {
        cards.show(view, name);
        current = name;
        switch (current) {
            case VIEWER:
                export.setEnabled(true);
                print.setEnabled(true);
                if (!fullScreen) setResizable(false);
                editor.stop();
                viewer.reset();
                viewer.start();
                break;
            case DETAILS:
                export.setEnabled(false);
                print.setEnabled(true);
                if (!fullScreen) setResizable(true);
                editor.stop();
                viewer.stop();
                break;
            case EDITOR:
                export.setEnabled(false);
                print.setEnabled(false);
                if (!fullScreen) setResizable(true);
                viewer.stop();
                editor.start();
                break;
            default:
                throw new IllegalStateException("Unknown view type: " + current);
        }
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension resized) {
        size = resized.getSize();
        debug("Resized: %d, %d", size.width, size.height);
    }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS updated) {
        ifs = updated;
        String name = Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED);
        updateName(name);
        debug("Updated: %s", ifs);

        if (!ifs.isEmpty()) {
            save.setEnabled(true);
            saveAs.setEnabled(true);
        }

        repaint();
    }

    public void updateName(String updated) {
        String name = Splitter.onPattern("[^a-z0-9]")
                .omitEmptyStrings()
                .splitToList(updated.toLowerCase(Locale.UK))
                .stream()
                .map(s -> s.substring(0, 1).toUpperCase(Locale.UK) + s.substring(1))
                .collect(Collectors.joining(" "));
        setTitle(name);
        repaint();
    }

    public void save(File file) {
        print("Saving IFS file %s", file.getName());
        cwd = file.getParentFile();
        IFS.save(ifs, file);
    }

    public IFS load(File file) {
        print("Loading IFS file %s", file.getName());
        cwd = file.getParentFile();
        return IFS.load(file);
    }

    public BufferedImage getIcon() { return icon; }

    public JScrollPane getScroll() { return scroll; }

    public String getCurrent() { return current; }

    public JComponent getCurrentComponent() {
        switch (current) {
            case EDITOR: return editor;
            case VIEWER: return viewer;
            case DETAILS: return details;
            default:
                throw new IllegalStateException(String.format("Invalid component %s", current));
        }
    }

    public Viewer getViewer() { return viewer; }

    public Editor getEditor() { return editor; }

    public Details getDetails() { return details; }

    public About getAbout() { return about; }

    public Preferences getPreferences() { return prefs; }

    public IFS getIFS() { return ifs; }

    public Config getConfig() { return config; }

    public Iterator getIterator() { return iterator; }

    public EventBus getEventBus() { return bus; }

    public Messages getMessages() { return messages; }

    /**
     * Listener for key-presses across the whole application.
     * <p>
     * Handles {@link KeyEvent#VK_TAB TAB} for switching between viewer,
     * editor and details, and {@literal S} for changing the random seed.
     *
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_TAB:
                if (e.isShiftDown()) {
                    if (current.equals(EDITOR)) {
                        showDetails.setSelected(true);
                        show(DETAILS);
                    } else if (current.equals(DETAILS)) {
                        showViewer.setSelected(true);
                        show(VIEWER);
                    } else if (current.equals(VIEWER)) {
                        showEditor.setSelected(true);
                        show(EDITOR);
                    }
                } else {
                    if (current.equals(EDITOR)) {
                        showViewer.setSelected(true);
                        show(VIEWER);
                    } else if (current.equals(VIEWER)) {
                        showDetails.setSelected(true);
                        show(DETAILS);
                    } else if (current.equals(DETAILS)) {
                        showEditor.setSelected(true);
                        show(EDITOR);
                    }
                }
                break;
            case KeyEvent.VK_S:
                long seed = config.getSeed();
                if (e.isShiftDown()) {
                    config.setSeed(seed - 1);
                } else {
                    config.setSeed(seed + 1);
                }
                config.loadColours();
                bus.post(ifs);
                break;
            case KeyEvent.VK_SLASH:
                if (!e.isShiftDown()) break;
            case KeyEvent.VK_H:
                if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) break;
                String help = HELP.stream()
                        .map(STACK::concat)
                        .collect(Collectors.joining(NEWLINE));
                System.out.println(help);
                break;
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

    public void debug(String format, Object...varargs) {
        output(config.isDebug() ? System.out : System.err, DEBUG + format, varargs);
    }

    public void error(String format, Object...varargs) {
        error(Optional.absent(), format, varargs);
    }

    public void error(Throwable t, String format, Object...varargs) {
        error(Optional.of(t), format, varargs);
    }

    public void error(Throwable t, String message) {
        error(Optional.of(t), "%s: %s", message, t.getMessage());
    }

    public void error(Optional<Throwable> t, String format, Object...varargs) {
        output(System.out, ERROR + format, varargs);
        if (t.isPresent()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream print = new PrintStream(bytes);
            t.get().printStackTrace(print);
            String trace = Splitter.on(CharMatcher.anyOf("\r\n"))
                    .omitEmptyStrings()
                    .splitToList(bytes.toString())
                    .stream()
                    .map(STACK::concat)
                    .collect(Collectors.joining(NEWLINE));
            System.err.println(trace);
        }
        System.exit(1);
    }

    public void timestamp(String format, Object...varargs) {
        String message = String.format(format,  varargs);
        String timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        debug("%s at %s", message, timestamp);
    }

    public void dumpStack() {
        List<StackTraceElement> stack = Arrays.asList(Thread.getAllStackTraces().get(Thread.currentThread()));
        String trace = stack.stream()
                .skip(3)
                .map(e -> String.format("  %s.%s(%s:%d)", e.getClassName(), e.getMethodName(), e.getFileName(), e.getLineNumber()))
                .map(STACK::concat)
                .collect(Collectors.joining(NEWLINE));
        timestamp("Dumping stack");
        System.err.println(trace);
    }

    public void print(String format, Object...varargs) {
        output(System.out, PRINT + format, varargs);
    }

    protected void output(PrintStream out, String format, Object...varargs) {
        String output = String.format(format, varargs);
        if (!output.endsWith(NEWLINE)) output = output.concat(NEWLINE);
        out.print(output);
    }

    /** @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable) */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        error(Optional.of(e), "Error: Thread %s (%d) caused %s: %s", t.getName(), t.getId(), e.getClass().getName(), e.getMessage());
    }

    /** @see com.google.common.eventbus.SubscriberExceptionHandler#handleException(Throwable, SubscriberExceptionContext) */
    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
        debug("Event bus caught %s(%s) in %s#%s(%s)",
                exception.getClass().getSimpleName(), exception.getMessage(),
                context.getSubscriber().getClass().getSimpleName(), context.getSubscriberMethod().getName(), Objects.toString(context.getEvent(), "null"));
        uncaughtException(Thread.currentThread(), exception);
    }

    @Override
    public void accept(Throwable t, String message) {
        error(t, message);
    }

    /**
     * Explorer application launch.
     */
    public static void main(final String...argv) throws Exception {
        // Print text banner
        String banner = Joiner.on(NEWLINE).join(BANNER);
        System.out.printf(banner, version.get());
        System.out.println();

        // Print splash screen text
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null && splash.isVisible()) {
            context(printError(), splash.createGraphics(), g -> {
                About.paintSplashText(g, splash.getSize().width, splash.getSize().height);
                splash.update();
            });
        }

        // Start application
        SwingUtilities.invokeLater(() -> {
            Explorer explorer = new Explorer(argv);
            explorer.print("Starting Explorer UI");
            explorer.timestamp("Started");
            explorer.start();
        });
    }

}
