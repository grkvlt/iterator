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

import static iterator.Utils.loadImage;
import static iterator.util.Config.DEBUG_PROPERTY;
import static iterator.util.Config.DEFAULT_GRID_MAX;
import static iterator.util.Config.DEFAULT_GRID_MIN;
import static iterator.util.Config.DEFAULT_GRID_SNAP;
import static iterator.util.Config.DEFAULT_ITERATIONS;
import static iterator.util.Config.DEFAULT_MODE;
import static iterator.util.Config.DEFAULT_PALETTE_FILE;
import static iterator.util.Config.DEFAULT_PALETTE_SEED;
import static iterator.util.Config.DEFAULT_PALETTE_SIZE;
import static iterator.util.Config.DEFAULT_RENDER;
import static iterator.util.Config.DEFAULT_WINDOW_SIZE;
import static iterator.util.Config.GRID_PROPERTY;
import static iterator.util.Config.ITERATIONS_PROPERTY;
import static iterator.util.Config.MIN_THREADS;
import static iterator.util.Config.MIN_WINDOW_SIZE;
import static iterator.util.Config.MODE_PROPERTY;
import static iterator.util.Config.PALETTE_PROPERTY;
import static iterator.util.Config.RENDER_PROPERTY;
import static iterator.util.Config.THREADS_PROPERTY;
import static iterator.util.Config.WINDOW_PROPERTY;
import static iterator.util.Messages.DIALOG_FILES_PNG;
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
import static iterator.util.Messages.MENU_FILE_PRINT;
import static iterator.util.Messages.MENU_FILE_QUIT;
import static iterator.util.Messages.MENU_FILE_SAVE;
import static iterator.util.Messages.MENU_FILE_SAVE_AS;
import static iterator.util.Messages.TITLE;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
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

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.common.io.Resources;

