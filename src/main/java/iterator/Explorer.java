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

import static iterator.util.Config.DEBUG_PROPERTY;
import static iterator.util.Config.DEFAULT_GRID_MAX;
import static iterator.util.Config.DEFAULT_GRID_MIN;
import static iterator.util.Config.DEFAULT_GRID_SNAP;
import static iterator.util.Config.DEFAULT_PALETTE_FILE;
import static iterator.util.Config.DEFAULT_PALETTE_SEED;
import static iterator.util.Config.DEFAULT_PALETTE_SIZE;
import static iterator.util.Config.DEFAULT_WINDOW_SIZE;
import static iterator.util.Config.GRID_PROPERTY;
import static iterator.util.Config.MIN_THREADS;
import static iterator.util.Config.MIN_WINDOW_SIZE;
import static iterator.util.Config.MODE_COLOUR;
import static iterator.util.Config.MODE_GRAY;
import static iterator.util.Config.MODE_IFS_COLOUR;
import static iterator.util.Config.MODE_PALETTE;
import static iterator.util.Config.MODE_PROPERTY;
import static iterator.util.Config.MODE_STEALING;
import static iterator.util.Config.PALETTE_PROPERTY;
import static iterator.util.Config.RENDER_PROPERTY;
import static iterator.util.Config.THREADS_PROPERTY;
import static iterator.util.Config.WINDOW_PROPERTY;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Resources;

import iterator.model.IFS;
import iterator.util.Config;
import iterator.util.Config.Mode;
import iterator.util.Config.Render;
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
public class Explorer extends JFrame implements KeyListener, UncaughtExceptionHandler, Subscriber {
    /** serialVersionUID */
    private static final long serialVersionUID = -2003170067188344917L;

    public static final String BANNER =
            "   ___ _____ ____    _____            _                     \n" +
            "  |_ _|  ___/ ___|  | ____|_  ___ __ | | ___  _ __ ___ _ __ \n" +
            "   | || |_  \\___ \\  |  _| \\ \\/ / '_ \\| |/ _ \\| '__/ _ \\ '__|\n" +
            "   | ||  _|  ___) | | |___ >  <| |_) | | (_) | | |  __/ |   \n" +
            "  |___|_|   |____/  |_____/_/\\_\\ .__/|_|\\___/|_|  \\___|_|   \n" +
            "                               |_|                          \n" +
            "\n" +
            "    Iterated Function System Explorer Version %s\n" +
            "    Copyright 2012-2017 by Andrew Donald Kennedy\n" +
            "    Licensed under the Apache Software License, Version 2.0\n" +
            "    https://grkvlt.github.io/iterator/\n" +
            "\n";

    private static final Version version = Version.instance();

    public static final String EXPLORER = "IFS Explorer";
    public static final String EDITOR = "Editor";
    public static final String VIEWER = "Viewer";
    public static final String DETAILS = "Details";

    public static final String FULLSCREEN_OPTION = "-f";
    public static final String FULLSCREEN_OPTION_LONG = "--fullscreen";
    public static final String COLOUR_OPTION = "-c";
    public static final String COLOUR_OPTION_LONG = "--colour";
    public static final String PALETTE_OPTION = "-p";
    public static final String PALETTE_OPTION_LONG = "--palette";
    public static final String CONFIG_OPTION_LONG = "--config";

    private Config config;
    private File override;

    private boolean fullScreen = false;

    private boolean colour = false;
    private boolean palette = false;
    private boolean stealing = false;
    private boolean ifscolour = false;

    private Render render = Render.STANDARD;

    private Platform platform = Platform.getPlatform();
    private BufferedImage icon, source;
    private Preferences prefs;
    private About about;

    private IFS ifs;
    private Set<Color> colours;
    private int paletteSize;
    private String paletteFile;
    private long seed;
    private Dimension size;
    private Dimension min = new Dimension(MIN_WINDOW_SIZE, MIN_WINDOW_SIZE);
    private File cwd;

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

    private EventBus bus;
    private Runnable postponed;

