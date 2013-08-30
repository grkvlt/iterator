/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import iterator.model.IFS;
import iterator.view.Details;
import iterator.view.Editor;
import iterator.view.Viewer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer extends JFrame implements KeyListener {
    /** serialVersionUID */
    private static final long serialVersionUID = -2003170067188344917L;

    public static final Logger LOG = LoggerFactory.getLogger(Explorer.class);

    public static enum Platform {
        LINUX,
        OSX,
        WINDOWS,
        UNKNOWN;

        private static final String OS_NAME = "os.name";

        public static Platform getPlatform() {
            String osName = Strings.nullToEmpty(System.getProperty(OS_NAME)).toLowerCase();
            if (osName.equals("linux")) return LINUX;
            if (osName.equals("mac os x")) return OSX;
            if (osName.startsWith("windows")) return WINDOWS;
            // TODO other operating systems
            return UNKNOWN;
        }
    }

    public static final String EXPLORER = "IFS Explorer";
    public static final String EDITOR = "Editor";
    public static final String VIEWER = "Viewer";
    public static final String DETAILS = "Details";

    public static final Integer SIZE = 600;

    public static final String FULLSCREEN_OPTION = "-F";
    public static final String COLOUR_OPTION = "-C";

    private boolean fullScreen = false;
    private boolean colour = false;
    
    private Platform platform = Platform.getPlatform();
    private BufferedImage icon, splash;
    private Preferences prefs;
    private About about;
    private Splash splashScreen;

    private IFS ifs;

    private JMenuBar menuBar;
    private Editor editor;
    private Viewer viewer;
    private Details details;
    private JPanel view;
    private CardLayout cards;
    private String current;
    private JCheckBoxMenuItem showEditor, showViewer, showDetails;
    private JMenuItem export, save, saveAs;

    private EventBus bus;

    public Explorer(String...argv) {
        super(EXPLORER);

        // Parse arguments
        if (argv.length != 0) {
            for (int i = 0; i < argv.length; i++) {
	            if (argv[i].equalsIgnoreCase(FULLSCREEN_OPTION)) {
	                fullScreen = true;
	            } else if (argv[i].equalsIgnoreCase(COLOUR_OPTION)) {
	                colour = true;
	            } else throw new IllegalArgumentException();
            }
        }

        // Setup full-screen mode if required
        if (fullScreen) {
            setUndecorated(true);
            setResizable(false);
	        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            setBounds(insets.left, insets.top, screen.width - (insets.left + insets.right), screen.height - (insets.top + insets.bottom));
        }

        // Setup event bus
        bus = new EventBus(EXPLORER);
        bus.register(this);

        // Load resources
        try {
	        icon = ImageIO.read(Resources.getResource("icon.png"));
	        splash = ImageIO.read(Resources.getResource("splash.png"));
        } catch (IOException ioe) {
            Throwables.propagate(ioe);
        }
        setIconImage(icon);

        // Load splash screen
        splashScreen = new Splash(bus, this);
        splashScreen.showDialog();

        // Load dialogs
        prefs = new Preferences(bus, this);
        about = new About(bus, this);

        // Setup platform specifics
        if (platform == Platform.OSX) {
            try {
	            Class<?> support = Class.forName("iterator.AppleSupport");
	            Constructor<?> ctor = support.getConstructor(EventBus.class, Explorer.class);
	            Method setup = support.getDeclaredMethod("setup");
	            Object apple = ctor.newInstance(bus, this);
	            setup.invoke(apple);
            } catch (InvocationTargetException ite) {
                LOG.error("Error while configuring OSX support: %s", ite.getCause().getMessage());
                System.exit(1);
            } catch (Exception e) {
                LOG.warn("Unable to configure OSX support: %s", e.getMessage());
            }
        }
    }

    @SuppressWarnings("serial")
    public void start() {
        JPanel content = new JPanel(new BorderLayout());

        menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        if (platform != Platform.OSX) {
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
                Explorer.this.bus.post(untitled);
            }
        });
        newIfs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        file.add(newIfs);
        JMenuItem open = new JMenuItem(new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML Files", "xml");
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(filter);
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    IFS loaded = load(chooser.getSelectedFile());
                    loaded.setSize(getSize());
	                Explorer.this.bus.post(loaded);
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
	                File saveAs = new File(ifs.getName() + ".xml");
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
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File((ifs.getName() == null ? IFS.UNTITLED : ifs.getName()) + ".xml"));
                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File saveAs = chooser.getSelectedFile();
                    ifs.setName(saveAs.getName().replace(".xml", ""));
                    save(saveAs);
	                Explorer.this.bus.post(ifs);
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
                chooser.setFileFilter(filter);
                chooser.setSelectedFile(new File((ifs.getName() == null ? IFS.UNTITLED : ifs.getName()) + ".png"));
		        int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    viewer.save(chooser.getSelectedFile());
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
        if (platform != Platform.OSX) {
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

        cards = new CardLayout();
        view = new JPanel(cards);
        view.add(editor, EDITOR);
        view.add(viewer, VIEWER);
        view.add(details, DETAILS);
        content.add(view, BorderLayout.CENTER);
        show(EDITOR);
        showEditor.setSelected(true);

        setContentPane(content);

        if (!fullScreen) {
	        Dimension minimum = new Dimension(SIZE, SIZE);
            editor.setMinimumSize(minimum);
            editor.setSize(minimum);
            viewer.setMinimumSize(minimum);
            viewer.setSize(minimum);
            pack();
            final int top = getInsets().top + (fullScreen ? 0 :  menuBar.getHeight());
	        Dimension size = new Dimension(SIZE, SIZE + top);
	        setSize(size);
	        setPreferredSize(size);
	        setMinimumSize(size);
	        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	        setLocation((screen.width / 2) - (size.width / 2), (screen.height / 2) - (size.height / 2));
	        addComponentListener(new ComponentAdapter() {
	            @Override
				public void componentResized(ComponentEvent e) {
	                Dimension s = getSize();
	                int side = Math.min(s.width, s.height - top);
	                setSize(side,  side + top);
	                Explorer.this.bus.post(new Dimension(side, side));
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
    public void update(IFS ifs) {
        this.ifs = ifs;
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, ifs.getName() == null ? IFS.UNTITLED : ifs.getName());
        setTitle(name);
        if (!ifs.getTransforms().isEmpty()) {
            save.setEnabled(true);
            saveAs.setEnabled(true);
        }
        repaint();
     }

    public void save(File file) {
       try {
          JAXBContext context = JAXBContext.newInstance(IFS.class);
          Marshaller marshaller = context.createMarshaller();
          marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
          FileWriter writer = new FileWriter(file);
          marshaller.marshal(ifs, writer);
          Closeables.closeQuietly(writer);
       } catch (Exception e) {
          throw Throwables.propagate(e);
       }
    }

    public IFS load(File file) {
       try {
          JAXBContext context = JAXBContext.newInstance(IFS.class);
          FileReader reader = new FileReader(file);
          Unmarshaller unmarshaller = context.createUnmarshaller();
          IFS ifs = (IFS) unmarshaller.unmarshal(reader);
          Closeables.closeQuietly(reader);
          return ifs;
       } catch (Exception e) {
          throw Throwables.propagate(e);
       }
    }
    
    public boolean isFullScreen() { return fullScreen; }
    
    public boolean isColour() { return colour; }

    /** Small grid spacing. */
    public int getMinGrid() { return 10; }

    /** Large grid spacing. */
    public int getMaxGrid() { return 50; }

    /** Snap to grid distance. */
    public int getSnapGrid() { return 10; }

    public BufferedImage getIcon() { return icon; }

    public BufferedImage getSplash() { return splash; }

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

    /**
     * Explorer
     */
    public static void main(final String...argv) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
		        Explorer explorer = new Explorer(argv);
		        explorer.start();
            }
        });
    }
}