import iterator.dialog.About;
import iterator.dialog.Preferences;
import iterator.model.IFS;
import iterator.util.Config;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
import iterator.util.Messages;
import iterator.util.Platform;
import iterator.util.Subscriber;
import iterator.util.Version;
import iterator.view.Details;
import iterator.view.Editor;
import iterator.view.Viewer;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer extends JFrame implements KeyListener, UncaughtExceptionHandler, SubscriberExceptionHandler, Subscriber {

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

    public static final String FULLSCREEN_OPTION = "-f";
    public static final String FULLSCREEN_OPTION_LONG = "--fullscreen";
    public static final String COLOUR_OPTION = "-c";
    public static final String COLOUR_OPTION_LONG = "--colour";
    public static final String PALETTE_OPTION = "-p";
    public static final String PALETTE_OPTION_LONG = "--palette";
    public static final String CONFIG_OPTION_LONG = "--config";

    public static final String EDITOR = "editor";
    public static final String VIEWER = "viewer";
    public static final String DETAILS = "details";

    private static final Version version = Version.instance();

    private static final String DEBUG = "[?] ";
    private static final String PRINT = "[-] ";
    private static final String ERROR = "[!] ";
    private static final String STACK = "[>] ";

    private static final String NEWLINE = StandardSystemProperty.LINE_SEPARATOR.value();

    private Config config;
    private File override;

    private boolean fullScreen = false;

    private boolean colour = false;
    private boolean palette = false;
    private boolean stealing = false;
    private boolean ifscolour = false;

    private Mode mode = DEFAULT_MODE;
    private Render render = DEFAULT_RENDER;
    private int paletteSize = DEFAULT_PALETTE_SIZE;
    private String paletteFile = DEFAULT_PALETTE_FILE;
    private long seed = 0l;

    private Platform platform = Platform.getPlatform();
    private int threads;
    private boolean debug;
    private BufferedImage icon, source;
    private Preferences prefs;
    private About about;

    private IFS ifs;
    private Set<Color> colours;
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

        setTitle(messages.getText(TITLE));

        Thread.setDefaultUncaughtExceptionHandler(this);

        // Parse arguments
        if (argv.length != 0) {
            for (int i = 0; i < argv.length; i++) {
                if (argv[i].charAt(0) == '-') {
                    // Argument is a program option
                    if (argv[i].equalsIgnoreCase(FULLSCREEN_OPTION) ||
                            argv[i].equalsIgnoreCase(FULLSCREEN_OPTION_LONG)) {
                        fullScreen = true;
                    } else if (argv[i].equalsIgnoreCase(COLOUR_OPTION) ||
                            argv[i].equalsIgnoreCase(COLOUR_OPTION_LONG)) {
                        colour = true;
                    } else if (argv[i].equalsIgnoreCase(PALETTE_OPTION) ||
                            argv[i].equalsIgnoreCase(PALETTE_OPTION_LONG)) {
                        if (argv.length >= i + 1) {
                            paletteFile = argv[++i];
                        } else error("Palette argument not provided");
                    } else if (argv[i].equalsIgnoreCase(CONFIG_OPTION_LONG)) {
                        if (argv.length >= i + 1) {
                            override = new File(argv[++i]);
                            if (!override.exists()) {
                                error("Configuration file does not exist: %s", override);
                            }
                        } else error("Configuration file argument not provided");
                    } else {
                        error("Cannot parse option: %s", argv[i]);
                    }
                } else if (i == argv.length - 1) {
                    // Last argument is a file
                    final File file = new File(argv[i]);
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

        threads = config.get(THREADS_PROPERTY, Math.max(Runtime.getRuntime().availableProcessors() / 2, MIN_THREADS));
        debug = config.get(DEBUG_PROPERTY, Boolean.FALSE);

        // Check colour mode configuration
        if (config.containsKey(MODE_PROPERTY)) {
            String value = config.get(MODE_PROPERTY);
            setMode(value);
        }

        // Check rendering configuration
        if (config.containsKey(RENDER_PROPERTY)) {
            String value = config.get(RENDER_PROPERTY);
            setRender(value);
        }

        // Load colour palette
        if (palette) {
            setSeed(config.get(PALETTE_PROPERTY + ".seed", DEFAULT_PALETTE_SEED));
            if (Strings.isNullOrEmpty(paletteFile)) {
                setPaletteFile(config.get(PALETTE_PROPERTY + ".file", DEFAULT_PALETTE_FILE));
            }
            setPaletteSize(config.get(PALETTE_PROPERTY + ".size", DEFAULT_PALETTE_SIZE));
            loadColours();
        }
        debug("Configured %s %s: %s", render,
                colour ? palette ? stealing ? "stealing" : "palette" : ifscolour ? "ifscolour" : "colour" : "grayscale",
                palette ? paletteFile : colour ? "hsb" : "black");

        // Get window size configuration
        int w = Math.max(MIN_WINDOW_SIZE, config.get(WINDOW_PROPERTY + ".width", DEFAULT_WINDOW_SIZE));
        int h = Math.max(MIN_WINDOW_SIZE, config.get(WINDOW_PROPERTY + ".height", DEFAULT_WINDOW_SIZE));
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

        // Load dialogs
        prefs = new Preferences(this);
        about = new About(this);

        // Setup platform specifics
        if (platform == Platform.MAC_OS_X) {
            try {
                Class<?> support = Class.forName("iterator.AppleSupport");
                Constructor<?> ctor = support.getConstructor(EventBus.class, Explorer.class);
                Method setup = support.getDeclaredMethod("setup");
                Object apple = ctor.newInstance(bus, this);
                setup.invoke(apple);
            } catch (InvocationTargetException ite) {
                error(Optional.of(ite.getCause()), "Error while configuring OSX support: %s", ite.getCause().getMessage());
            } catch (Exception e) {
                error(e, "Unable to configure OSX support");
            }
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            error(e, "Unable to configure UI support");
        }
    }

    public void setThreads(int value) {
        threads = value;
    }

    public void setDebug(boolean value) {
        debug = value;
    }

    public void setSeed(long value) {
        seed = value;
    }

    public void setPaletteFile(String value) {
        paletteFile = value;
    }

    public void setPaletteSize(int value) {
        paletteSize = value;
    }

    public void setRender(String value) {
        render = Render.valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, value));
    }

    public void setMode(String value) {
        mode = Mode.valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, value));
        colour = mode.isColour();
        palette = mode.isPalette();
        stealing = mode.isStealing();
        ifscolour = mode.isIFSColour();
    }

    public void loadColours() {
        try {
            if (paletteFile.contains(".")) {
                source = loadImage(URI.create(paletteFile).toURL());
            } else {
                source = loadImage(Resources.getResource("palette/" + paletteFile + ".png"));
            }
        } catch (MalformedURLException | RuntimeException e) {
            error(e, "Cannot load colour palette %s: %s", paletteFile, e.getMessage());
        }
        colours = Sets.newHashSet();
        Random random = new Random(seed);
        while (colours.size() < paletteSize) {
            int x = random.nextInt(source.getWidth());
            int y = random.nextInt(source.getHeight());
            Color c = new Color(source.getRGB(x, y));
            colours.add(c);
        }
    }

    public Color getPixel(double x, double y) {
        int sx = (int) Math.max(0, Math.min(source.getWidth() - 1, (x / getWidth()) * source.getWidth()));
        int sy = (int) Math.max(0, Math.min(source.getHeight() - 1, (y / getHeight()) * source.getHeight()));
        return new Color(source.getRGB(sx, sy));
    }

    @SuppressWarnings("serial")
    public void start() {
        JPanel content = new JPanel(new BorderLayout());

        menuBar = new JMenuBar();
        JMenu file = new JMenu(messages.getText(MENU_FILE));
        if (platform != Platform.MAC_OS_X) {
            file.add(new AbstractAction(messages.getText(MENU_FILE_ABOUT)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    about.showDialog();
                }
            });
        }
        JMenuItem newIfs = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_NEW)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                IFS untitled = new IFS();
                show(EDITOR);
                bus.post(untitled);
            }
        });
        newIfs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(newIfs);
        JMenuItem open = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_OPEN)) {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            }
        });
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(open);
        save = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_SAVE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ifs.getName() == null) {
                    saveAs.doClick();
                } else {
                    File saveAs = new File(cwd, ifs.getName() + ".xml");
                    save(saveAs);
                    save.setEnabled(false);
                    bus.post(ifs);
                }
            }
        });
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.setEnabled(false);
        file.add(save);
        saveAs = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_SAVE_AS)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(messages.getText(DIALOG_FILES_XML), "xml");
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(cwd);
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED) + ".xml"));
                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File saveAs = chooser.getSelectedFile();
                    String name = saveAs.getName();
                    if (!name.toLowerCase().endsWith(".xml")) {
                        saveAs = new File(saveAs.getParent(), name + ".xml");
                    }
                    ifs.setName(saveAs.getName().replace(".xml", ""));
                    save(saveAs);
                    bus.post(ifs);
                }
            }
        });
        saveAs.setEnabled(false);
        file.add(saveAs);
        export = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_EXPORT)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.stop();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(messages.getText(DIALOG_FILES_PNG), "png");
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(cwd);
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED)+ ".png"));
                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File export = chooser.getSelectedFile();
                    String name = export.getName();
                    if (!name.toLowerCase().endsWith(".png")) {
                        export = new File(export.getParent(), name + ".png");
                    }
                    viewer.save(export);
                    cwd = export.getParentFile();
                }
                viewer.start();
            }
        });
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(export);
        print = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_PRINT)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean viewerStopped = false;
                if (current.equals(VIEWER)) {
                    viewerStopped = viewer.stop();
                }
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
                        Throwables.propagate(pe);
                    }
                }
                if (current.equals(VIEWER) && viewerStopped) {
                    viewer.start();
                }
            }
        });
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        print.setEnabled(false);
        file.add(print);
        if (platform != Platform.MAC_OS_X) {
            file.add(new AbstractAction(messages.getText(MENU_FILE_PREFERENCES)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    prefs.showDialog();
                }
            });
            JMenuItem quit = new JMenuItem(new AbstractAction(messages.getText(MENU_FILE_QUIT)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            file.add(quit);
        }
        menuBar.add(file);

        JMenu system = new JMenu(messages.getText(MENU_DISPLAY));
        ButtonGroup displayGroup = new ButtonGroup();
        showEditor = new JCheckBoxMenuItem(new AbstractAction(messages.getText(MENU_DISPLAY_EDITOR)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(EDITOR);
            }
        });
        system.add(showEditor);
        displayGroup.add(showEditor);
        showViewer = new JCheckBoxMenuItem(new AbstractAction(messages.getText(MENU_DISPLAY_VIEWER)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(VIEWER);
            }
        });
        system.add(showViewer);
        displayGroup.add(showViewer);
        showDetails = new JCheckBoxMenuItem(new AbstractAction(messages.getText(MENU_DISPLAY_DETAILS)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(DETAILS);
            }
        });
        system.add(showDetails);
        displayGroup.add(showDetails);
        menuBar.add(system);

        setJMenuBar(menuBar);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /** @see WindowListener#windowClosed(WindowEvent) */
            @Override
            public void windowClosed(WindowEvent e) {
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

        IFS untitled = new IFS();
        bus.post(untitled);

        // Check for post-startup task
        if (postponed != null) {
            SwingUtilities.invokeLater(postponed);
        }

        setVisible(true);
    }

    public void show(String name) {
        cards.show(view, name);
        current = name;
        switch (current) {
            case VIEWER:
                export.setEnabled(true);
                print.setEnabled(true);
                setResizable(false);
                viewer.reset();
                viewer.start();
                break;
            case DETAILS:
                export.setEnabled(false);
                print.setEnabled(true);
                setResizable(true);
                viewer.stop();
                break;
            case EDITOR:
                export.setEnabled(false);
                print.setEnabled(false);
                setResizable(true);
                viewer.stop();
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
        debug("Updated: %s", ifs);
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED));
        setTitle(name);
        if (!ifs.isEmpty()) {
            save.setEnabled(true);
            saveAs.setEnabled(true);
        }
        repaint();
    }

    public void save(File file) {
        print("Saving %s", file.getName());
        cwd = file.getParentFile();
        IFS.save(ifs, file);
    }

    public IFS load(File file) {
        print("Loading %s", file.getName());
        cwd = file.getParentFile();
        return IFS.load(file);
    }

    public boolean isFullScreen() { return fullScreen; }

    public boolean isColour() { return colour; }

    public boolean hasPalette() { return palette && colours != null; }

    public boolean isStealing() { return stealing && source != null; }

    public boolean isIFSColour() { return ifscolour; }

    public Set<Color> getColours() { return colours; }

    public String getPaletteFile() { return paletteFile; }

    public int getPaletteSize() { return paletteSize; }

    public Render getRender() { return render; }

    public Mode getMode() { return mode; }

    public long getSeed() { return seed; }

    /** Debug information shown. */
    public boolean isDebug() { return debug; }

    /** Small grid spacing. */
    public int getMinGrid() { return config.get(GRID_PROPERTY + ".min", DEFAULT_GRID_MIN); }

    /** Large grid spacing. */
    public int getMaxGrid() { return config.get(GRID_PROPERTY + ".max", DEFAULT_GRID_MAX); }

    /** Snap to grid distance. */
    public int getSnapGrid() { return config.get(GRID_PROPERTY + ".snap", DEFAULT_GRID_SNAP); }

    public BufferedImage getIcon() { return icon; }

    public JScrollPane getScroll() { return scroll; }

    public String getCurrent() { return current; }

    public JComponent getCurrentComponent() {
        switch (current) {
            case EDITOR: return editor;
            case VIEWER: return viewer;
            case DETAILS: return details;
            default: return null;
        }
    }

    public Viewer getViewer() { return viewer; }

    public Editor getEditor() { return editor; }

    public About getAbout() { return about; }

    public Preferences getPreferences() { return prefs; }

    public IFS getIFS() { return ifs; }

    public EventBus getEventBus() { return bus; }

    public int getThreads() { return threads; }

    /** Number of iterations each thread loop. */
    public long getIterations() { return config.get(ITERATIONS_PROPERTY, DEFAULT_ITERATIONS); }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /**
     * Listener for key-presses across the whole application.
     * <p>
     * Handles {@link KeyEvent#VK_TAB TAB} for switching between viewer and editor,
     * and {@literal S} for changing the random seed.
     *
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
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
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            if (viewer.isVisible() || details.isVisible()) {
                if (e.isShiftDown()) {
                    seed--;
                } else {
                    seed++;
                }
                loadColours();
                bus.post(ifs);
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

    public Messages getMessages() {
        return messages;
    }

    public void debug(String format, Object...varargs) {
        if (isDebug()) {
            output(System.err, DEBUG + format, varargs);
        }
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
        output(System.err, ERROR + format, varargs);
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
            Graphics2D g = splash.createGraphics();

            try {
                About.paintSplashText(g, splash.getSize().width, splash.getSize().height);
                splash.update();
            } catch (Exception e) {
                System.err.println("Failure painting splash text");
                e.printStackTrace(System.err);
                System.exit(0);
            } finally {
                g.dispose();
            }
        }

        // Start application
        SwingUtilities.invokeLater(() -> {
            Explorer explorer = new Explorer(argv);
            explorer.print("Starting Explorer UI");
            explorer.start();
        });
    }

}
