/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator;

import static com.google.common.collect.Sets.intersection;
import iterator.model.IFS;
import iterator.view.Editor;
import iterator.view.Status;
import iterator.view.ToolBar;
import iterator.view.Viewer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * IFS Explorer main class.
 *
 * @author andrew.international@gmail.com
 */
public class Explorer implements KeyListener {
    public static final Logger LOG = LoggerFactory.getLogger(Explorer.class);
    
    public static final String EXPLORER = "IFS Explorer";
    public static final String EDITOR = "Editor";
    public static final String VIEWER = "Viewer";
    
    public static final String FULLSCREEN_OPTION = "-F";
    
    private boolean fullScreen = false;
    
    private JFrame window;
    private JMenuBar menuBar;
    private Editor editor;
    private Viewer viewer;
    private Status status;
    private JPanel view;
    private CardLayout cards;
    private String current;
    
    private EventBus bus = new EventBus("explorer");

    public Explorer(String...argv) {
        // Parse arguments
        if (argv.length == 1 && argv[0].equalsIgnoreCase(FULLSCREEN_OPTION)) {
            fullScreen = true;
        }

        // Create explorer window
        window = new JFrame(EXPLORER);
        
        // Setup full-screen mode if required
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        if (fullScreen && device.isFullScreenSupported()) {
            window.setUndecorated(true);
            window.setResizable(false);
            device.setFullScreenWindow(window);
        }
    }

    @SuppressWarnings("serial")
    public void start() {
        JPanel content = new JPanel(new BorderLayout());

        menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(new AbstractAction("New IFS") {
            @Override
            public void actionPerformed(ActionEvent e) {
                IFS untitled = new IFS("Untitled");
                Explorer.this.bus.post(untitled);
            }
        });
        file.add(new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Preferences...") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        menuBar.add(file);
        JMenu system = new JMenu("Display");
        system.add(new AbstractAction("Editor") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(EDITOR);
            }
        });
        system.add(new AbstractAction("Viewer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                show(VIEWER);
            }
        });
        system.add(new AbstractAction("Details") {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        menuBar.add(system);
        window.setJMenuBar(menuBar);

        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowListener windowListener = new WindowAdapter() {
            /** @see WindowListener#windowClosed(WindowEvent) */
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);   
            }
        };
        window.addWindowListener(windowListener);
        
//        bar = new ToolBar(bus, this);
//        content.add(bar, BorderLayout.NORTH);
        
        status = new Status(bus, this);
        content.add(status, BorderLayout.SOUTH);
        
        editor = new Editor(bus, this);
        viewer = new Viewer(bus, this);

        cards = new CardLayout();
        view = new JPanel(cards);
        view.add(editor, EDITOR);
        view.add(viewer, VIEWER);
        content.add(view, BorderLayout.CENTER);
        show(EDITOR);

        window.setContentPane(content);

        if (!fullScreen) {
	        Dimension minimum = new Dimension(500, 500);
	        editor.setMinimumSize(minimum);
	        editor.setSize(minimum);
	        Dimension size = new Dimension(500, 500 + (status.getHeight() + menuBar.getHeight()));
	        window.setSize(size);
	        window.setMinimumSize(size);
	        window.addComponentListener(new ComponentAdapter() {
	            @Override
				public void componentResized(ComponentEvent e) {
	                Dimension s = window.getSize();
	                int side = Math.min(s.width, s.height - (status.getHeight() + menuBar.getHeight()));
	                window.setSize(side,  side + (status.getHeight() + menuBar.getHeight()));
	            }
            });
        }

        IFS untitled = new IFS("Untitled");
        bus.post(untitled);
        
        window.setVisible(true);
    }

    private void show(String name) {
        cards.show(view, name);
        current = name;
    }
    
    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == ' ') {
            if (current.equals(EDITOR)) show(VIEWER);
            if (current.equals(VIEWER)) show(EDITOR);
        }
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Explorer
     */
    public static void main(String...argv) throws Exception {
        Explorer explorer = new Explorer(argv);
        explorer.start();
    }
}