    public Explorer(String...argv) {
        super(EXPLORER);
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
                        postponed = new Runnable() {
                            public void run() {
                                IFS loaded = load(file);
                                loaded.setSize(size);
                                bus.post(loaded);
                            }
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

        // Check colour mode configuration
        if (config.containsKey(MODE_PROPERTY)) {
            Mode mode = Mode.valueOf(config.get(MODE_PROPERTY).toUpperCase(Locale.UK));
            switch (mode) {
            case COLOUR:
                colour = true;
                palette = false;
                stealing = false;
                ifscolour = false;
                break;
            case PALETTE:
                colour = true;
                palette = true;
                stealing = false;
                ifscolour = false;
                break;
            case STEALING:
                colour = true;
                palette = true;
                stealing = true;
                ifscolour = false;
                break;
            case IFS_COLOUR:
                colour = true;
                palette = false;
                stealing = false;
                ifscolour = true;
                break;
            case GRAY:
                colour = false;
                palette = false;
                stealing = false;
                ifscolour = false;
                break;
            default:
                error("Cannot set colour mode: %s", mode);
            }
        }

        // Check rendering configuration
        if (config.containsKey(RENDER_PROPERTY)) {
            String value = config.get(RENDER_PROPERTY);
            render = Render.valueOf(value.toUpperCase(Locale.UK));
        }

        // Get window size configuration
        int w = Math.max(MIN_WINDOW_SIZE, config.get(WINDOW_PROPERTY + ".width", DEFAULT_WINDOW_SIZE));
        int h = Math.max(MIN_WINDOW_SIZE, config.get(WINDOW_PROPERTY + ".height", DEFAULT_WINDOW_SIZE));
        size = new Dimension(w, h);

        // Load icon resources
        icon = loadImage(Resources.getResource("icon.png"));
        setIconImage(icon);

        // Load colour palette
        if (palette) {
            seed = config.get(PALETTE_PROPERTY + ".seed", DEFAULT_PALETTE_SEED);
            if (Strings.isNullOrEmpty(paletteFile)) {
                paletteFile = config.get(PALETTE_PROPERTY + ".file", DEFAULT_PALETTE_FILE);
            }
            paletteSize = config.get(PALETTE_PROPERTY + ".size", DEFAULT_PALETTE_SIZE);
            loadColours();
        }
        debug("Configured %s: %s",
                colour ? palette ? stealing ? "stealing" : "palette" : ifscolour ? "ifscolour" : "colour" : "grayscale",
                palette ? paletteFile : colour ? "hsb" : "black");

        // Setup event bus
        bus = new EventBus(EXPLORER);
        bus.register(this);

        // Setup full-screen mode if required
        if (fullScreen) {
            setUndecorated(true);
            setResizable(false);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            setBounds(insets.left, insets.top, screen.width - (insets.left + insets.right), screen.height - (insets.top + insets.bottom));
        }

        // Load dialogs
        prefs = new Preferences(bus, this);
        about = new About(bus, this);

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

    public static BufferedImage loadImage(URL url) {
        try {
            return ImageIO.read(url);
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
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
        JMenu file = new JMenu("File");
        if (platform != Platform.MAC_OS_X) {
            file.add(new AbstractAction("About...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    about.showDialog();
                }
            });
        }
        JMenuItem newIfs = new JMenuItem(new AbstractAction("New IFS") {
            @Override
            public void actionPerformed(ActionEvent e) {
                IFS untitled = new IFS();
                show(EDITOR);
                bus.post(untitled);
            }
        });
        newIfs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(newIfs);
        JMenuItem open = new JMenuItem(new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
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
        save = new JMenuItem(new AbstractAction("Save") {
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
        saveAs = new JMenuItem(new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
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
        export = new JMenuItem(new AbstractAction("Export...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.stop();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Image Files", "png");
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
        print = new JMenuItem(new AbstractAction("Print...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.stop();
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setJobName(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED));
                PageFormat pf = job.defaultPage();
                if (getWidth() > getHeight()) {
                    pf.setOrientation(PageFormat.LANDSCAPE);
                } else {
                    pf.setOrientation(PageFormat.PORTRAIT);
                }
                job.setPrintable(viewer, pf);
                boolean ok = job.printDialog();
                if (ok) {
                    try {
                         job.print();
                    } catch (PrinterException pe) {
                        Throwables.propagate(pe);
                    }
                }
                viewer.start();
            }
        });
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        print.setEnabled(false);
        file.add(print);
        if (platform != Platform.MAC_OS_X) {
            file.add(new AbstractAction("Preferences...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            });
            JMenuItem quit = new JMenuItem(new AbstractAction("Quit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            file.add(quit);
        }
        menuBar.add(file);

        JMenu system = new JMenu("Display");
        showEditor = new JCheckBoxMenuItem(new AbstractAction("Editor") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(EDITOR);
            }
        });
        showViewer = new JCheckBoxMenuItem(new AbstractAction("Viewer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(VIEWER);
            }
        });
        showDetails = new JCheckBoxMenuItem(new AbstractAction("Details") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(DETAILS);
            }
        });
        system.add(showEditor);
        system.add(showViewer);
        system.add(showDetails);
        ButtonGroup displayGroup = new ButtonGroup();
        displayGroup.add(showEditor);
        displayGroup.add(showViewer);
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

        editor = new Editor(bus, this);
        viewer = new Viewer(bus, this);
        details = new Details(bus, this);
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
        if (name.equals(VIEWER)) {
            export.setEnabled(true);
            print.setEnabled(true);
            viewer.reset();
            viewer.start();
        } else {
            export.setEnabled(false);
            print.setEnabled(false);
            viewer.stop();
        }
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension resized) {
        size = resized.getSize();
        if (isDebug()) System.err.println("Resized: " + size.width + ", " + size.height);
    }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS updated) {
        ifs = updated;
        if (isDebug()) System.err.println("Updated: " + ifs);
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED));
        setTitle(name);
        if (!ifs.isEmpty()) {
            save.setEnabled(true);
            saveAs.setEnabled(true);
        }
        repaint();
    }

    public void save(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            JAXBContext context = JAXBContext.newInstance(IFS.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(ifs, writer);
            cwd = file.getParentFile();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public IFS load(File file) {
        try (FileReader reader = new FileReader(file)) {
            JAXBContext context = JAXBContext.newInstance(IFS.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            IFS ifs = (IFS) unmarshaller.unmarshal(reader);
            cwd = file.getParentFile();
            return ifs;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean isFullScreen() { return fullScreen; }

    public boolean isColour() { return colour; }

    public boolean hasPalette() { return palette && colours != null; }

    public boolean isStealing() { return stealing && source != null; }

    public boolean isIFSColour() { return ifscolour; }

    public Set<Color> getColours() { return colours; }

    public int getPaletteSize() { return paletteSize; }

    public Render getRenderMode() { return render; }

    public long getSeed() { return seed; }

    /** Debug information shown. */
    public boolean isDebug() { return config.get(DEBUG_PROPERTY, Boolean.FALSE); }

    /** Small grid spacing. */
    public int getMinGrid() { return config.get(GRID_PROPERTY + ".min", DEFAULT_GRID_MIN); }

    /** Large grid spacing. */
    public int getMaxGrid() { return config.get(GRID_PROPERTY + ".max", DEFAULT_GRID_MAX); }

    /** Snap to grid distance. */
    public int getSnapGrid() { return config.get(GRID_PROPERTY + ".snap", DEFAULT_GRID_SNAP); }

    public BufferedImage getIcon() { return icon; }

    public JScrollPane getScroll() { return scroll; }

    public Viewer getViewer() { return viewer; }

    public Editor getEditor() { return editor; }

    public About getAbout() { return about; }

    public Preferences getPreferences() { return prefs; }

    public IFS getIFS() { return ifs; }

    public EventBus getEventBus() { return bus; }

    public int getThreads() {
        Integer threads = Math.max(Runtime.getRuntime().availableProcessors() / 2, MIN_THREADS);
        return config.get(THREADS_PROPERTY, threads);
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

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
    public void keyReleased(KeyEvent e) {
    }

    public void debug(String format, Object...varargs) {
        if (isDebug()) {
            output(System.err, format, varargs);
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
        output(System.err, format, varargs);
        if (t.isPresent() && isDebug()) {
            t.get().printStackTrace(System.err);
        }
        System.exit(1);
    }

    public void print(String format, Object...varargs) {
        output(System.out, format, varargs);
    }

    protected void output(PrintStream out, String format, Object...varargs) {
        String output = String.format(format, varargs);
        if (!output.endsWith("\n")) output = output.concat("\n");
        out.print(output);
    }

    /** @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable) */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        error(Optional.of(e), "Error: Thread %s (%d) caused %s: %s", t.getName(), t.getId(), e.getClass().getName(), e.getMessage());
    }

    /**
     * Explorer.
     */
    public static void main(final String...argv) throws Exception {
        // Print text banner
        System.out.printf(BANNER, version.get());

        // Load splash screen first
        Splash splashScreen = new Splash();
        splashScreen.showDialog();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Start application
                Explorer explorer = new Explorer(argv);
                explorer.start();
            }
        });
    }

}
