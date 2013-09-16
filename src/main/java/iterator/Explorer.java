/*
 * Copyright 2012-2013 by Andrew Kennedy.
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

import static iterator.Config.*;

import iterator.model.IFS;
import iterator.view.Details;
import iterator.view.Editor;
import iterator.view.Viewer;

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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;

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
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer extends JFrame implements KeyListener, UncaughtExceptionHandler {
    /** serialVersionUID */
    private static final long serialVersionUID = -2003170067188344917L;

    public static enum Platform {
        LINUX,
        MAC_OS_X,
        WINDOWS,
        UNKNOWN;

        public static Platform getPlatform() {
            String osName = Strings.nullToEmpty(System.getProperty(OS_NAME_PROPERTY)).toUpperCase(Locale.UK).replace(' ', '_');
            try {
                // TODO Check behaviour on Windows variants
                return Platform.valueOf(osName);
            } catch (IllegalArgumentException iee) {
                // TODO Add other operating systems
                return UNKNOWN;
            }
        }
    }

    public static final String OS_NAME_PROPERTY = "os.name";

    private static final Config config = new Config();
    private File override;

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

    private boolean fullScreen = false;
    private boolean colour = false;
    private boolean palette = false;

    private Platform platform = Platform.getPlatform();
    private BufferedImage icon;
    private Preferences prefs;
    private About about;

    private IFS ifs;
    private List<Color> colours;
    private int paletteSize;
    private Dimension size, min = new Dimension(MIN_WINDOW_SIZE, MIN_WINDOW_SIZE);
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
    private JMenuItem export, save, saveAs;

    private EventBus bus;
    private Queue<Runnable> tasks = Queues.newArrayDeque();

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
                        palette = true;
                    } else if (argv[i].equalsIgnoreCase(CONFIG_OPTION_LONG)) {
                        if (argv.length >= i + 1) {
                            override = new File(argv[++i]);
                            if (!override.exists()) {
                                throw new IllegalArgumentException("Configuration file does not exist: " + override);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot parse option: " + argv[i]);
                    }
                } else if (i == argv.length - 1) {
                    // Last argument is a file
                    final File file = new File(argv[i]);
                    if (file.canRead()) {
                        // Add a task to load the file
                        tasks.add(new Runnable() {
                            public void run() {
                                IFS loaded = load(file);
                                loaded.setSize(getSize());
                                bus.post(loaded);
                            }
                        });
                    } else {
                        throw new IllegalArgumentException("Cannot load XML data file: " + argv[i]);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + argv[i]);
                }
            }
        }

        // Load configuration
        config.loadProperties(override);

        // Get window size configuration
        int w = Math.max(MIN_WINDOW_SIZE, config.get(WINDOW_PROPERTY + ".width", DEFAULT_WINDOW_SIZE));
        int h = Math.max(MIN_WINDOW_SIZE, config.get(WINDOW_PROPERTY + ".height", DEFAULT_WINDOW_SIZE));
        size = new Dimension(w, h);

        // Load icon resources
        icon = loadImage("icon.png");
        setIconImage(icon);

        // Load colour palette
        if (palette) {
            loadColours();
        }

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
                System.err.printf("Error while configuring OSX support: %s\n", ite.getCause().getMessage());
                System.exit(1);
            } catch (Exception e) {
                System.err.printf("Unable to configure OSX support: %s\n", e.getMessage());
            }
        }
    }

    public static BufferedImage loadImage(String name) {
        try {
            return ImageIO.read(Resources.getResource(name));
        } catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
    }

    public boolean isDebug() {
        return config.get(DEBUG_PROPERTY, Boolean.FALSE);
    }

    public List<Color> getColours() {
        return colours;
    }

    public int getPaletteSize() {
        return paletteSize;
    }

    public void loadColours() {
        String file = config.get(PALETTE_PROPERTY + ".file", DEFAULT_PALETTE_FILE);
        Long seed = config.get(PALETTE_PROPERTY + ".seed", DEFAULT_PALETTE_SEED);
        paletteSize = config.get(PALETTE_PROPERTY + ".size", DEFAULT_PALETTE_SIZE);
        try {
            BufferedImage image = ImageIO.read(Resources.getResource("palette/" + file + ".png"));
            colours = Lists.newArrayList();
            Random random = new Random(seed);
            while (colours.size() < paletteSize) {
                int x = random.nextInt(image.getWidth());
                int y = random.nextInt(image.getHeight());
                Color c = new Color(image.getRGB(x, y));
                if (!colours.contains(c)) {
                    colours.add(c);
                }
            }
        } catch (IOException ioe) {
            Throwables.propagate(ioe);
        }
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
                    loaded.setSize(getSize());
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
                FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Image Files", "png");
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(cwd);
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File(Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED)+ ".png"));
                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    viewer.save(file);
                    cwd = file.getParentFile();
                }
            }
        });
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(export);
        JMenuItem print = new JMenuItem(new AbstractAction("Print...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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

        while (tasks.size() > 0) {
            SwingUtilities.invokeLater(tasks.poll());
        }

        setVisible(true);
    }

    private void show(String name) {
        cards.show(view, name);
        current = name;
        if (name.equals(VIEWER)) {
            export.setEnabled(true);
            viewer.start();
        } else {
            export.setEnabled(false);
            viewer.stop();
        }
    }

    @Subscribe
    public void resized(Dimension resized) {
        size = resized.getSize();
    }

    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
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
             Closeables.close(writer, true);
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

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
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
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /** @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable) */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.printf("Thread %s (%d) caused %s: %s\n", t.getName(), t.getId(), e.getClass().getName(), e.getMessage());
        if (isDebug()) { 
            e.printStackTrace(System.err);
        }
        System.exit(1);
    }

    /**
     * Explorer
     */
    public static void main(final String...argv) throws Exception {
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
