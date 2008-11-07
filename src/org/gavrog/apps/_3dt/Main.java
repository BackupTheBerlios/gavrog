/*
   Copyright 2008 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.media.opengl.GLException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.gavrog.box.gui.Config;
import org.gavrog.box.gui.ExtensionFilter;
import org.gavrog.box.gui.Invoke;
import org.gavrog.box.gui.OptionCheckBox;
import org.gavrog.box.gui.OptionColorBox;
import org.gavrog.box.gui.OptionInputBox;
import org.gavrog.box.gui.OptionSliderBox;
import org.gavrog.box.gui.TextAreaOutputStream;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.graphics.Surface;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.io.Output;
import org.gavrog.joss.tilings.Tiling;
import org.gavrog.joss.tilings.Tiling.Facet;
import org.gavrog.joss.tilings.Tiling.Tile;

import buoy.event.CommandEvent;
import buoy.event.EventSource;
import buoy.event.MouseClickedEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BDialog;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BOutline;
import buoy.widget.BScrollPane;
import buoy.widget.BSeparator;
import buoy.widget.BSplitPane;
import buoy.widget.BStandardDialog;
import buoy.widget.BTabbedPane;
import buoy.widget.BorderContainer;
import buoy.widget.ColumnContainer;
import buoy.widget.LayoutInfo;
import buoy.widget.RowContainer;
import buoy.widget.Widget;
import de.jreality.geometry.BallAndStickFactory;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedLineSetFactory;
import de.jreality.geometry.SphereUtility;
import de.jreality.geometry.TubeUtility;
import de.jreality.io.JrScene;
import de.jreality.jogl.JOGLRenderer;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Transformation;
import de.jreality.scene.Viewer;
import de.jreality.scene.pick.PickResult;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.Tool;
import de.jreality.scene.tool.ToolContext;
import de.jreality.shader.CommonAttributes;
import de.jreality.softviewer.SoftViewer;
import de.jreality.sunflow.RenderOptions;
import de.jreality.sunflow.Sunflow;
import de.jreality.tools.ClickWheelCameraZoomTool;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.ui.viewerapp.ViewerAppMenu;
import de.jreality.ui.viewerapp.ViewerSwitch;
import de.jreality.ui.viewerapp.actions.AbstractJrAction;
import de.jreality.ui.viewerapp.actions.file.ExportImage;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;
import de.jtem.beans.DimensionPanel;

public class Main extends EventSource {
	// --- some constants used in the GUI
    final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);
	final private static Insets defaultInsets = new Insets(5, 5, 5, 5);

	private final static String TILING_MENU = "Tiling";
	private final static String NET_MENU = "Net";
	private final static String HELP_MENU = "Help";
	
	private final static String configFileName = System.getProperty("user.home")
			+ "/.3dt";
	
	// --- file choosers
	final private FileChooser inFileChooser =
		new FileChooser(FileChooser.OPEN_FILE);
	final private FileChooser outNetChooser =
		new FileChooser(FileChooser.SAVE_FILE);
	final private FileChooser outTilingChooser =
		new FileChooser(FileChooser.SAVE_FILE);
	final private FileChooser outSceneChooser =
		new FileChooser(FileChooser.SAVE_FILE);
	final private FileChooser outSunflowChooser =
		new FileChooser(FileChooser.SAVE_FILE);
	final private DimensionPanel dimPanel = new DimensionPanel();
	
    // --- tile options
	private int subdivisionLevel = 2;
	private int tileRelaxationSteps = 3;
	private double edgeWidth = 0.02;
	private int edgeRoundingLevel = 2;
	private Color edgeColor = Color.BLACK;
	private double edgeOpacity = 1.0;
	private boolean smoothFaces = true;
	
	// --- display options
	private boolean drawEdges = true;
	private boolean drawFaces = true;
	private double tileSize = 0.9;
	private boolean showUnitCell = false;
    private Color unitCellColor = Color.BLACK;
    private double unitCellEdgeWidth = 0.02;
    
    // --- net options
    private boolean showNet = true;
    private Color netEdgeColor = Color.BLACK;
    private Color netNodeColor = Color.RED;
    private double netEdgeRadius = 0.05;
    private double netNodeRadius = 0.075;
    
    // --- scene options
    private int minX = 0;
    private int maxX = 0;
    private int minY = 0;
    private int maxY = 0;
    private int minZ = 0;
    private int maxZ = 0;
    private boolean clearOnFill = true;
    
	// --- material options
	private double ambientCoefficient = 0.0;
	private Color ambientColor = Color.WHITE;
	private double diffuseCoefficient = 0.8;
	private double specularCoefficient = 0.1;
	private double specularExponent = 15.0;
	private Color specularColor = Color.WHITE;
	private double faceTransparency = 0.0;
	
    // --- embedding options
    private int equalEdgePriority = 2;
    private int embedderStepLimit = 100000;
    private boolean useBarycentricPositions = false;
    
    // --- camera options
    private Color backgroundColor = Color.WHITE;
    private boolean useFog = false;
    private double fogDensity = 0.1;
    private Color fogColor = Color.WHITE;
    private boolean fogToBackground = true;
    private double fieldOfView = 25.0;
    
    // --- the currently active interface options
    private InterfaceOptions ui = new InterfaceOptions();
    
    // --- the current document and the document list in which it lives
    private List<Document> documents;
	private int tilingCounter;
    private Document currentDocument;
    
    // --- mapping of display list items to scene graph nodes and vice versa
    private Map<SceneGraphComponent, DisplayList.Item> node2item =
    	new HashMap<SceneGraphComponent, DisplayList.Item>();
    private Map<DisplayList.Item, SceneGraphComponent> item2node =
    	new HashMap<DisplayList.Item, SceneGraphComponent>();

    // --- the last thing selected
	private DisplayList.Item selectedItem = null;
	private int selectedFace = -1;
    
    // --- gui elements and such
	private BDialog aboutFrame;
	private BDialog controlsFrame;
	private BStandardDialog inputDialog = new BStandardDialog();
	private BStandardDialog messageDialog = new BStandardDialog();
	private Map<String, BLabel> tInfoFields;
	private Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);
	private Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	// --- scene graph components and associated properties
	final private ViewerApp viewerApp;
	private int previousViewer = -1;
    final private JrScene scene;
    final private SceneGraphComponent world;

    final public static SceneGraphComponent sphereTemplate = new SceneGraphComponent();
	static {
		sphereTemplate.setGeometry(SphereUtility.sphericalPatch(0.0, 0.0,
				360.0, 180.0, 40, 20, 1.0));
	}
    
    private SceneGraphComponent tiling;
    private SceneGraphComponent net;
	private SceneGraphComponent unitCell;
    private SceneGraphComponent templates[];
    private Appearance materials[];
    
    // --- command line options
	private boolean expertMode;
    
    /**
     * Constructs an instance.
     * @param args command-line arguments
     */
    public Main(final String[] args) {
    	// --- parse command line options
    	String infilename = null;
    	int i = 0;
    	if (args.length > i && args[i].equals("-x")) {
    		this.expertMode = true;
    		++i;
    	} else {
    		this.expertMode = false;
    	}
    	if (args.length > i) {
    		infilename = args[i];
    	}
    	
        // --- retrieved stored user options
		loadOptions();

		// --- initialize the file choosers
		setupFileChoosers();
		
		// --- create the viewing infrastructure
		this.world = SceneGraphUtility.createFullSceneGraphComponent("World");
		final Appearance a = new Appearance();
		updateAppearance(a);
		this.world.setAppearance(a);
		
		viewerApp = new ViewerApp(world);
		this.scene = viewerApp.getJrScene();
        viewerApp.setAttachNavigator(false);
        viewerApp.setAttachBeanShell(false);
        
        // --- create a root node for the tiling
        this.tiling = new SceneGraphComponent("Tiling");
        this.tiling.addTool(new SelectionTool());
        
        // --- create a root node for the net
        this.net = new SceneGraphComponent("Net");
        
        // --- create a node for the unit cell
        this.unitCell = new SceneGraphComponent("UnitCell");
        
        // --- remove the encompass tool (we'll have a menu entry for that)
        final SceneGraphComponent root = this.scene.getSceneRoot()
				.getChildComponent(0);
        final List tools = root.getTools();
        Tool encompass = null;
        for (final Iterator iter = tools.iterator(); iter.hasNext();) {
            final Tool t = (Tool) iter.next();
            if (t instanceof de.jreality.tools.EncompassTool) {
                encompass = t;
                break;
            }
        }
        if (encompass != null) {
            root.removeTool(encompass);
        }
        
        // --- add the mouse wheel zoom tool
        this.world.addTool(new ClickWheelCameraZoomTool());
        
        // --- change the menu
        modifyDefaultMenu(viewerApp.getMenu());
        
        // --- set up the viewer window
        updateCamera();
        viewerApp.update();
        viewerApp.display();
        viewerApp.getFrame().setTitle("3dt Viewer");
        updateViewerSize();
        
        viewerApp.getViewingComponent().addComponentListener(
        	new ComponentListener() {
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}

			public void componentResized(ComponentEvent e) {
				ui.setViewerWidth(viewerApp.getViewingComponent().getWidth());
				ui.setViewerHeight(viewerApp.getViewingComponent().getHeight());
				saveOptions();
			}
        });
        
        // --- check if an installed OpenGL viewer actually works
        Invoke.andWait(new Runnable() { public void run() { checkOpenGL(); }});
        
        // --- show the controls window
        Invoke.andWait(new Runnable() { public void run() { showControls(); }});
        
        // --- open a file if specified on the command line
        if (infilename != null) {
        	openFile(infilename);
        }
    }

    private void modifyDefaultMenu(final ViewerAppMenu menu) {
    	// --- remove the Edit menu
    	menu.removeMenu(ViewerAppMenu.EDIT_MENU);
    	
        // --- create and populate a new Tiling menu
        menu.addMenu(new JMenu(TILING_MENU), 1);
        menu.addAction(actionFirst(), TILING_MENU);
        menu.addAction(actionNext(), TILING_MENU);
        menu.addAction(actionPrevious(), TILING_MENU);
        menu.addAction(actionLast(), TILING_MENU);
        menu.addSeparator(TILING_MENU);
        menu.addAction(actionJump(), TILING_MENU);
        menu.addAction(actionSearch(), TILING_MENU);
        menu.addSeparator(TILING_MENU);
        menu.addAction(actionDualize(), TILING_MENU);
        menu.addAction(actionSymmetrize(), TILING_MENU);
        menu.addSeparator(TILING_MENU);
        menu.addAction(actionRecolor(), TILING_MENU);
        
        // --- create and populate a new Net menu
        menu.addMenu(new JMenu(NET_MENU), 2);
        menu.addAction(actionUpdateNet(), NET_MENU);
        menu.addAction(actionGrowNet(), NET_MENU);
        menu.addAction(actionClearNet(), NET_MENU);
        
        // --- modify the File menu
        for (int i = 0; i < 5; ++i) {
            menu.removeMenuItem(ViewerAppMenu.FILE_MENU, 0);
        }
        
        int k = 0;
        menu.addAction(actionOpen(), ViewerAppMenu.FILE_MENU, k++);
        menu.addAction(actionSaveTiling(), ViewerAppMenu.FILE_MENU, k++);
        menu.addAction(actionSaveNet(), ViewerAppMenu.FILE_MENU, k++);
        menu.addAction(actionSaveScene(), ViewerAppMenu.FILE_MENU, k++);
        menu.addSeparator(ViewerAppMenu.FILE_MENU, k++);
        
        menu.removeMenuItem(ViewerAppMenu.FILE_MENU, k);
        menu.addAction(new ExportImage("Screen Shot...", viewerApp
				.getViewerSwitch(), null) {
        	public void actionPerformed(ActionEvent e) {
        		forceSoftwareViewer();
        		super.actionPerformed(e);
        		restoreViewer();
        	}
        }, ViewerAppMenu.FILE_MENU, k++);
        menu.addAction(actionSunflowRender(), ViewerAppMenu.FILE_MENU, k++);
        menu.addAction(actionSunflowPreview(), ViewerAppMenu.FILE_MENU, k++);
        
        ++k; // jump over separator

        // --- modify the View menu
        if (!this.expertMode) {
			for (int i = 0; i < 15; ++i) {
				menu.removeMenuItem(ViewerAppMenu.VIEW_MENU, 0);
			}
		} else {
			menu.removeMenuItem(ViewerAppMenu.VIEW_MENU, 13);
		}
        k = 0;
        menu.addAction(actionEncompass(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionViewAlong(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addSeparator(ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionXView(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionYView(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionZView(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(action011View(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(action101View(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(action110View(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(action111View(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addSeparator(ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionRotateRight(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionRotateLeft(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionRotateUp(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionRotateDown(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionRotateClockwise(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionRotateCounterClockwise(), ViewerAppMenu.VIEW_MENU,
        		k++);
        menu.addSeparator(ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionShowControls(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addSeparator(ViewerAppMenu.VIEW_MENU, k++);
        
        // --- create a help menu
        menu.addMenu(new JMenu(HELP_MENU));
        menu.addAction(actionAbout(), HELP_MENU);
    }
    
    private void setupFileChoosers() {
    	inFileChooser.setTitle("Open data file");
		inFileChooser.addChoosableFileFilter(new ExtensionFilter("ds",
				"Delaney-Dress Symbol Files"));
		inFileChooser.addChoosableFileFilter(new ExtensionFilter("cgd",
				"Geometric Description Files"));
		inFileChooser.addChoosableFileFilter(new ExtensionFilter("gsl",
				"Gavrog Scene Files"));
		inFileChooser.addChoosableFileFilter(new ExtensionFilter(new String[] {
				"cgd", "ds", "gsl" }, "All 3dt Files"));

		outNetChooser.setTitle("Save net");
		outNetChooser.addChoosableFileFilter(new ExtensionFilter("pgr",
				"Raw Periodic Net Files (for Systre)"));
		outNetChooser.setAppendEnabled(true);

		outTilingChooser.setTitle("Save tiling");
		outTilingChooser.addChoosableFileFilter(new ExtensionFilter("ds",
				"Delaney-Dress Symbol Files"));
		outTilingChooser.setAppendEnabled(true);

		outSceneChooser.setTitle("Save scene");
		outSceneChooser.addChoosableFileFilter(new ExtensionFilter("gsl",
				"Gavrog Scene Files"));
		outSceneChooser.setAppendEnabled(false);

		outSunflowChooser.setTitle("Save image");
		outSunflowChooser.addChoosableFileFilter(new ExtensionFilter(
				new String[] { "png", "tga", "hdr" }, "Images files"));
		dimPanel.setDimension(new Dimension(800, 600));
		TitledBorder title = BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Dimension");
		dimPanel.setBorder(title);
		outSunflowChooser.setAccessory(dimPanel);
		outSunflowChooser.setAppendEnabled(false);
    }
    
    private String getInput(final String message, final String title,
    		final String defaultVal) {
    	inputDialog.setMessage(message);
    	inputDialog.setTitle(title);
    	inputDialog.setStyle(BStandardDialog.PLAIN);
    	return inputDialog.showInputDialog(null, null, defaultVal);
    }
    
    private void messageBox(final String message, final String title,
    		final BStandardDialog.Style style) {
    	messageDialog.setMessage(message);
    	messageDialog.setTitle(title);
    	messageDialog.setStyle(style);
    	messageDialog.showMessageDialog(null);
    }
    
    private Action actionAbout() {
    	final String name = "About 3dt";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					showAbout();
				}
			}, null, null);
    	}
    	return ActionRegistry.instance().get(name);
    }
    
    private Action actionShowControls() {
		final String name = "Show Controls";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					showControls();
				}
			}, null, null);
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionOpen() {
		final String name = "Open...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final File file = inFileChooser.pickFile(
							ui.getLastInputPath(), null);
					if (file == null) return;
					ui.setLastInputPath(file);
					saveOptions();
					openFile(file.getAbsolutePath());
				}
			}, "Open a tiling file",
			KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		}
		return ActionRegistry.instance().get(name);
    }
    
    private Action actionSaveTiling() {
		final String name = "Save Tiling...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final File file =outTilingChooser.pickFile(
							ui.getLastTilingOutputPath(), "ds");
					if (file == null) return;
					ui.setLastTilingOutputPath(file);
					saveOptions();
                    try {
                    	final DelaneySymbol ds = doc().getSymbol();
                    	final boolean append = outTilingChooser.getAppend();
                    	final Writer out = new FileWriter(file, append);
                    	for (final String key: tInfoFields.keySet()) {
                    		if (!key.startsWith("_")) {
                    			out.write("#@ info " + key + " = " +
                    					tInfoFields.get(key).getText() + "\n");
                    		}
                    	}
                    	if (doc().getName() != null) {
                    		out.write("#@ name " + doc().getName() + "\n");
                    	}
                    	out.write(ds.canonical().flat().toString());
                    	out.write("\n");
                    	out.flush();
                    	out.close();
                    } catch (IOException ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote file " + file.getName() + ".");
				}
			}, "Save the raw tiling as a Delaney-Dress symbol", null);
		}
		return ActionRegistry.instance().get(name);
    }
    
    private Action actionSaveNet() {
		final String name = "Save Net...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final File file = outNetChooser.pickFile(
							ui.getLastNetOutputPath(), "pgr");
					if (file == null) return;
					ui.setLastNetOutputPath(file);
					saveOptions();
                    try {
                    	final boolean append = outNetChooser.getAppend();
                    	final Writer out = new FileWriter(file, append);
                    	Output.writePGR(out, doc().getNet(), doc().getName());
                    	out.flush();
                    	out.close();
                    } catch (IOException ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote file " + file.getName() + ".");
				}
			}, "Save the raw net as a Systre file", null);
		}
		return ActionRegistry.instance().get(name);
    }
    
    private Action actionSaveScene() {
		final String name = "Save Scene...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final File file = outSceneChooser.pickFile(
							ui.getLastSceneOutputPath(), "gsl");
					if (file == null) return;
					ui.setLastSceneOutputPath(file);
					saveOptions();
                    try {
                    	final Writer out = new FileWriter(file);
                    	doc().setTransformation(getViewingTransformation());
                    	out.write(doc().toXML());
                    	out.flush();
                    	out.close();
                    } catch (IOException ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote file " + file.getName() + ".");
				}
			}, "Save the scene", null);
		}
		return ActionRegistry.instance().get(name);
    }
    
    private Action actionSunflowRender() {
		final String name = "Raytraced Image...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					dimPanel.setDimension(viewerApp.getCurrentViewer()
							.getViewingComponentSize());
					final File file = outSunflowChooser.pickFile(
							ui.getLastSunflowRenderPath(), "png");
					if (file == null) return;
					ui.setLastSunflowRenderPath(file);
					saveOptions();
                    try {
                    	final RenderOptions opts = new RenderOptions();
                    	opts.setProgressiveRender(false);
                    	opts.setAaMin(0);
                    	opts.setAaMax(2);
                    	opts.setGiEngine("ambocc");
                    	opts.setFilter("mitchell");
                    	Sunflow.renderAndSave(viewerApp.getCurrentViewer(),
								opts, dimPanel.getDimension(), file);
                    } catch (Throwable ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote raytraced image to file " + file.getName() + ".");
				}
			}, "Render the scene using the Sunflow raytracer",
				KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		}
		return ActionRegistry.instance().get(name);
    }
    
    private Action actionSunflowPreview() {
		final String name = "Preview Raytraced...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    try {
                    	final RenderOptions opts = new RenderOptions();
                    	opts.setProgressiveRender(true);
                    	final Viewer v = viewerApp.getCurrentViewer();
                    	Sunflow.render(v, v.getViewingComponentSize(), opts);
                    } catch (Throwable ex) {
                    	log(ex.toString());
                    	return;
                    }
				}
			}, "Preview the Sunflow render",
				KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
		}
		return ActionRegistry.instance().get(name);
    }
    
    private Action actionFirst() {
		final String name = "First";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    doTiling(1);
				}
			}, "Display the first tiling in this file",
			KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionNext() {
		final String name = "Next";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    doTiling(tilingCounter + 1);
				}
			}, "Display the next tiling in this file",
			KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionPrevious() {
		final String name = "Previous";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    doTiling(tilingCounter - 1);
				}
			}, "Display the previous tiling in this file",
			KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionLast() {
		final String name = "Last";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    doTiling(documents.size());
				}
			}, "Display the last tiling in this file",
			KeyStroke.getKeyStroke(KeyEvent.VK_L, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionJump() {
		final String name = "Jump To...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final String input = getInput("Jump to tiling #:",
									"3dt Jump To",
									String.valueOf(tilingCounter + 1));
					final int n;
					try {
						n = Integer.parseInt(input);
					} catch (final NumberFormatException ex) {
						return;
					}
					doTiling(n);
				}
			}, "Jump to a specific tiling",
			KeyStroke.getKeyStroke(KeyEvent.VK_J, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionSearch() {
		final String name = "Search...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final String input = getInput(
							"Find tiling by name pattern or number:",
							"3dt Search", String.valueOf(tilingCounter + 1));
                    if (input != null && !input.equals("")) {
                    	final Pattern p;
                    	try {
                    		p = Pattern.compile(input, Pattern.CASE_INSENSITIVE);
                    	} catch (final PatternSyntaxException ex) {
							messageBox(ex.getMessage(), "3dt Search",
									BStandardDialog.INFORMATION);
							return;
                    	}
	                    if (documents != null) {
							for (int n = 0; n < documents.size(); ++n) {
								String name = documents .get(n).getName();
								if (name != null && p.matcher(name).find()) {
									doTiling(n + 1);
									return;
								}
							}
							try {
								final int n = Integer.parseInt(input);
								doTiling(n);
							} catch (NumberFormatException ex) {
								messageBox("Found no tiling matching '"
										+ input + "'.", "3dt Search",
										BStandardDialog.INFORMATION);
							}
						}
					}
				}
			}, "Search for a tiling by name",
			KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionDualize() {
		final String name = "Dualize";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final DSymbol ds = doc().getSymbol();
					if (ds.equals(ds.dual())) {
						messageBox("The tiling is self-dual.", "3dt Dualize",
								BStandardDialog.INFORMATION);
						return;
					}
    				final String name = doc().getName();
    				final String newName;
    				if (name != null) {
    					newName = name + " (dual)";
    				} else {
    					newName = "#" + tilingCounter + " dual";
    				}
    				final Document dual = new Document(ds.dual(), newName);
    				documents.add(tilingCounter, dual);
    				doTiling(tilingCounter + 1);
    			}
    		}, "Dualize the current tiling", null);
    	}
    	return ActionRegistry.instance().get(name);
    }
    
    private Action actionSymmetrize() {
		final String name = "Max. Symmetry";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final DSymbol ds = doc().getSymbol();
					if (ds.isMinimal()) {
						messageBox("The tiling is already maximally symmetric.",
								"3dt Max Symmetry", BStandardDialog.INFORMATION);
						return;
					}
    				final String name = doc().getName();
    				final String newName;
    				if (name != null) {
    					newName = name + " (symmetric)";
    				} else {
    					newName = "#" + tilingCounter + " symmetric";
    				}
    				final Document minimal =
    					new Document((DSymbol) ds.minimal(), newName);
    				documents.add(tilingCounter, minimal);
    				doTiling(tilingCounter + 1);
    			}
    		}, "Maximize the symmetry of the current tiling", null);
    	}
    	return ActionRegistry.instance().get(name);
    }
    
    private Action actionRecolor() {
		final String name = "Recolor";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    doc().randomlyRecolorTiles();
                    suspendRendering();
                    updateMaterials();
                    resumeRendering();
                }
            }, "Pick new random colors for tiles",
            KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        }
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionUpdateNet() {
		final String name = "Update Net";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final List<DisplayList.Item> tiles =
						new ArrayList<DisplayList.Item>();
                    for (final DisplayList.Item item: doc()) {
                    	if (item.isTile()) {
                    		tiles.add(item);
                    	}
                    }
                    suspendRendering();
                    for (final DisplayList.Item item: tiles) {
                    	makeTileOutline(item.getTile(), item.getShift());
                    }
                    resumeRendering();
                }
            }, "Add all edges and nodes in the outlines of visible tiles.",
            null);
        }
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionGrowNet() {
		final String name = "Grow Net";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final List<DisplayList.Item> nodes =
						new LinkedList<DisplayList.Item>();
					for (DisplayList.Item item: doc()) {
						if (item.isNode()) {
							nodes.add(item);
						}
					}
					suspendRendering();
					int count = 0;
					for (DisplayList.Item item: nodes) {
						if (item.isNode()) {
							count += doc().connectToExisting(item);
						}
					}
					if (count == 0) {
						for (DisplayList.Item item: nodes) {
							if (item.isNode()) {
								count += doc().addIncident(item);
							}
						}
					}
					resumeRendering();
				}
			}, "Add all missing edges between or all edges incident with "
				+ "visible nodes.",
				KeyStroke.getKeyStroke(KeyEvent.VK_G, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionClearNet() {
		final String name = "Clear Net";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    suspendRendering();
                    doc().removeAllEdges();
                    doc().removeAllNodes();
                    resumeRendering();
                }
            }, "Remove all nodes and edges of the net.", null);
        }
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionAddTile() {
		final String name = "Add Tile";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null && selectedFace >= 0) {
						doc().addNeighbor(selectedItem, selectedFace);
					}
				}
    		}, "Add a tile at the selected face.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionRemoveTile() {
		final String name = "Remove Tile";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						doc().remove(selectedItem);
					}
				}
    		}, "Remove the selected tile.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionRemoveTileClass() {
		final String name = "Remove Tile Class";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						doc().removeKind(selectedItem);
					}
				}
    		}, "Remove the selected tile and all its symmetric images.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionAddFacetOutline() {
    	final String name = "Add Facet Outline to Net";
    	if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null && selectedFace >= 0) {
						makeFacetOutline(selectedItem.getTile().facet(
								selectedFace), selectedItem.getShift());
					}
				}
			}, "Add all edges around the selected facet to the net.", null);
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionAddTileOutline() {
    	final String name = "Add Tile Outline to Net";
    	if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						makeTileOutline(selectedItem.getTile(),
								selectedItem.getShift());
					}
				}
			}, "Add all edges around the selected tile to the net.", null);
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Color pickColor(final String title, final Color oldColor) {
    	return JColorChooser.showDialog(viewerApp.getFrame(), title, oldColor);
	}
    
    private Action actionRecolorTileClass() {
    	final String name = "Recolor Tile Class";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem == null) {
						return;
					}
					final int kind = selectedItem.getTile().getKind();
					final Color c= doc().getTileClassColor(kind);
					final Color picked = pickColor("Set Tile Class Color", c);
					if (picked == null) {
						return;
					}
					doc().setTileClassColor(kind, picked);
                    suspendRendering();
                    updateMaterials();
                    resumeRendering();
				}
			}, "Set the color for all tiles of the selected kind.",
			KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK));
		}
        return ActionRegistry.instance().get(name);
	}
    
    private Action actionRecolorTile() {
    	final String name = "Recolor Tile";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem == null) {
						return;
					}
					Color c = doc().color(selectedItem);
					if (c == null) {
						final int kind = selectedItem.getTile().getKind();
						c = doc().getTileClassColor(kind);
					}
					final Color picked = pickColor("Set Tile Color", c);
					if (picked == null) {
						return;
					}
                    suspendRendering();
					doc().recolor(selectedItem, picked);
                    resumeRendering();
				}
			}, "Set the color for this tile.",
			KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
		}
        return ActionRegistry.instance().get(name);
	}
    
    private Action actionUncolorTile() {
    	final String name = "Uncolor Tile";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
	                    suspendRendering();
	                    doc().recolor(selectedItem, null);
	                    resumeRendering();
					}
				}
			}, "Use the tile class color for selected tile.",
			KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionRecolorFacetClass() {
    	final String name = "Recolor Facet Class";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem == null || selectedFace < 0) {
						return;
					}
					Tiling.Facet f = selectedItem.getTile().facet(selectedFace);
					Color c = doc().getFacetClassColor(f);
					if (c == null) {
						c = doc().color(selectedItem);
					}
					if (c == null) {
						final int kind = selectedItem.getTile().getKind();
						c = doc().getTileClassColor(kind);
					}
					final Color picked = pickColor(
							"Set the color for all facets of this kind", c);
					if (picked == null) {
						return;
					}
					
                    recolorFacetClass(f, picked);
                    suspendRendering();
                    updateMaterials();
                    resumeRendering();
				}
			}, "Set the color for all facets of the selected kind.",
			KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK));
		}
        return ActionRegistry.instance().get(name);
	}
    
    private Action actionUncolorFacetClass() {
    	final String name = "Uncolor Facet Class";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem == null || selectedFace < 0) {
						return;
					}
					uncolorFacetClass(selectedItem.getTile().facet(selectedFace));
                    suspendRendering();
                    updateMaterials();
                    resumeRendering();
				}
			}, "Use the tile color for all facets of the selected kind.", null);
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionHideFacetClass() {
    	final String name = "Hide Facet Class";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem == null || selectedFace < 0) {
						return;
					}
					hideFacetClass(selectedItem.getTile().facet(selectedFace));
                    suspendRendering();
                    updateDisplayProperties();
                    resumeRendering();
				}
			}, "Toggle visibility for this facet.", null);
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionShowAllInTile() {
    	final String name = "Show All Facets in Tile";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						showAllInTile(selectedItem.getTile());
	                    suspendRendering();
	                    updateDisplayProperties();
	                    resumeRendering();
					}
				}
			}, "Show all facets in tiles of the selected kind.", null);
		}
		return ActionRegistry.instance().get(name);
	}
    
    private Action actionAddEndNodes() {
		final String name = "Add End Nodes";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						doc().addIncident(selectedItem);
					}
				}
    		}, "Add the nodes at the ends of the selected edge.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionRemoveEdge() {
		final String name = "Remove Edge";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						doc().remove(selectedItem);
					}
				}
    		}, "Remove the selected edge.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionConnectNode() {
		final String name = "Connect Node";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						if (doc().connectToExisting(selectedItem) == 0) {
							doc().addIncident(selectedItem);
						}
					}
				}
    		},
    		"Connect the selected node with all visible neighbors "
    		+ " or add all neighbors.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionRemoveNode() {
		final String name = "Remove Node";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					if (selectedItem != null) {
						doc().remove(selectedItem);
					}
				}
    		}, "Remove the selected node.",
    		KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
    	}
        return ActionRegistry.instance().get(name);
    }
    
    private Action actionEncompass() {
    	final String name = "Fit To Scene";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
                    encompass();
                }
            }, "Adjust camera to fit scene to window",
            KeyStroke.getKeyStroke(KeyEvent.VK_0, 0));
        }
        return ActionRegistry.instance().get(name);
    }

    private Action actionViewAlong() {
    	final String name = "View along...";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				final String input = getInput("View along (x y z):",
    						"3dt Viewing Direction", "");
					final String fields[] = input.trim().split("\\s+");
					try {
						final double x = Double.parseDouble(fields[0]);
						final double y = Double.parseDouble(fields[1]);
						final double z = Double.parseDouble(fields[2]);
						final Vector eye = new Vector(new double[] { x,
								y, z });
						final Vector up;
						if (x == 0 && y == 0) {
							up = new Vector(0, 1, 0);
						} else {
							up = new Vector(0, 0, 1);
						}
						setViewingTransformation(eye, up);
					} catch (final Exception ex) {
						messageBox("Can't view along '" + input.trim() + "'.",
								"3dt Viewing Direction",
								BStandardDialog.INFORMATION);
					}
					encompass();
				}
    		}, "View the scene along an arbitrary dirextion",
    		KeyStroke.getKeyStroke(KeyEvent.VK_V, 0));
    	}
    	return ActionRegistry.instance().get(name);
    }
    
	private Action actionXView() {
    	final String name = "View along X";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(1, 0, 0), new Vector(0, 0, 1));
					encompass();
				}
			}, "View the scene along the X axis or 100 direction.",
			KeyStroke.getKeyStroke(KeyEvent.VK_X, 0));
		}
        return ActionRegistry.instance().get(name);
	}

	private Action actionYView() {
    	final String name = "View along Y";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(0, 1, 0), new Vector(0, 0, 1));
					encompass();
				}
			}, "View the scene along the Y axis or 010 direction.",
			KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0));
		}
		return ActionRegistry.instance().get(name);
	}

	private Action actionZView() {
    	final String name = "View along Z";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(0, 0, 1), new Vector(0, 1, 0));
					encompass();
				}
			}, "View the scene along the Z axis or 001 direction.",
			KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action action011View() {
    	final String name = "View along 011";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(0, 1, 1), new Vector(0, 0, 1));
					encompass();
				}
			}, "View the scene along the 011 direction.",
			KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action action101View() {
    	final String name = "View along 101";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(1, 0, 1), new Vector(0, 0, 1));
					encompass();
				}
			}, "View the scene along the 101 direction.",
			KeyStroke.getKeyStroke(KeyEvent.VK_B, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action action110View() {
    	final String name = "View along 110";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(1, 1, 0), new Vector(0, 0, 1));
					encompass();
				}
			}, "View the scene along the 110 direction.",
			KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action action111View() {
    	final String name = "View along 111";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					setViewingTransformation(new Vector(1, 1, 1), new Vector(0, 0, 1));
					encompass();
				}
			}, "View the scene along the 111 vector.",
			KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateRight() {
    	final String name = "Rotate Right";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 0, 1, 0 }, ui.getRotationStep() * Math.PI / 180.0);
				}
			}, "Rotate the scene to the right",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateLeft() {
    	final String name = "Rotate Left";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 0, 1, 0 },
    						-ui.getRotationStep() * Math.PI / 180.0);
				}
			}, "Rotate the scene to the left",
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateUp() {
    	final String name = "Rotate Up";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 1, 0, 0 },
    						-ui.getRotationStep() * Math.PI / 180.0);
				}
			}, "Rotate the scene upward",
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateDown() {
    	final String name = "Rotate Down";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 1, 0, 0 },
    						ui.getRotationStep() * Math.PI / 180.0);
				}
			}, "Rotate the scene downward",
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateClockwise() {
    	final String name = "Rotate Clockwise";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 0, 0, 1 },
    						-ui.getRotationStep() * Math.PI / 180.0);
				}
			}, "Rotate the scene clockwise",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
							InputEvent.CTRL_DOWN_MASK));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateCounterClockwise() {
    	final String name = "Rotate Counter-Clockwise";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 0, 0, 1 },
    						ui.getRotationStep() * Math.PI / 180.0);
				}
			}, "Rotate the scene counter-clockwise",
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
							InputEvent.CTRL_DOWN_MASK));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private void disableTilingChange() {
		Invoke.andWait(new Runnable() {
			public void run() {
				actionOpen().setEnabled(false);
				actionFirst().setEnabled(false);
				actionNext().setEnabled(false);
				actionPrevious().setEnabled(false);
				actionLast().setEnabled(false);
				actionJump().setEnabled(false);
				actionSearch().setEnabled(false);
			}
		});
	}
    
    private void enableTilingChange() {
		Invoke.andWait(new Runnable() {
			public void run() {
				actionOpen().setEnabled(true);
				actionFirst().setEnabled(true);
				actionNext().setEnabled(true);
				actionPrevious().setEnabled(true);
				actionLast().setEnabled(true);
				actionJump().setEnabled(true);
				actionSearch().setEnabled(true);
			}
		});
	}
    
    private void openFile(final String path) {
    	final String filename = new File(path).getName();
        disableTilingChange();
		busy();
        new Thread(new Runnable() {
            public void run() {
                try {
                	documents = Document.load(path);
                	done();
                	enableTilingChange();
                	Invoke.later(new Runnable() {
                		public void run() {
							log("File " + filename + " opened with "
									+ documents.size() + " tiling"
									+ (documents.size() > 1 ? "s" : "") + ".");
							for (String key : tInfoFields.keySet()) {
								setTInfo(key, "");
							}
							setTInfo("_file", filename);
						}
                	});
                    tilingCounter = 0;
                    if (documents.size() == 1) {
                    	doTiling(1);
                    } else if (documents.size() > 1) {
                    	actionSearch().actionPerformed(null);
                    }
                    return;
                } catch (final FileNotFoundException ex) {
                	log("Could not find file " + filename);
                } catch (final Exception ex) {
        			ex.printStackTrace();
                }
            	done();
            	enableTilingChange();
            }
        }).start();
    }
    
    private void busy() {
    	Invoke.andWait(new Runnable() {
    		public void run() {
    	    	viewerApp.getFrame().setCursor(busyCursor);
    	    	controlsFrame.setCursor(busyCursor);
    		}
    	});
    }
    
    private void done() {
    	Invoke.andWait(new Runnable() {
    		public void run() {
    	    	viewerApp.getFrame().setCursor(normalCursor);
    	    	controlsFrame.setCursor(normalCursor);
    		}
    	});
    }
    
    private void doTiling(final int n) {
        if (documents == null || n < 1 || n > documents.size()) {
            return;
        }
        disableTilingChange();
        busy();
        new Thread(new Runnable() {
            public void run() {
                tilingCounter = n;
                try {
                	processTiling(documents.get(tilingCounter - 1));
                } catch (final Exception ex) {
        			ex.printStackTrace();
                }
                done();
                enableTilingChange();
            }
        }).start();
    }
    
    private Document doc() {
        return this.currentDocument;
    }
    
    private void processTiling(final Document doc) {
		final Stopwatch timer = new Stopwatch();

        for (String key : tInfoFields.keySet()) {
        	if (key != "_file") {
        		setTInfo(key, "");
        	}
        }
		setTInfo("_num", tilingCounter + " of " + documents.size());
		String name = doc.getName();
		if (name == null) {
			name = "--";
		}
		setTInfo("_name", name);
		
		this.currentDocument = doc;
		// -- get the user options saved in the document
		if (doc.getProperties() != null) {
			try {
				Config.pushProperties(doc.getProperties(), this);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// -- set the callback for display list changes
		doc.removeEventLink(DisplayList.Event.class, this);
		doc.addEventLink(DisplayList.Event.class, this,
				"handleDisplayListEvent");
				
		// -- update the info display
		Invoke.later(new Runnable() {
			public void run() {
				final DSymbol ds = doc().getSymbol();
				setTInfo("size", ds.size());
				setTInfo("dim", ds.dim());
				setTInfo("transitivity", doc().getTransitivity());
				setTInfo("minimal", ds.isMinimal());
				setTInfo("selfdual", ds.equals(ds.dual()));
				setTInfo("signature", "pending...");
				setTInfo("group", "pending...");
			}
		});

		// -- render new tiling
        suspendRendering();
		log("Constructing geometry...");
		startTimer(timer);
		try {
			construct();
		} catch (final Exception ex) {
			clearSceneGraph();
			ex.printStackTrace();
		}
		log("  " + getTimer(timer));

		// -- update more of the info display
        final Document oldDoc = doc();
        
		final Thread worker = new Thread(new Runnable() {
			public void run() {
				final String sig = oldDoc.getSignature();
				if (doc() == oldDoc) {
					setTInfo("signature", sig);
				}
				final String group = oldDoc.getGroupName();
				if (doc() == oldDoc) {
					setTInfo("group", group);
				}
			}
		});
		worker.setPriority(Thread.MIN_PRIORITY);
		worker.start();
		
		// -- set camera and viewing transformation as specified in document
		updateCamera();
		if (doc.getTransformation() != null) {
			setViewingTransformation(doc.getTransformation());
		} else {
			setViewingTransformation(new Vector(0,0,1), new Vector(0,1,0));
		}
		resumeRendering();
		encompass();
	}
    
    private SceneGraphComponent makeBody(final Tile b) {
    	// --- make subdivision surfaces for the individual faces
    	final int nFaces = b.size();
    	final int fStart[] = new int[nFaces];
    	final Surface fSurf[] = new Surface[nFaces];
    	final String fTag[] = new String[nFaces + 1];
    	fTag[b.size()] = "outline";
    	final int outlineFixing = getSubdivisionLevel() - getEdgeRoundingLevel();
		int nextStart = 0;
		
		for (int i = 0; i < b.size(); ++i) {
			final Facet face = b.facet(i);
			final int n = face.size();
			fStart[i] = nextStart;

			// --- compute the vertex positions for this face
			final double corners[][] = new double[n][3];
			for (int j = 0; j < n; ++j) {
				corners[j] = doc().cornerPosition(0, face.chamber(j));
			}

			// --- compute the position of the face center
			final double center[] = doc().cornerPosition(2, face.getChamber());

			// --- generate the base surface
			final double p[][] = new double[n][3];
			for (int j = 0; j < n; ++j) {
				final double u[] = corners[(j + n - 1) % n];
				final double v[] = corners[j];
				final double w[] = corners[(j + 1) % n];
				final double vu[] = Rn.subtract(null, u, v);
				final double vw[] = Rn.subtract(null, w, v);
				final double vc[] = Rn.subtract(null, center, v);
				Rn.normalize(vu, vu);
				Rn.normalize(vw, vw);
				final double d[] = Rn.add(null, vu, vw);
				double f = Rn.innerProduct(d, vu);
				final double tmp[] = Rn.times(null, f, vu);
				Rn.subtract(tmp, d, tmp);
				final double length = edgeWidth * Rn.euclideanNorm(d)
						/ Rn.euclideanNorm(tmp);
				Rn.crossProduct(tmp, vu, vw);
				Rn.crossProduct(tmp, d, tmp);
				Rn.normalize(tmp, tmp);
				f = Rn.innerProduct(tmp, vc);
				Rn.times(tmp, f, tmp);
				Rn.subtract(tmp, vc, tmp);
				f = length / Rn.euclideanNorm(tmp);
				Rn.linearCombination(p[j], 1, corners[j], f, tmp);
// final double tmp[] = Rn.subtract(null, corners[j], center);
//				final double f = getEdgeWidth() / Rn.euclideanNorm(tmp);
//				Rn.linearCombination(p[j], 1 - f, corners[j], f, center);
			}
			
            fSurf[i] = Surface.fromOutline(p, 1000);
			fTag[i] = "face:" + face.getIndex();
			fSurf[i].tagAll(fTag[i]);
			nextStart += fSurf[i].vertices.length;
		}
		
		// --- combine all those surfaces into one
		final Surface allFaces = Surface.concatenation(fSurf);
		
		// --- extract the data in order to augment
    	final List<double[]> vertices = new ArrayList<double[]>();
    	vertices.addAll(Arrays.asList(allFaces.vertices));
    	final List<int[]> faces = new ArrayList<int[]>();
    	faces.addAll(Arrays.asList(allFaces.faces));
    	final List<Object> tagsList = new ArrayList<Object>();
    	for (int i = 0; i < allFaces.faces.length; ++i) {
    		tagsList.add(allFaces.getAttribute(Surface.FACE, i, Surface.TAG));
    	}
    	final List<Integer> fixedList = new ArrayList<Integer>();
    	for (int i = 0; i < allFaces.fixed.length; ++i) {
    		fixedList.add(allFaces.fixed[i]);
    	}
    	final List<Boolean> convexList = new ArrayList<Boolean>();
    	for (int i = 0; i < allFaces.vertices.length; ++i) {
			convexList.add(false);
		}
		
    	// --- get some more data from the tiling
        final Tiling til = doc().getTiling();
    	final DSCover cover = til.getCover();
    	final List idcsB = new IndexList(0, 1, 2);
    	final List idcsV = new IndexList(1, 2);
    	final Object D = b.getChamber();
    	
    	// --- generate and map vertices on the body's edges
    	final Map<Object,Integer> ch2edge = new HashMap<Object,Integer>();
    	for (final Iterator elms = cover.orbit(idcsB, D); elms.hasNext();) {
    		final Object E = elms.next();
    		if (til.coverOrientation(E) < 0) {
    			continue;
    		}
    		final double v[] = doc().cornerPosition(0, E);
    		final double w[] = doc().cornerPosition(0, cover.op(0, E));
			final double f = getEdgeWidth()
					/ Rn.euclideanNorm(Rn.subtract(null, v, w));
    		ch2edge.put(E, vertices.size());
    		ch2edge.put(cover.op(2, E), vertices.size());
			vertices.add(Rn.linearCombination(null, 1 - f, v, f, w));
			fixedList.add(outlineFixing);
			convexList.add(true);
    	}

    	// --- generate and map vertices for the body's corners
    	final Map<Object,Integer> ch2vertex = new HashMap<Object,Integer>();
    	for (final Iterator elms = cover.orbit(idcsB, D); elms.hasNext();) {
    		final Object E = elms.next();
    		if (ch2vertex.containsKey(E)) {
    			continue;
    		}
    		double sum[] = new double[3];
    		int n = 0;
    		for (final Iterator orb = cover.orbit(idcsV, E); orb.hasNext();) {
    			final Object C = orb.next();
    			ch2vertex.put(C, vertices.size());
    			Rn.add(sum, sum, vertices.get(ch2edge.get(C)));
    			++n;
    		}
    		if (getEdgeRoundingLevel() > 0) {
    			vertices.add(Rn.linearCombination(null, 0.5 / n, sum, 0.5,
						doc().cornerPosition(0, E)));
    		} else {
    			vertices.add(doc().cornerPosition(0, E));
    		}
    		fixedList.add(outlineFixing);
			convexList.add(true);
    	}
    	
    	// --- map vertices inside the body's faces
    	final Map<Object,Integer> ch2inner = new HashMap<Object,Integer>();
    	for (int k = 0; k < b.size(); ++k) {
    		final Facet face = b.facet(k);
            for (int i = 0; i < face.size(); ++i) {
            	final Object E = face.chamber(i);
            	final int pos = fStart[k] + i;
            	ch2inner.put(E, pos);
    			ch2inner.put(cover.op(1, E), pos);
            }
    	}
    	
    	// --- finally, make the faces
    	for (int k = 0; k < b.size(); ++k) {
    		final Facet face = b.facet(k);
            final int n = face.size();
            
            for (int i = 0; i < n; ++i) {
            	final Object E = face.chamber(i);
            	final Object E0 = cover.op(0, E);
            	final Object E1 = cover.op(1, E);
            	if (getEdgeRoundingLevel() > 0) {
					faces.add(new int[] { ch2edge.get(E), ch2edge.get(E0),
							ch2inner.get(E0), ch2inner.get(E) });
					faces.add(new int[] { ch2edge.get(E), ch2inner.get(E),
							ch2edge.get(E1), ch2vertex.get(E) });
					tagsList.add(fTag[b.size()]);
					tagsList.add(fTag[b.size()]);
				} else {
					faces.add(new int[] { ch2edge.get(E), ch2edge.get(E0),
							ch2inner.get(E0), ch2inner.get(E) });
					faces.add(new int[] { ch2edge.get(E), ch2inner.get(E),
							ch2vertex.get(E) });
					faces.add(new int[] { ch2inner.get(E), ch2edge.get(E1),
							ch2vertex.get(E) });
					tagsList.add(fTag[b.size()]);
					tagsList.add(fTag[b.size()]);
					tagsList.add(fTag[b.size()]);
				}
			}
		}
    	
    	// --- make a subdivision surface and subdivide it
    	final double pos[][] = new double[vertices.size()][];
    	vertices.toArray(pos);
    	final int idcs[][] = new int[faces.size()][];
    	faces.toArray(idcs);
    	final int fixed[] = new int[vertices.size()];
    	for (int i = 0; i < vertices.size(); ++i) {
    		fixed[i] = fixedList.get(i);
    	}
    	Surface surf = new Surface(pos, idcs, fixed);
    	for (int i = 0; i < tagsList.size(); ++i) {
    		surf.setAttribute(Surface.FACE, i, Surface.TAG, tagsList.get(i));
    	}
    	for (int i = 0; i < convexList.size(); ++i) {
    		surf.setAttribute(Surface.VERTEX, i, Surface.CONVEX, convexList
					.get(i));
    	}
		for (int level = 0; level < getSubdivisionLevel(); ++level) {
			surf = surf.subdivision();
		}
		// --- force normals to be generated
		surf.computeNormals();

        // --- make a node for the body
        final SceneGraphComponent sgc = new SceneGraphComponent("template:"
        		+ b.getIndex());
        
		// --- split the subdivided surface back into its parts and add them
		for (int i = 0; i <= b.size(); ++i) {
			final String tag = fTag[i];
			final Surface surfPart = surf.extract(tag);
//			if (tag.startsWith("face:")) {
//				surfPart.pull(getTileRelaxationSteps());
//			}
			
			// --- make a geometry that jReality can use
			final IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
			ifsf.setVertexCount(surfPart.vertices.length);
			ifsf.setFaceCount(surfPart.faces.length);
			ifsf.setVertexCoordinates(surfPart.vertices);
			ifsf.setFaceIndices(surfPart.faces);
			ifsf.setGenerateEdgesFromFaces(true);
			ifsf.setFaceNormals(surfPart.getFaceNormals());
			if (getSmoothFaces()) {
				ifsf.setVertexNormals(surfPart.getVertexNormals());
			}
			ifsf.update();
			final SceneGraphComponent part = new SceneGraphComponent(tag);
			part.setGeometry(ifsf.getIndexedFaceSet());
			sgc.addChild(part);
		}
        
        return sgc;
	}
    
    private void addTile(final DisplayList.Item item) {
    	if (item == null) {
    		return;
    	}
    	
    	final int kind = item.getTile().getIndex();
    	final Vector shift = item.getShift();
        final SceneGraphComponent template = this.templates[kind];
        final SceneGraphComponent sgc = new SceneGraphComponent("body");
        MatrixBuilder.euclidean().translate(
                ((Vector) shift.times(doc().getEmbedderToWorld()))
                        .getCoordinates().asDoubleArray()[0]).assignTo(sgc);
        sgc.addChild(template);
        sgc.setAppearance(this.materials[kind]);
        this.tiling.addChild(sgc);
        this.node2item.put(sgc, item);
        this.item2node.put(item, sgc);
    }
    
    private void addEdge(final DisplayList.Item item) {
    	if (item == null) {
    		return;
    	}
    	
    	final Color c = getNetEdgeColor();
    	final double r = getNetEdgeRadius();
    	final IEdge e = item.getEdge();
    	final Vector s =
    		(Vector) item.getShift().times(doc().getEmbedderToWorld());
    	final double p[] = ((Point) doc().edgeSourcePoint(e).plus(s))
				.getCoordinates().asDoubleArray()[0];
    	final double q[] = ((Point) doc().edgeTargetPoint(e).plus(s))
				.getCoordinates().asDoubleArray()[0];
        final SceneGraphComponent sgc = TubeUtility.tubeOneEdge(p, q, r,
				TubeUtility.octagonalCrossSection, Pn.EUCLIDEAN);
        final Appearance a = new Appearance();
        setColor(a, c);
        a.setAttribute(CommonAttributes.TRANSPARENCY, 0.0);
        sgc.setAppearance(a);
        this.net.addChild(sgc);
        this.node2item.put(sgc, item);
        this.item2node.put(item, sgc);
    }
    
    private void addNode(final DisplayList.Item item) {
    	if (item == null) {
    		return;
    	}
    	
    	final Color c = getNetNodeColor();
    	final double r = getNetNodeRadius();
    	final INode node = item.getNode();
    	final Vector s =
    		(Vector) item.getShift().times(doc().getEmbedderToWorld());
    	final double p[] = ((Point) doc().nodePoint(node).plus(s))
				.getCoordinates().asDoubleArray()[0];
		SceneGraphComponent sgc = SceneGraphUtility
				.createFullSceneGraphComponent("node");
		MatrixBuilder.init(null, Pn.EUCLIDEAN).translate(p).scale(r).assignTo(
				sgc.getTransformation());
		sgc.addChild(sphereTemplate);
        final Appearance a = new Appearance();
        setColor(a, c);
        a.setAttribute(CommonAttributes.TRANSPARENCY, 0.0);
        sgc.setAppearance(a);
        this.net.addChild(sgc);
        this.node2item.put(sgc, item);
        this.item2node.put(item, sgc);
    }
    
    private void recolorTile(final DisplayList.Item item, final Color color) {
    	final Appearance a;
		if (color != null) {
			a = new Appearance();
			setColor(a, color);
		} else {
			a = this.materials[item.getTile().getIndex()];
		}
		item2node.get(item).setAppearance(a);
    }
    
    private void clearSceneGraph() {
		item2node.clear();
		node2item.clear();
        SceneGraphUtility.removeChildren(tiling);
        SceneGraphUtility.removeChildren(net);
    }
    
	public void handleDisplayListEvent(final Object event) {
		final DisplayList.Event e = (DisplayList.Event) event;
		final DisplayList.Item item = e.getInstance();
		
		if (e.getEventType() == DisplayList.BEGIN) {
		} else if (e.getEventType() == DisplayList.END) {
		} else if (e.getEventType() == DisplayList.ADD) {
			if (item.isTile()) {
				addTile(item);
			} else if (item.isEdge()) {
				addEdge(item);
			} else if (item.isNode()) {
				addNode(item);
			}
		} else if (e.getEventType() == DisplayList.DELETE) {
			final SceneGraphNode node = item2node.get(item);
			item2node.remove(item);
			node2item.remove(node);
			if (item.isTile()) {
				SceneGraphUtility.removeChildNode(tiling, node);
			} else if (item.isEdge() || item.isNode()) {
				SceneGraphUtility.removeChildNode(net, node);
			}
		} else if (e.getEventType() == DisplayList.RECOLOR) {
			if (item.isTile()) {
				recolorTile(item, e.getNewColor());
			}
		}
	}
	
    @SuppressWarnings("unchecked")
    private void construct() {
        final Stopwatch timer = new Stopwatch();

        log("    Making tiling...");
        startTimer(timer);
        doc().getTiling();
        log("      " + getTimer(timer));
        
        log("    Initializing embedder...");
        startTimer(timer);
        doc().initializeEmbedder();
        log("      " + getTimer(timer));
        
        embed();
        makeTiles();
        makeMaterials();
        updateDisplayProperties();
        updateMaterials();
		if (doc().size() == 0) {
			clearSceneGraph();
			makeCopies();
		} else {
			refreshScene();
		}
    }

    private void reembed() {
        if (doc() == null) {
            return;
        }
        doc().invalidateEmbedding();
        embed();
        makeTiles();
        updateDisplayProperties();
        updateMaterials();
        suspendRendering();
        refreshScene();
        resumeRendering();
    }
    
    private void embed() {
        doc().setEmbedderStepLimit(getEmbedderStepLimit());
        doc().setEqualEdgePriority(getEqualEdgePriority());
        doc().setUseBarycentricPositions(getUseBarycentricPositions());

        final Stopwatch timer = new Stopwatch();
        
        log("    Embedding...");
        startTimer(timer);
        doc().getEmbedderToWorld();
        log("      " + getTimer(timer));
    }
    
    private void makeUnitCell() {
    	final Stopwatch timer = new Stopwatch();
    	
    	log("    Determining unit cell");
    	startTimer(timer);
    	final double origin[] = doc().getOrigin();
    	final double cellVecs[][] = doc().getUnitCellVectors();
    	log("      " + getTimer(timer));
    	final double corners[][] = new double[8][];
    	corners[0] = origin;
    	corners[1] = Rn.add(null, corners[0], cellVecs[0]);
    	for (int i = 0; i < 2; ++i) {
    		corners[i + 2] = Rn.add(null, corners[i], cellVecs[1]);
    	}
    	for (int i = 0; i < 4; ++i) {
    		corners[i + 4] = Rn.add(null, corners[i], cellVecs[2]);
    	}
    	final int[][] idcs = new int[][] {
    			{ 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 },
    			{ 0, 2 }, { 1, 3 }, { 4, 6 }, { 5, 7 },
    			{ 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 },
    	};
    	
        final IndexedLineSetFactory ilsf = new IndexedLineSetFactory();
        ilsf.setVertexCount(corners.length);
        ilsf.setLineCount(idcs.length);
        ilsf.setVertexCoordinates(corners);
        ilsf.setEdgeIndices(idcs);
		ilsf.update();
		final IndexedLineSet ils = ilsf.getIndexedLineSet();
		
		final BallAndStickFactory basf = new BallAndStickFactory(ils);
        final double r = getUnitCellEdgeWidth() / 2;
        final Color c = getUnitCellColor();
		basf.setBallRadius(r);
		basf.setStickRadius(r);
		basf.update();
        
		final SceneGraphComponent bas = basf.getSceneGraphComponent();
		bas.setName("UnitCell");
		final Appearance a = new Appearance();
		setColor(a, c);
		bas.setAppearance(a);
		for (final SceneGraphComponent child: bas.getChildComponents()) {
			child.setAppearance(null);
		}
        this.unitCell = basf.getSceneGraphComponent();
    }
    
    private void makeTiles() {
    	if (doc() == null) {
    		return;
    	}
        final Stopwatch timer = new Stopwatch();

        log("    Making tile templates...");
        startTimer(timer);
        this.templates = new SceneGraphComponent[doc().getTiles().size()];
        for (final Tile b: doc().getTiles()) {
            this.templates[b.getIndex()] = makeBody(b);
        }
        log("      " + getTimer(timer));
    }
    
    private void refreshScene() {
    	if (doc() == null) {
    		return;
    	}
        SceneGraphUtility.removeChildren(this.tiling);
        SceneGraphUtility.removeChildren(this.net);
        for (final DisplayList.Item item: doc()) {
        	if (item.isTile()) {
        		addTile(item);
        		if (doc().color(item) != null) {
        			recolorTile(item, doc().color(item));
        		}
        	} else if (item.isEdge()) {
        		addEdge(item);
        	} else if (item.isNode()) {
        		addNode(item);
        	}
        }
        this.net.setVisible(getShowNet());
    }

    private void makeMaterials() {
    	if (doc() == null) {
    		return;
    	}

    	this.materials = new Appearance[doc().getTiles().size()];
		for (int i = 0; i < this.templates.length; ++i) {
			this.materials[i] = new Appearance();
			setColor(this.materials[i], doc().getDefaultTileColor(i));
		}
	}
    
    private void setColor(final Appearance a, final Color c) {
    	a.setAttribute(CommonAttributes.DIFFUSE_COLOR, c);
    }
    
    private void updateAppearance(final Appearance a) {
        a.setAttribute(CommonAttributes.EDGE_DRAW, false);
        a.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		a.setAttribute(CommonAttributes.AMBIENT_COEFFICIENT,
				getAmbientCoefficient());
		a.setAttribute(CommonAttributes.DIFFUSE_COEFFICIENT,
				getDiffuseCoefficient());
		a.setAttribute(CommonAttributes.SPECULAR_COEFFICIENT,
				getSpecularCoefficient());
		a.setAttribute(CommonAttributes.AMBIENT_COLOR, getAmbientColor());
		a.setAttribute(CommonAttributes.SPECULAR_COLOR, getSpecularColor());
		a.setAttribute(CommonAttributes.SPECULAR_EXPONENT,
				getSpecularExponent());
		a.setAttribute(CommonAttributes.TRANSPARENCY, getFaceTransparency());
    }
    
    private Color blendColors(final Color c1, final Color c2, final double f) {
    	return new Color(
    			(int) (c1.getRed()   * (1-f) + c2.getRed()   * f + 0.5),
    			(int) (c1.getGreen() * (1-f) + c2.getGreen() * f + 0.5),
    			(int) (c1.getBlue()  * (1-f) + c2.getBlue()  * f + 0.5)
    			);
	}
    
    private void updateMaterials() {
    	if (this.templates == null) {
    		return;
    	}
        final Stopwatch timer = new Stopwatch();
        log("    Updating materials...");
        startTimer(timer);
        
        updateAppearance(this.world.getAppearance());
        this.viewerApp.getCurrentViewer().getSceneRoot().getAppearance()
				.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED,
						getFaceTransparency() > 0);
        
        for (final Tile b: doc().getTiles()) {
        	final int i = b.getIndex();
        	final Color tileColor = doc().getDefaultTileColor(i);
            setColor(this.materials[i], tileColor);
        	final SceneGraphComponent sgc = this.templates[b.getIndex()];
			for (Object node : sgc.getChildNodes()) {
				if (!(node instanceof SceneGraphComponent)) {
					continue;
				}
				final SceneGraphComponent child = (SceneGraphComponent) node;
				final String name = child.getName();
				if (name.startsWith("face:")) {
					final int j = Integer.parseInt(name.substring(5));
					final Tiling.Facet f = b.facet(j);
					final Color c = doc().getFacetClassColor(f);
					if (c == null) {
						child.setAppearance(null);
					} else {
						final Appearance a = new Appearance();
						setColor(a, c);
						child.setAppearance(a);
					}
				} else if (name.startsWith("outline")) {
					final Appearance a = new Appearance();
					final double f = getEdgeOpacity();
					if (f > 0) {
						setColor(a, blendColors(tileColor, getEdgeColor(), f));
					}
					a.setAttribute(CommonAttributes.TRANSPARENCY, 0.0);
					child.setAppearance(a);
				}
			}
    	}
        log("      " + getTimer(timer));
    }
    
    private void updateDisplayProperties() {
    	if (this.templates == null) {
    		return;
    	}
        final Stopwatch timer = new Stopwatch();
        log("    Updating display properties...");
        startTimer(timer);
        
        for (final Tile b : doc().getTiles()) {
			final int i = b.getIndex();
			final SceneGraphComponent sgc = this.templates[b.getIndex()];
			for (Iterator iter = sgc.getChildNodes().iterator(); iter.hasNext();) {
				final Object node = iter.next();
				if (node instanceof SceneGraphComponent) {
					final SceneGraphComponent child = (SceneGraphComponent) node;
					final String name = child.getName();
					if (name.startsWith("outline")) {
						child.setVisible(getDrawEdges());
					} else if (name.startsWith("face:")) {
						final int j = Integer.parseInt(name.substring(5));
						final Tiling.Facet f = b.facet(j);
						child.setVisible(getDrawFaces()
								&& !doc().isHiddenFacetClass(f));
					}
				}
			}
            final double[] center = doc().cornerPosition(3,
					doc().getTile(i).getChamber());
			final double[] cneg = new double[3];
			for (int j = 0; j < 3; ++j) {
				cneg[j] = -center[j];
			}
            MatrixBuilder.euclidean().translate(center).scale(getTileSize())
    				.translate(cneg).assignTo(sgc);
		}
    	
		makeUnitCell();
    	this.unitCell.setVisible(getShowUnitCell());
    	this.net.setVisible(getShowNet());
        log("      " + getTimer(timer));
    }
    
    private void makeCopies() {
		final Stopwatch timer = new Stopwatch();
		log("    Generating scene...");
		startTimer(timer);

		final List<Vector> vecs = replicationVectors();
		for (final Tile b : doc().getTiles()) {
			for (final Vector s : doc().centerIntoUnitCell(b)) {
				for (Vector v : vecs) {
					final Vector shift = (Vector) s.plus(v);
					doc().add(b, shift);
				}
			}
		}
		log("      " + getTimer(timer));
	}

    private List<Vector> replicationVectors() {
    	final Vector xyz[] = doc().getUnitCellVectorsInEmbedderCoordinates();
    	final Vector vx = xyz[0];
    	final Vector vy = xyz[1];
    	final Vector vz = xyz[2];
    	final int loX = getMinX();
    	final int hiX = Math.max(minX, getMaxX()) + 1;
    	final int loY = getMinY();
    	final int hiY = Math.max(minY, getMaxY()) + 1;
    	final int loZ = getMinZ();
    	final int hiZ = Math.max(minZ, getMaxZ()) + 1;
    	
    	final List<Vector> result = new ArrayList<Vector>();
    	for (int x = loX; x < hiX; ++x) {
    		for (int y = loY; y < hiY; ++y) {
    			for (int z = loZ; z < hiZ; ++z) {
    				result.add((Vector) (vx.times(x)).plus(vy.times(y)).plus(
							vz.times(z)));
    			}
    		}
    	}
    	return result;
    }
    
    private void makeFacetOutline(final Tiling.Facet f, final Vector s) {
		for (int i = 0; i < f.size(); ++i) {
			doc().add(f.edge(i), (Vector) f.edgeShift(i).plus(s));
		}
    }
    
    private void makeTileOutline(final Tiling.Tile t, final Vector s) {
		for (int k = 0; k < t.size(); ++k) {
			final Tiling.Facet f = t.facet(k);
			for (int i = 0; i < f.size(); ++i) {
				doc().add(f.edge(i), (Vector) f.edgeShift(i).plus(s));
			}
		}
    }
    
    private void encompass() {
    	Invoke.later(new Runnable() {
    		public void run() {
    	    	encompass(viewerApp.getCurrentViewer(), scene);
    		}
    	});
    }
    
    double lastCenter[] = null;
    
	public void encompass(final Viewer viewer, final JrScene scene) {
		// --- extract parameters from scene and viewer
		final SceneGraphPath avatarPath = scene.getPath("avatarPath");
		final SceneGraphPath scenePath = scene.getPath("emptyPickPath");
		final SceneGraphPath cameraPath = scene.getPath("cameraPath");
		final double aspectRatio = CameraUtility.getAspectRatio(viewer);
        final int signature = viewer.getSignature();
		
        // --- compute scene-to-avatar transformation
		final Matrix toAvatar = new Matrix();
		scenePath.getMatrix(toAvatar.getArray(), 0, scenePath.getLength() - 2);
		toAvatar.multiplyOnRight(avatarPath.getInverseMatrix(null));
		
		// --- compute bounding box of scene
		final Rectangle3D bounds = GeometryUtility.calculateBoundingBox(
				toAvatar.getArray(), scenePath.getLastComponent());
		if (bounds.isEmpty()) {
			return;
		}
		
		// --- compute best camera position based on bounding box and viewport
        final Camera camera = (Camera) cameraPath.getLastElement();
		final Rectangle2D vp = CameraUtility.getViewport(camera, aspectRatio);
		final double[] e = bounds.getExtent();
		final double radius = Math
				.sqrt(e[0] * e[0] + e[2] * e[2] + e[1] * e[1]) / 2.0;
		final double front = e[2] / 2;

		final double xscale = e[0] / vp.getWidth();
		final double yscale = e[1] / vp.getHeight();
		double camdist = Math.max(xscale, yscale) * 1.1;
		if (!camera.isPerspective()) {
			camdist *= camera.getFocus(); // adjust for viewport scaling
			camera.setFocus(camdist);
		}

		// --- compute new camera position and adjust near/far clipping planes
		final double[] c = bounds.getCenter();
		c[2] += front + camdist;
		camera.setFar(camdist + front + 5 * radius);
		camera.setNear(0.1 * camdist);
		
		// --- make rotateScene() recompute the center
		lastCenter = null;
		
		// --- adjust the avatar position to make scene fit
		final Matrix camMatrix = new Matrix();
		cameraPath.getInverseMatrix(camMatrix.getArray(), avatarPath
				.getLength());
		final SceneGraphComponent avatar = avatarPath.getLastComponent();
		final Matrix m = new Matrix(avatar.getTransformation());
		MatrixBuilder.init(m, signature).translate(c).translate(
				camMatrix.getColumn(3)).assignTo(avatar);
	}

	public void setViewingTransformation(final Vector eye, final Vector up) {
		final SceneGraphComponent root = this.scene.getPath("emptyPickPath")
				.getLastComponent();
		if (doc() == null) {
			return;
		}
		final CoordinateChange c = doc().getCellToWorld();
		final Operator op = Operator.viewingRotation((Vector) eye.times(c),
				(Vector) up.times(c));
		final double[][] a = op.getCoordinates().transposed().asDoubleArray();
		final double[] b = new double[] { a[0][0], a[0][1], a[0][2], a[0][3],
				a[1][0], a[1][1], a[1][2], a[1][3], a[2][0], a[2][1], a[2][2],
				a[2][3], a[3][0], a[3][1], a[3][2], a[3][3] };
		MatrixBuilder.euclidean(new Matrix(b)).assignTo(root);
	}
	
	public void rotateScene(final double axis[], final double angle) {
		final SceneGraphPath scenePath = scene.getPath("emptyPickPath");
		final SceneGraphComponent sceneRoot = scenePath.getLastComponent();

		if (lastCenter == null) {
			// --- compute the center of the scene in world coordinates
			final Rectangle3D bounds = GeometryUtility
					.calculateBoundingBox(sceneRoot);
			if (bounds.isEmpty()) {
				return;
			} else {
				lastCenter = new Matrix(sceneRoot.getTransformation())
						.getInverse().multiplyVector(bounds.getCenter());
			}
		}
		
		// --- rotate around the last computed scene center
		final Matrix tOld = new Matrix(sceneRoot.getTransformation());
		final Matrix tNew = MatrixBuilder.euclidean().rotate(angle, axis)
				.times(tOld).getMatrix();
		final double p[] = tOld.multiplyVector(lastCenter);
		final double q[] = tNew.multiplyVector(lastCenter);
		MatrixBuilder.euclidean().translateFromTo(q, p).times(tNew).assignTo(
				sceneRoot);
	}
	
    private void updateCamera() {
    	final Camera cam = CameraUtility.getCamera(this.viewerApp.getCurrentViewer());
    	boolean re_encompass = false;
    	if (getFieldOfView() != cam.getFieldOfView()) {
        	cam.setFieldOfView(getFieldOfView());
            re_encompass = true;
    	}
    	if (re_encompass) {
    		encompass();
    	}
        final Appearance a =
        	this.viewerApp.getCurrentViewer().getSceneRoot().getAppearance();
        a.setAttribute(CommonAttributes.BACKGROUND_COLORS, Appearance.INHERITED);
        a.setAttribute(CommonAttributes.BACKGROUND_COLOR, getBackgroundColor());
        a.setAttribute(CommonAttributes.FOG_ENABLED, getUseFog());
        a.setAttribute(CommonAttributes.FOG_DENSITY, getFogDensity());
        if (getFogToBackground()) {
            a.setAttribute(CommonAttributes.FOG_COLOR, getBackgroundColor());
        } else {
            a.setAttribute(CommonAttributes.FOG_COLOR, getFogColor());
        }
    }
    
    private void suspendRendering() {
    	Invoke.andWait(new Runnable() {
			public void run() {
		    	SceneGraphUtility.removeChildren(world);
			}
    	});
    }
    
    private void resumeRendering() {
		Invoke.andWait(new Runnable() {
			public void run() {
				world.addChild(tiling);
				world.addChild(net);
				world.addChild(unitCell);
				viewerApp.getCurrentViewer().render();
			}
		});
	}

    private Transformation getViewingTransformation() {
		return this.scene.getPath("emptyPickPath").getLastComponent()
				.getTransformation();
    }
    
    private void setViewingTransformation(final Transformation trans) {
    	this.scene.getPath("emptyPickPath").getLastComponent()
				.setTransformation(trans);
    }
    
    private JPopupMenu _selectionPopupForTiles = null;
    
    private JPopupMenu selectionPopupForTiles() {
    	if (_selectionPopupForTiles == null) {
    		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    		_selectionPopupForTiles = new JPopupMenu("Tile Actions");
    		_selectionPopupForTiles.setLightWeightPopupEnabled(false);
    		_selectionPopupForTiles.add(actionAddTile());
    		_selectionPopupForTiles.add(actionRemoveTile());
    		_selectionPopupForTiles.add(actionRemoveTileClass());
    		_selectionPopupForTiles.addSeparator();
    		_selectionPopupForTiles.add(actionHideFacetClass());
    		_selectionPopupForTiles.add(actionShowAllInTile());
    		_selectionPopupForTiles.addSeparator();
    		_selectionPopupForTiles.add(actionAddFacetOutline());
    		_selectionPopupForTiles.add(actionAddTileOutline());
    		_selectionPopupForTiles.addSeparator();
    		_selectionPopupForTiles.add(actionRecolorTile());
    		_selectionPopupForTiles.add(actionUncolorTile());
    		_selectionPopupForTiles.add(actionRecolorTileClass());
    		_selectionPopupForTiles.add(actionRecolorFacetClass());
    		_selectionPopupForTiles.add(actionUncolorFacetClass());
    		
    		_selectionPopupForTiles.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					if (ui.getUseLeopardWorkaround()) {
						restoreViewer();
					}
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (ui.getUseLeopardWorkaround()) {
						forceSoftwareViewer();
					}
				}
    		});
    	}
    	return _selectionPopupForTiles;
    }
    
    private JPopupMenu _selectionPopupForNodes = null;
    
    private JPopupMenu selectionPopupForNodes() {
    	if (_selectionPopupForNodes == null) {
    		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    		_selectionPopupForNodes = new JPopupMenu("Node Actions");
    		_selectionPopupForNodes.setLightWeightPopupEnabled(false);
    		_selectionPopupForNodes.add(actionConnectNode());
    		_selectionPopupForNodes.add(actionRemoveNode());
    		
    		_selectionPopupForNodes.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					if (ui.getUseLeopardWorkaround()) {
						restoreViewer();
					}
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (ui.getUseLeopardWorkaround()) {
						forceSoftwareViewer();
					}
				}
    		});
    	}
    	return _selectionPopupForNodes;
    }
    
    private JPopupMenu _selectionPopupForEdges = null;
    
    private JPopupMenu selectionPopupForEdges() {
    	if (_selectionPopupForEdges == null) {
    		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    		_selectionPopupForEdges = new JPopupMenu("Edge Actions");
    		_selectionPopupForEdges.setLightWeightPopupEnabled(false);
    		_selectionPopupForEdges.add(actionAddEndNodes());
    		_selectionPopupForEdges.add(actionRemoveEdge());
    		
    		_selectionPopupForEdges.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					if (ui.getUseLeopardWorkaround()) {
						restoreViewer();
					}
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (ui.getUseLeopardWorkaround()) {
						forceSoftwareViewer();
					}
				}
    		});
    	}
    	return _selectionPopupForEdges;
    }
    
    public class SelectionTool extends AbstractTool {
        final InputSlot activationSlot = InputSlot.getDevice("PrimarySelection");
        final InputSlot addSlot = InputSlot.getDevice("SecondarySelection");
        final InputSlot removeSlot = InputSlot.getDevice("Meta");

        public SelectionTool() {
            addCurrentSlot(activationSlot);
            addCurrentSlot(removeSlot);
            addCurrentSlot(addSlot);
        }

        public void perform(ToolContext tc) {
            if (tc.getAxisState(activationSlot).isReleased()) {
                return;
            }
            final PickResult pr = tc.getCurrentPick();

            if (pr == null) {
                final ViewerSwitch sw = (ViewerSwitch) tc.getViewer();
                final Viewer v = sw.getCurrentViewer();
                if (v instanceof de.jreality.jogl.Viewer) {
                	final JOGLRenderer renderer = ((de.jreality.jogl.Viewer) v)
							.getRenderer();
					log("polygon count is " + renderer.getPolygonCount());
				}
            } else {
                final SceneGraphPath selection = pr.getPickPath();
                selectedFace = -1;
				for (Iterator path = selection.iterator(); path.hasNext();) {
                    final SceneGraphNode node = (SceneGraphNode) path.next();
                    final String name = node.getName();
                    final DisplayList.Item item = node2item.get(node);
                    if (item != null) {
                        selectedItem = item;
                    } else if (name.startsWith("face:")) {
                        selectedFace = Integer.parseInt(name.substring(5));
                    }
                }
                try {
					if (tc.getAxisState(addSlot).isPressed()) {
						if (selectedItem.isTile()) {
							actionAddTile().actionPerformed(null);
						} else if (selectedItem.isEdge()) {
							actionAddEndNodes().actionPerformed(null);
						} else if (selectedItem.isNode()) {
							actionConnectNode().actionPerformed(null);
						}
					} else if (tc.getAxisState(removeSlot).isPressed()) {
						if (selectedItem.isTile()) {
							actionRemoveTile().actionPerformed(null);
						} else if (selectedItem.isEdge()) {
							actionRemoveEdge().actionPerformed(null);
						} else if (selectedItem.isNode()) {
							actionRemoveNode().actionPerformed(null);
						}
					} else  {
						final java.awt.Component comp = viewerApp.getContent();
						final java.awt.Point pos = comp.getMousePosition();
						if (selectedItem.isTile()) {
							Invoke.andWait(new Runnable() {
								public void run() {
									selectionPopupForTiles().show(comp, pos.x,
											pos.y);
								}
							});
						} else if (selectedItem.isNode()) {
							Invoke.andWait(new Runnable() {
								public void run() {
									selectionPopupForNodes().show(comp, pos.x,
											pos.y);
								}
							});
						} else if (selectedItem.isEdge()) {
							Invoke.andWait(new Runnable() {
								public void run() {
									selectionPopupForEdges().show(comp, pos.x,
											pos.y);
								}
							});
						} else {
							log("Selected: " + selectedItem);
						}
					}
				} catch (final Exception ex) {
				}
            }
        }
    }
    
    private void checkOpenGL() {
		final ViewerSwitch vSwitch = viewerApp.getViewerSwitch();
		final Viewer viewers[] = vSwitch.getViewers();
		int softViewer = -1;
		int joglViewer = -1;
		for (int i = 0; i < viewers.length; ++i) {
			if (viewers[i] instanceof SoftViewer) {
				softViewer = i;
			} else if (viewers[i] instanceof de.jreality.jogl.Viewer) {
				joglViewer = i;
			}
		}
		if (softViewer >= 0 && joglViewer >= 0) {
			try {
				vSwitch.selectViewer(joglViewer);
				vSwitch.render();
			} catch (GLException ex) {
				vSwitch.selectViewer(softViewer);
				vSwitch.render();
			}
		}
    }
    
    private void forceSoftwareViewer() {
		final ViewerSwitch vSwitch = viewerApp.getViewerSwitch();
		final Viewer viewers[] = vSwitch.getViewers();
		final Viewer current = vSwitch.getCurrentViewer();
		for (int i = 0; i < viewers.length; ++i) {
			if (viewers[i] == current) {
				this.previousViewer = i;
				break;
			}
		}
		for (int i = 0; i < viewers.length; ++i) {
			if (viewers[i] instanceof SoftViewer) {
				vSwitch.selectViewer(i);
				vSwitch.render();
				break;
			}
		}
	}
    
    @SuppressWarnings("unused")
	private void restoreViewer() {
    	if (this.previousViewer >= 0) {
			final ViewerSwitch vSwitch = viewerApp.getViewerSwitch();
			vSwitch.selectViewer(this.previousViewer);
			vSwitch.render();
			this.previousViewer = -1;
    	}
    }
    
    private void updateViewerSize() {
    	final java.awt.Component viewer = viewerApp.getViewingComponent();
    	// -- make sure to trigger a property change event
    	viewer.setPreferredSize(null);
    	// -- now set to the desired size
    	viewer.setPreferredSize(
    			new Dimension(ui.getViewerWidth(), ui.getViewerHeight()));
    	// -- update the parent frame
    	viewerApp.getFrame().pack();
    	// -- not completely sure what this does
    	viewer.requestFocusInWindow();
    }
    
    private BButton makeButton(final String label, final Object target,
            final String method) {
    	final BButton button = new BButton(label);
    	button.setBackground(buttonColor);
        button.addEventLink(CommandEvent.class, target, method);
    	return button;
    }
    
    private void saveOptions() {
    	// --- pick up all property values for this instance
    	final Properties ourProps = new Properties();
		try {
			Config.pullProperties(ourProps, this);
			if (doc() != null) {
				doc().setProperties(ourProps);
			}
			Config.pullProperties(ourProps, ui);
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		// --- write them to the configuration file
    	try {
			ourProps.store(new FileOutputStream(configFileName), "3dt options");
		} catch (final FileNotFoundException ex) {
			log("Could not find configuration file " + configFileName);
		} catch (final IOException ex) {
			log("Exception occurred while writing configuration file");
		}
    }

    private void loadOptions() {
    	// --- read the configuration file
    	final Properties ourProps = new Properties();
    	try {
			ourProps.load(new FileInputStream(configFileName));
		} catch (FileNotFoundException e1) {
			log("Could not find configuration file " + configFileName);
			return;
		} catch (IOException e1) {
			log("Exception occurred while reading configuration file");
			return;
		}
		
		// --- override by system properties if defined
		for (final Iterator keys = ourProps.keySet().iterator(); keys.hasNext();) {
			final String key = (String) keys.next();
			final String val = System.getProperty(key);
			if (val != null) {
				ourProps.setProperty(key, val);
			}
		}
    	
		// --- set the properties for this instance
    	try {
    		Config.pushProperties(ourProps, this);
    		Config.pushProperties(ourProps, ui);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    
    private ColumnContainer emptyOptionsContainer() {
		final ColumnContainer options = new ColumnContainer();
		options.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST,
				LayoutInfo.HORIZONTAL, new Insets(2, 5, 2, 5), null));
		options.setBackground(null);
		return options;
	}
    
    private Widget separator() {
    	return new BSeparator();
    }
    
    private Widget label(final String txt, final BorderContainer.Position pos) {
		final BorderContainer result = new BorderContainer();
		result.setBackground(null);
		result.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST,
				LayoutInfo.NONE, new Insets(2, 10, 2, 10), null));
		result.add(new BLabel(txt), pos);
		return result;
	}
    
    private Widget optionsDialog(final Widget options, final Widget buttons) {
        final BorderContainer dialog = new BorderContainer();
        dialog.setBackground(textColor);
        dialog.add(options, BorderContainer.NORTH, new LayoutInfo(
                LayoutInfo.NORTH, LayoutInfo.HORIZONTAL, defaultInsets, null));
        dialog.add(buttons, BorderContainer.SOUTH, new LayoutInfo(
                LayoutInfo.SOUTH, LayoutInfo.NONE, defaultInsets, null));
        return dialog;
    }
    
    private OptionSliderBox slider(final String label, final String option,
			final int min, final int max, final int major, final int minor,
			final boolean snap, final boolean ticks, final boolean labels)
			throws Exception {
		final OptionSliderBox slider = new OptionSliderBox(label, this, option,
				min, max, major, minor, snap);
		slider.setShowTicks(ticks);
		slider.setShowLabels(labels);
		return slider;
	}
    
    private Widget optionsTiles() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
        	options.add(slider("Surface Detail", "subdivisionLevel", 0, 4, 1,
					1, true, true, false));
        	options.add(label("(reduce in case of memory problems)",
					BorderContainer.EAST));
			options.add(separator());
			options.add(new OptionInputBox("Edge Width", this, "edgeWidth"));
			options.add(slider("Edge Creasing", "edgeRoundingLevel", 0, 4, 1,
					1, true, true, false));
			options.add(new OptionColorBox("Edge Color", this, "edgeColor"));
			final OptionSliderBox slider = slider("Color Blending",
					"edgeOpacity", 0, 100, 20, 5, false, true, false);
			slider.setFactor(0.01);
			options.add(slider);
            options.add(separator());
            options.add(new OptionCheckBox("Smooth Shading", this,
					"smoothFaces"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
			@SuppressWarnings("unused")
			public void call() {
				new Thread(new Runnable() {
					public void run() {
						makeTiles();
						updateDisplayProperties();
						updateMaterials();
						suspendRendering();
						refreshScene();
						resumeRendering();
						saveOptions();
					}
				}).start();
			}
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget sceneSlider(final String label, final String option,
    		final boolean ticks)
			throws Exception {
		final OptionSliderBox slider = new OptionSliderBox(label, this, option,
				-2, 2, 1, 1, true);
		slider.setShowTicks(ticks);
		slider.setShowLabels(ticks);
		return slider;
	}

    private Widget optionsScene() {
    	final ColumnContainer options = emptyOptionsContainer();
    	try {
    		options.add(sceneSlider("", "minX", false));
    		options.add(sceneSlider("x Range", "maxX", true));
            options.add(separator());
    		options.add(sceneSlider("", "minY", false));
    		options.add(sceneSlider("y Range", "maxY", true));
            options.add(separator());
    		options.add(sceneSlider("", "minZ", false));
    		options.add(sceneSlider("z Range", "maxZ", true));
            options.add(separator());
    		options.add(new OptionCheckBox("Clear Existing", this,
					"clearOnFill"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
    	}
        
        final Object apply = new Object() {
			@SuppressWarnings("unused")
			public void call() {
				new Thread(new Runnable() {
					public void run() {
						if (doc() != null) {
							suspendRendering();
							if (getClearOnFill()) {
								doc().removeAllTiles();
							}
							makeCopies();
							resumeRendering();
						}
						saveOptions();
					}
				}).start();
			}
		};
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget optionsDisplay() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
        	final OptionSliderBox slider = new OptionSliderBox("Tile Size in %",
					this, "tileSize", 0, 100, 20, 5, false);
        	slider.setFactor(0.01);
        	//slider.setShowLabels(false);
        	options.add(slider);
            options.add(new OptionCheckBox("Draw Faces", this, "drawFaces"));
            options.add(new OptionCheckBox("Draw Edges", this, "drawEdges"));
            options.add(separator());
            options.add(new OptionCheckBox("Show Unit Cell", this,
					"showUnitCell"));
            options.add(new OptionColorBox("Unit Cell Color", this,
                    "unitCellColor"));
            options.add(new OptionInputBox("Unit Cell Edge Width", this,
                    "unitCellEdgeWidth"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
            @SuppressWarnings("unused")
            public void call() {
            	suspendRendering();
                updateDisplayProperties();
                resumeRendering();
                saveOptions();
            }
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget optionsGUI() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
        	options.add(new OptionSliderBox("Rotation Step", ui, "rotationStep",
        			0, 45, 5, 1, true));
            options.add(separator());
        	options.add(new OptionInputBox("Viewer Width", ui, "viewerWidth"));
			options.add(new OptionInputBox("Viewer Height", ui, "viewerHeight"));
            options.add(separator());
            options.add(new OptionCheckBox("MacOS Context Menu Workaround",
					ui, "useLeopardWorkaround"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
            @SuppressWarnings("unused")
            public void call() {
            	updateViewerSize();
                saveOptions();
            }
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget optionsNet() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
            options.add(new OptionCheckBox("Show Net", this, "showNet"));
            options.add(separator());
            options.add(new OptionInputBox("Node Radius", this, "netNodeRadius"));
            options.add(new OptionColorBox("Node Color", this, "netNodeColor"));
            options.add(separator());
            options.add(new OptionInputBox("Edge Radius", this, "netEdgeRadius"));
            options.add(new OptionColorBox("Edge Color", this, "netEdgeColor"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
            @SuppressWarnings("unused")
            public void call() {
            	suspendRendering();
            	refreshScene();
                resumeRendering();
                saveOptions();
            }
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget optionsMaterial() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
			options.add(new OptionInputBox("Diffuse Coefficient", this,
					"diffuseCoefficient"));
            options.add(separator());
			options.add(new OptionColorBox("Specular Color", this,
					"specularColor"));
			options.add(new OptionInputBox("Specular Coefficient", this,
					"specularCoefficient"));
			options.add(new OptionInputBox("Specular Exponent", this,
					"specularExponent"));
            options.add(separator());
			options.add(new OptionColorBox("Ambient Color", this,
					"ambientColor"));
			options.add(new OptionInputBox("Ambient Coefficient", this,
					"ambientCoefficient"));
//			options.add(separator());
//        	final OptionSliderBox slider =
//        		new OptionSliderBox("Transparency", this,
//        				"faceTransparency", 0, 100, 20, 5, false);
//        	slider.setFactor(0.01);
//        	options.add(slider);
//        	options.add(label("(requires 'Softviewer' in 'View' menu)",
//        			BorderContainer.EAST));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
            @SuppressWarnings("unused")
            public void call() {
            	suspendRendering();
                updateMaterials();
                resumeRendering();
                saveOptions();
            }
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget optionsEmbedding() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
            options.add(new OptionInputBox("Relaxation Step Limit", this,
					"embedderStepLimit"));
			options.add(new OptionInputBox("Edge Equalizing Priority", this,
					"equalEdgePriority"));
            options.add(separator());
			options.add(new OptionCheckBox("Skip Relaxation", this,
					"useBarycentricPositions"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
            @SuppressWarnings("unused")
            public void call() {
            	new Thread(new Runnable() {
					public void run() {
		                reembed();
		                saveOptions();
					}}).start();
            }
        };
        return optionsDialog(options, makeButton("Embed", apply, "call"));
    }

    private Widget optionsCamera() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
			options.add(new OptionSliderBox("Field Of View", this,
			"fieldOfView", 0, 120, 20, 5, false));
			options.add(new OptionColorBox("Background Color", this,
					"backgroundColor"));
            options.add(separator());
			options.add(new OptionCheckBox("Use Fog", this, "useFog"));
			options.add(new OptionCheckBox("Ignore Fog Color", this,
					"fogToBackground"));
			options.add(new OptionColorBox("Fog Color", this, "fogColor"));
			options.add(new OptionInputBox("Fog Density", this, "fogDensity"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
        }
        
        final Object apply = new Object() {
            @SuppressWarnings("unused")
            public void call() {
                updateCamera();
                saveOptions();
            }
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }

    private Widget allOptions() {
		final BTabbedPane options = new BTabbedPane();
		options.setBackground(null);
		options.add(optionsTiles(), "Tiles");
		options.add(optionsScene(), "Fill Space");
		options.add(optionsDisplay(), "Display");
		options.add(optionsNet(), "Net");
		options.add(optionsMaterial(), "Material");
		options.add(optionsEmbedding(), "Embedding");
        options.add(optionsCamera(), "Camera");
        options.add(optionsGUI(), "GUI");
		return options;
    }
    
    private Widget tilingInfo() {
		final RowContainer info = new RowContainer();
		info.setBackground(null);
		info.setDefaultLayout(new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.BOTH,
				new Insets(10, 10, 10, 10), null));

		final String[][] items = new String[][] {
				{ "_file", "File:" }, { "_num", "#" }, { "_name", "Name:" },
				{ "dim", "Dimension:" },
				{ "size", "Complexity:" }, { "transitivity", "Transitivity:" },
				{ "selfdual", "Self-dual:" }, { "signature", "Signature:" },
				{ "group", "Symmetry:" }, { "minimal", "Max. Symmetric:" }
		};

		final ColumnContainer captions = new ColumnContainer();
		captions.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				new Insets(2, 0, 2, 0), null));
		captions.setBackground(null);
		final ColumnContainer data = new ColumnContainer();
		data.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
				new Insets(2, 0, 2, 0), null));
		data.setBackground(null);
		this.tInfoFields = new LinkedHashMap<String, BLabel>();
		for (int i = 0; i < items.length; ++i) {
			final BLabel label = new BLabel();
			this.tInfoFields.put(items[i][0], label);
			data.add(label);
			captions.add(new BLabel(items[i][1]));
		}
		info.add(captions);
		info.add(data);
		return info;
	}
    
    private void setTInfo(final String key, final String value) {
    	Invoke.andWait(new Runnable() {
    		public void run() {
    	    	tInfoFields.get(key).setText(value);
    		}
    	});
    }
    
    private void setTInfo(final String key, final int value) {
    	setTInfo(key, String.valueOf(value));
    }
    
    private void setTInfo(final String key, final boolean value) {
    	setTInfo(key, value ? "yes" : "no");
    }
    
    public void hideControls() {
    	if (this.controlsFrame != null) {
    		this.controlsFrame.setVisible(false);
    	}
    }
    
    public void showControls() {
    	if (this.controlsFrame == null) {
    		// The following is a bad hack to make the viewer application's
    		// frame an acceptable parent for a Buoy dialog. We need a parented
    		// dialog here to make sure the viewer's menu bar does not disappear
    		// on MacOS.
    		final JFrame vF = viewerApp.getFrame();
    		final LayoutManager vL = vF.getContentPane().getLayout();
    		final int vCO = vF.getDefaultCloseOperation();
    	    class WrappedFrame extends BFrame {
    	    	public WrappedFrame() {
    	    		super();
    	    		vF.getContentPane().setLayout(vL);
    	    		vF.setDefaultCloseOperation(vCO);
    	    	}
    	    	
    	    	protected JFrame createComponent() {
    	    		return vF;
    	    	}
    	    };
    	    final WrappedFrame viewerFrame = new WrappedFrame();
    	    
			final BSplitPane top = new BSplitPane();
			top.setOrientation(BSplitPane.HORIZONTAL);
			final BScrollPane scrollA = new BScrollPane(allOptions(),
					BScrollPane.SCROLLBAR_AS_NEEDED,
					BScrollPane.SCROLLBAR_AS_NEEDED);
			scrollA.setBackground(textColor);
			scrollA.setForceWidth(true);
			scrollA.setForceHeight(true);
			final BScrollPane scrollB = new BScrollPane(tilingInfo(),
					BScrollPane.SCROLLBAR_AS_NEEDED,
					BScrollPane.SCROLLBAR_AS_NEEDED);
			scrollB.setBackground(textColor);
			top.add(scrollA, 0);
			top.add(scrollB, 1);
			
			final TextAreaOutputStream out = new TextAreaOutputStream();
			final PrintStream sysout = new PrintStream(out);
			System.setErr(sysout);
			System.setOut(sysout);

			final BSplitPane content = new BSplitPane();
			content.setOrientation(BSplitPane.VERTICAL);
			content.add(top, 0);
			content.add(out.getWidget(), 1);

			this.controlsFrame = new BDialog(viewerFrame, "3dt Controls", false);
			this.controlsFrame.addEventLink(WindowClosingEvent.class, this,
					"hideControls");
			this.controlsFrame.setContent(content);
			final JDialog jf = this.controlsFrame.getComponent();
			jf.setSize(600, vF.getHeight());
			jf.setLocation(vF.getWidth(), 0);
			jf.validate();

			top.setDividerLocation(350);
			content.setDividerLocation(375);
		}
    	if (!this.controlsFrame.isVisible()) {
    		final JFrame vF = viewerApp.getFrame();
    		final JDialog jf = this.controlsFrame.getComponent();
    		jf.setSize(600, vF.getHeight());
    		jf.setLocation(vF.getWidth(), 0);
    	}
    	this.controlsFrame.setVisible(true);
		this.controlsFrame.repaint();
	}
    
    public void hideAbout() {
    	if (this.aboutFrame != null) {
    		this.aboutFrame.setVisible(false);
    	}
    }
    
    public void showAbout() {
    	if (this.aboutFrame == null) {
			this.aboutFrame = new BDialog(this.controlsFrame, "About 3dt", true);
			this.aboutFrame.addEventLink(WindowClosingEvent.class, this,
					"hideAbout");
			this.aboutFrame.addEventLink(MouseClickedEvent.class, this,
					"hideAbout");
			final BLabel label = new BLabel("<html><center><h2>Gavrog 3dt</h2>"
					+ "<p>Version " + Version.full + "</p>"
					+ "<p>by Olaf Delgado-Friedrichs 1997-2008<p>"
					+ "<p>For further information visit<br>"
					+ "<em>http://gavrog.org</em><p>"
					+ "</center></html>");
			final BOutline content = BOutline.createEmptyBorder(label, 20);
			content.setBackground(textColor);
			this.aboutFrame.setContent(content);
			this.aboutFrame.pack();
		}
		this.aboutFrame.setVisible(true);
	}
    
    public static void main(final String[] args) {
        new Main(args);
    }
    
    private static void startTimer(final Stopwatch timer) {
    	timer.stop();
        timer.reset();
        timer.start();
    }
    
    private static String getTimer(final Stopwatch timer) {
        timer.stop();
        return timer.format();
    }
    
    private static void log(final Object x) {
    	Invoke.later(new Runnable() {
			public void run() {
		    	System.err.println(x);
		    	System.err.flush();
			}
    	});
    }
    

	/**
	 * @param f
	 * @param color
	 */
	private List<Tiling.Facet> equivalentFacets(final Tiling.Facet f) {
		final Object D0 = f.getChamber();
		final DSCover ds = doc().getTiling().getCover();
		final Set<Object> orb = new HashSet<Object>();
		for (Iterator it = ds.elements(); it.hasNext();) {
			final Object E0 = it.next();
			if (ds.image(E0).equals(ds.image(D0))) {
				Object E = E0;
				do {
					orb.add(E);
					E = ds.op(0, E);
					orb.add(E);
					E = ds.op(1, E);
				} while (!E0.equals(E));
			}
		}
		
		final List<Tiling.Facet> result = new ArrayList<Tiling.Facet>();
		for (final Tile b : doc().getTiles()) {
			for (int j = 0; j < b.size(); ++j) {
				final Tiling.Facet fj = b.facet(j);
				final Object E = fj.getChamber();
				if (orb.contains(E)) {
					result.add(fj);
				}
			}
		}
		return result;
	}

	private void recolorFacetClass(final Tiling.Facet f, final Color color) {
		for (Tiling.Facet facet: equivalentFacets(f)) {
			doc().setFacetClassColor(facet, color);
		}
	}

	private void uncolorFacetClass(final Tiling.Facet f) {
		for (Tiling.Facet facet: equivalentFacets(f)) {
			doc().removeFacetClassColor(facet);
		}
	}
	
	private void showAllInTile(final Tiling.Tile t) {
		for (int i = 0; i < t.size(); ++i) {
			showFacetClass(t.facet(i));
		}
	}
	
	private void showFacetClass(final Tiling.Facet f) {
		for (Tiling.Facet facet : equivalentFacets(f)) {
			doc().showFacetClass(facet);
		}
	}
	
	private void hideFacetClass(final Tiling.Facet f) {
		for (Tiling.Facet facet : equivalentFacets(f)) {
			doc().hideFacetClass(facet);
		}
	}
	
    // --- Property getters and setters
    
	/**
	 * Generic setter method. Uses introspection to find the field to set and
	 * generates a PropertyChangeEvent if the value has changed.
	 * @param name  the name of the field to set.
	 * @param value the new value.
	 */
	private void _setField(final String name, final Object value) {
		try {
			final Field field = this.getClass().getDeclaredField(name);
			final Object old = field.get(this);
			if ((value == null) ? (old != null) : (!value.equals(old))) {
				dispatchEvent(new PropertyChangeEvent(this, name, old, value));
				field.set(this, value);
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}
	
    public Color getEdgeColor() {
        return edgeColor;
    }

    public void setEdgeColor(final Color value) {
    	_setField("edgeColor", value);
    }

    public double getEdgeWidth() {
        return edgeWidth;
    }

    public void setEdgeWidth(final double value) {
    	_setField("edgeWidth", value);
    }

    public double getEdgeOpacity() {
        return edgeOpacity;
    }

    public void setEdgeOpacity(final double value) {
    	_setField("edgeOpacity", value);
    }

    public boolean getUseEdgeColor() {
        return true;
    }

    public void setUseEdgeColor(final boolean value) {
		if (value == false) {
			setEdgeOpacity(0.0);
		}
	}

    public boolean getDrawEdges() {
        return drawEdges;
    }

    public void setDrawEdges(final boolean value) {
    	_setField("drawEdges", value);
    }
    
    public boolean getDrawFaces() {
        return drawFaces;
    }

    public void setDrawFaces(final boolean value) {
    	_setField("drawFaces", value);
    }
    
    public double getTileSize() {
        return tileSize;
    }

    public void setTileSize(final double value) {
    	_setField("tileSize", value);
    }
    
    public boolean getSmoothFaces() {
        return smoothFaces;
    }

    public void setSmoothFaces(final boolean value) {
    	_setField("smoothFaces", value);
    }

    public int getSubdivisionLevel() {
        return subdivisionLevel;
    }

    public void setSubdivisionLevel(final int value) {
    	_setField("subdivisionLevel", value);
    }

    public int getTileRelaxationSteps() {
        return tileRelaxationSteps;
    }

    public void setTileRelaxationSteps(final int value) {
    	_setField("tileRelaxationSteps", value);
    }

	public int getMinX() {
		return this.minX;
	}

	public void setMinX(final int value) {
		_setField("minX", value);
	}

	public int getMaxX() {
		return this.maxX;
	}

	public void setMaxX(final int value) {
		_setField("maxX", value);
	}

	public int getMinY() {
		return this.minY;
	}

	public void setMinY(final int value) {
		_setField("minY", value);
	}

	public int getMaxY() {
		return this.maxY;
	}

	public void setMaxY(final int value) {
		_setField("maxY", value);
	}

	public int getMinZ() {
		return this.minZ;
	}

	public void setMinZ(final int value) {
		_setField("minZ", value);
	}

	public int getMaxZ() {
		return this.maxZ;
	}

	public void setMaxZ(final int value) {
		_setField("maxZ", value);
	}

	public boolean getClearOnFill() {
		return this.clearOnFill;
	}
	
	public void setClearOnFill(final boolean value) {
		_setField("clearOnFill", value);
	}
	
	public int getEmbedderStepLimit() {
        return embedderStepLimit;
    }

    public void setEmbedderStepLimit(final int value) {
    	_setField("embedderStepLimit", value);
    }

    public int getEqualEdgePriority() {
        return equalEdgePriority;
    }

    public void setEqualEdgePriority(final int value) {
    	_setField("equalEdgePriority", value);
    }

	public boolean getUseBarycentricPositions() {
		return useBarycentricPositions;
	}

	public void setUseBarycentricPositions(final boolean value) {
		_setField("useBarycentricPositions", value);
	}

	public double getAmbientCoefficient() {
		return ambientCoefficient;
	}

	public void setAmbientCoefficient(final double value) {
		_setField("ambientCoefficient", value);
	}

	public Color getAmbientColor() {
		return ambientColor;
	}

	public void setAmbientColor(final Color value) {
		_setField("ambientColor", value);
	}

	public double getDiffuseCoefficient() {
		return diffuseCoefficient;
	}

	public void setDiffuseCoefficient(final double value) {
		_setField("diffuseCoefficient", value);
	}

	public double getSpecularCoefficient() {
		return specularCoefficient;
	}

	public void setSpecularCoefficient(final double value) {
		_setField("specularCoefficient", value);
	}

	public Color getSpecularColor() {
		return specularColor;
	}

	public void setSpecularColor(final Color value) {
		_setField("specularColor", value);
	}

	public double getSpecularExponent() {
		return specularExponent;
	}

	public void setSpecularExponent(final double value) {
		_setField("specularExponent", value);
	}

	public double getFaceTransparency() {
		return faceTransparency;
	}
	
	public void setFaceTransparency(final double value) {
//		_setField("faceTransparency", value);
		_setField("faceTransparency", 0.0);
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(final Color value) {
		_setField("backgroundColor", value);
	}

	public int getEdgeRoundingLevel() {
		return edgeRoundingLevel;
	}

	public void setEdgeRoundingLevel(final int value) {
		_setField("edgeRoundingLevel", value);
	}

	public Color getFogColor() {
		return fogColor;
	}

	public void setFogColor(final Color value) {
		_setField("fogColor", value);
	}

	public double getFogDensity() {
		return fogDensity;
	}

	public void setFogDensity(final double value) {
		_setField("fogDensity", value);
	}

	public boolean getFogToBackground() {
		return fogToBackground;
	}

	public void setFogToBackground(final boolean value) {
		_setField("fogToBackground", value);
	}

	public double getFieldOfView() {
		return this.fieldOfView;
	}

	public void setFieldOfView(final double value) {
		_setField("fieldOfView", Math.max(value, 1.0));
	}

	public boolean getUseFog() {
		return useFog;
	}

	public void setUseFog(final boolean value) {
		_setField("useFog", value);
	}

	public boolean getShowUnitCell() {
		return this.showUnitCell;
	}

	public void setShowUnitCell(final boolean value) {
		_setField("showUnitCell", value);
	}

    public Color getUnitCellColor() {
        return this.unitCellColor;
    }

    public void setUnitCellColor(final Color value) {
    	_setField("unitCellColor", value);
    }

    public double getUnitCellEdgeWidth() {
        return this.unitCellEdgeWidth;
    }

    public void setUnitCellEdgeWidth(final double value) {
    	_setField("unitCellEdgeWidth", value);
    }

	public boolean getShowNet() {
		return this.showNet;
	}

	public void setShowNet(final boolean value) {
		_setField("showNet", value);
	}

	public Color getNetEdgeColor() {
		return this.netEdgeColor;
	}

	public void setNetEdgeColor(final Color value) {
		_setField("netEdgeColor", value);
	}

	public Color getNetNodeColor() {
		return this.netNodeColor;
	}

	public void setNetNodeColor(final Color value) {
		_setField("netNodeColor", value);
	}

	public double getNetEdgeRadius() {
		return this.netEdgeRadius;
	}

	public void setNetEdgeRadius(final double value) {
		_setField("netEdgeRadius", value);
	}

	public double getNetNodeRadius() {
		return this.netNodeRadius;
	}

	public void setNetNodeRadius(final double value) {
		_setField("netNodeRadius", value);
	}
}
