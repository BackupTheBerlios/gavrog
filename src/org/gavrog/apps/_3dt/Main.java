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
import java.awt.Frame;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import buoy.widget.BFileChooser;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BOutline;
import buoy.widget.BScrollPane;
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
	final private BFileChooser inFileChooser = new BFileChooser(
			BFileChooser.OPEN_FILE, "Open data file");
	final private BFileChooser outNetChooser = new BFileChooser(
			BFileChooser.SAVE_FILE, "Save net");
	final private BFileChooser outTilingChooser = new BFileChooser(
			BFileChooser.SAVE_FILE, "Save tiling");
	final private BFileChooser outSceneChooser = new BFileChooser(
			BFileChooser.SAVE_FILE, "Save scene");
	final private BFileChooser outSunflowChooser = new BFileChooser(
			BFileChooser.SAVE_FILE, "Save image");
	final private DimensionPanel dimPanel = new DimensionPanel();
	
	private String lastInputDirectory = System.getProperty("3dt.home") + "/Data";
	private String lastNetOutputDirectory = System.getProperty("user.home");
	private String lastTilingOutputDirectory = System.getProperty("user.home");
	private String lastSceneOutputDirectory = System.getProperty("user.home");
	private String lastSunflowRenderDirectory = System.getProperty("user.home");
    
    // --- geometry options
	private double edgeWidth = 0.02;
	private boolean smoothFaces = true;
	private int subdivisionLevel = 2;
	private int edgeRoundingLevel = 2;
	
	// --- display options
	private Color edgeColor = Color.BLACK;
	private boolean useEdgeColor = true;
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
    
	// --- material options
	private double ambientCoefficient = 0.0;
	private Color ambientColor = Color.WHITE;
	private double diffuseCoefficient = 0.8;
	private double specularCoefficient = 0.1;
	private double specularExponent = 15.0;
	private Color specularColor = Color.WHITE;
	
    // --- embedding options
    private int equalEdgePriority = 2;
    private int embedderStepLimit = 10000;
    private boolean useBarycentricPositions = false;
    
    // --- camera options
    private Color backgroundColor = Color.WHITE;
    private boolean useFog = false;
    private double fogDensity = 0.1;
    private Color fogColor = Color.WHITE;
    private boolean fogToBackground = true;
    private double fieldOfView = 30.0;
    
    // --- viewing options
    private int viewerWidth = 800;
    private int viewerHeight = 800;
    private boolean useLeopardWorkaround = false;

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
    
    // --- gui elements
	private BDialog aboutFrame;
	private BDialog controlsFrame;
	private HashMap<String, BLabel> tInfoFields;

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
    		infilename = args[1];
    	}
    	
        // --- retrieved stored user options
		loadOptions();

		// --- init file choosers
		JFileChooser chooser = (JFileChooser) inFileChooser.getComponent();
		chooser.addChoosableFileFilter(new ExtensionFilter("ds",
				"Delaney-Dress Symbol Files"));
		chooser.addChoosableFileFilter(new ExtensionFilter("cgd",
				"Geometric Description Files"));
		chooser.addChoosableFileFilter(new ExtensionFilter("gsl",
		"Gavrog Scene Files"));
		chooser.addChoosableFileFilter(new ExtensionFilter(new String[] { "cgd",
				"ds", "gsl" }, "All 3dt Files"));
		
		chooser = (JFileChooser) outNetChooser.getComponent();
		chooser.addChoosableFileFilter(new ExtensionFilter("pgr",
				"Raw Periodic Net Files (for Systre)"));
		
		chooser = (JFileChooser) outTilingChooser.getComponent();
		chooser.addChoosableFileFilter(new ExtensionFilter("ds",
				"Delaney-Dress Symbol Files"));

		chooser = (JFileChooser) outSceneChooser.getComponent();
		chooser.addChoosableFileFilter(new ExtensionFilter("gsl",
				"Gavrog Scene Files"));

		chooser = (JFileChooser) outSunflowChooser.getComponent();
		chooser.addChoosableFileFilter(new ExtensionFilter(new String[] {
				"png", "tga", "hdr" }, "Images files"));
		dimPanel.setDimension(new Dimension(800, 600));
		TitledBorder title = BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Dimension");
		dimPanel.setBorder(title);
		chooser.setAccessory(dimPanel);

		// --- create the viewing infrastructure
		this.world = SceneGraphUtility.createFullSceneGraphComponent("World");
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
				setViewerWidth(viewerApp.getViewingComponent().getWidth());
				setViewerHeight(viewerApp.getViewingComponent().getHeight());
				saveOptions();
			}
        });
        
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
			for (int i = 0; i < 11; ++i) {
				menu.removeMenuItem(ViewerAppMenu.VIEW_MENU, 0);
			}
			menu.removeMenuItem(ViewerAppMenu.VIEW_MENU, 2);
		} else {
			menu.removeMenuItem(ViewerAppMenu.VIEW_MENU, 13);
		}
        k = 0;
        menu.addAction(actionEncompass(), ViewerAppMenu.VIEW_MENU, k++);
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
					inFileChooser.setDirectory(new File(getLastInputDirectory()));
					final boolean success = inFileChooser.showDialog(null);
					if (!success) {
						return;
					}
					setLastInputDirectory(inFileChooser.getDirectory()
							.getAbsolutePath());
					saveOptions();
					openFile(inFileChooser.getSelectedFile().getAbsolutePath());
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
                    outTilingChooser.setDirectory(new File(
							getLastTilingOutputDirectory()));
                    final boolean success = outTilingChooser.showDialog(null);
                    if (!success) {
                        return;
                    }
                    final File dir = outTilingChooser.getDirectory();
                    setLastTilingOutputDirectory(dir.getAbsolutePath());
                    saveOptions();
                    String filename = outTilingChooser.getSelectedFile()
							.getName();
                    if (filename.indexOf('.') < 0) {
                    	filename += ".ds";
                    }
                    final String path = new File(dir, filename)
							.getAbsolutePath();
                    try {
                    	final DelaneySymbol ds = doc().getSymbol();
                    	final Writer out = new FileWriter(path);
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
                    log("Wrote file " + filename + ".");
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
                    outNetChooser.setDirectory(new File(
							getLastNetOutputDirectory()));
                    final boolean success = outNetChooser.showDialog(null);
                    if (!success) {
                        return;
                    }
                    final File dir = outNetChooser.getDirectory();
                    setLastNetOutputDirectory(dir.getAbsolutePath());
                    saveOptions();
                    String filename = outNetChooser.getSelectedFile().getName();
                    if (filename.indexOf('.') < 0) {
                    	filename += ".pgr";
                    }
                    final String path = new File(dir, filename)
							.getAbsolutePath();
                    try {
                    	final Writer out = new FileWriter(path);
                    	Output.writePGR(out, doc().getNet(), doc().getName());
                    	out.flush();
                    	out.close();
                    } catch (IOException ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote file " + filename + ".");
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
                    outSceneChooser.setDirectory(new File(
							getLastSceneOutputDirectory()));
                    final boolean success = outSceneChooser.showDialog(null);
                    if (!success) {
                        return;
                    }
                    final File dir = outSceneChooser.getDirectory();
                    setLastSceneOutputDirectory(dir.getAbsolutePath());
                    saveOptions();
                    String filename = outSceneChooser.getSelectedFile().getName();
                    if (filename.indexOf('.') < 0) {
                    	filename += ".gsl";
                    }
                    final String path = new File(dir, filename)
							.getAbsolutePath();
                    try {
                    	final Writer out = new FileWriter(path);
                    	doc().setTransformation(getViewingTransformation());
                    	out.write(doc().toXML());
                    	out.flush();
                    	out.close();
                    } catch (IOException ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote file " + filename + ".");
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
                    outSunflowChooser.setDirectory(
                    		new File(getLastSunflowRenderDirectory()));
                    final boolean success = outSunflowChooser.showDialog(null);
                    if (!success) {
                        return;
                    }
                    final File dir = outSunflowChooser.getDirectory();
                    setLastSunflowRenderDirectory(dir.getAbsolutePath());
                    saveOptions();
                    String filename = outSunflowChooser.getSelectedFile().getName();
                    if (filename.indexOf('.') < 0) {
                    	filename += ".png";
                    }
                    final File path = new File(dir, filename);
                    try {
                    	final RenderOptions opts = new RenderOptions();
                    	opts.setProgressiveRender(false);
                    	opts.setAaMin(0);
                    	opts.setAaMax(2);
                    	opts.setGiEngine("ambocc");
                    	opts.setFilter("mitchell");
                    	Sunflow.renderAndSave(viewerApp.getCurrentViewer(),
								opts, dimPanel.getDimension(), path);
                    } catch (Throwable ex) {
                    	log(ex.toString());
                    	return;
                    }
                    log("Wrote raytraced image to file " + filename + ".");
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
    
    final private BStandardDialog jumpDialog = new BStandardDialog("3dt Jump To",
    		"Number of tiling to jump to:", BStandardDialog.PLAIN);
    
    private Action actionJump() {
		final String name = "Jump To...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
					final String input = jumpDialog.showInputDialog(
							null, null, "");
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
    
	final private BStandardDialog searchDialog = new BStandardDialog(
			"3dt Search", "Type a tiling's name or part of it:",
			BStandardDialog.PLAIN);
	final private BStandardDialog searchNotFound = new BStandardDialog(
			"3dt Search", "No tiling found.", BStandardDialog.INFORMATION);
    
    private Action actionSearch() {
		final String name = "Search...";
		if (ActionRegistry.instance().get(name) == null) {
			ActionRegistry.instance().put(new AbstractJrAction(name) {
				public void actionPerformed(ActionEvent e) {
                    String input = searchDialog.showInputDialog(null, null, "");
                    if (input != null && !input.equals("")) {
	                    if (documents != null) {
	                    	for (int n = 0; n < documents.size(); ++n) {
	                    		final String name = documents.get(n).getName();
								if (name != null && name.contains(input)) {
									doTiling(n + 1);
									return;
								}
							}
	                    }
	                    searchNotFound.showMessageDialog(null);
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
    				final String name = doc().getName();
    				final String newName;
    				if (name != null) {
    					newName = name + " (dual)";
    				} else {
    					newName = "#" + tilingCounter + " dual";
    				}
    				final DSymbol ds = doc().getSymbol().dual();
    				final Document dual = new Document(newName, ds);
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
    				final String name = doc().getName();
    				final String newName;
    				if (name != null) {
    					newName = name + " (symmetric)";
    				} else {
    					newName = "#" + tilingCounter + " symmetric";
    				}
    				final DSymbol ds = (DSymbol) doc().getSymbol().minimal();
    				final Document minimal = new Document(newName, ds);
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
    
    private Action actionRecolorTileClass() {
    	final String name = "Recolor Tile Class";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
					if (selectedItem == null) {
						return;
					}
					final int kind = selectedItem.getTile().getKind();
					final Color picked = 
						JColorChooser.showDialog(viewerApp.getFrame(),
								"Set Tile Color", doc().getDefaultTileColor(kind));
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
					final Color picked = 
						JColorChooser.showDialog(viewerApp.getFrame(),
								"Set Tile Color", doc().color(selectedItem));
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
					final Color picked = 
						JColorChooser.showDialog(viewerApp.getFrame(),
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
                    updateMaterials();
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
	                    updateMaterials();
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
    				rotateScene(new double[] { 0, 1, 0 }, Math.PI / 36);
				}
			}, "Rotate the scene to the right by 5 degrees",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateLeft() {
    	final String name = "Rotate Left";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 0, 1, 0 }, -Math.PI / 36);
				}
			}, "Rotate the scene to the left by 5 degrees",
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateUp() {
    	final String name = "Rotate Up";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 1, 0, 0 }, -Math.PI / 36);
				}
			}, "Rotate the scene upward by 5 degrees",
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateDown() {
    	final String name = "Rotate Down";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 1, 0, 0 }, Math.PI / 36);
				}
			}, "Rotate the scene downward by 5 degrees",
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
		}
		return ActionRegistry.instance().get(name);
	}
    
	private Action actionRotateClockwise() {
    	final String name = "Rotate Clockwise";
    	if (ActionRegistry.instance().get(name) == null) {
    		ActionRegistry.instance().put(new AbstractJrAction(name) {
    			public void actionPerformed(ActionEvent e) {
    				rotateScene(new double[] { 0, 0, 1 }, -Math.PI / 36);
				}
			}, "Rotate the scene clockwise by 5 degrees",
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
    				rotateScene(new double[] { 0, 0, 1 }, Math.PI / 36);
				}
			}, "Rotate the scene counter-clockwise by 5 degrees",
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
							InputEvent.CTRL_DOWN_MASK));
		}
		return ActionRegistry.instance().get(name);
	}
    
    private void disableTilingChange() {
        actionOpen().setEnabled(false);
        actionFirst().setEnabled(false);
        actionNext().setEnabled(false);
        actionPrevious().setEnabled(false);
        actionLast().setEnabled(false);
        actionJump().setEnabled(false);
        actionSearch().setEnabled(false);
    }
    
    private void enableTilingChange() {
        actionOpen().setEnabled(true);
        actionFirst().setEnabled(true);
        actionNext().setEnabled(true);
        actionPrevious().setEnabled(true);
        actionLast().setEnabled(true);
        actionJump().setEnabled(true);
        actionSearch().setEnabled(true);
    }
    
    private void openFile(final String path) {
    	final String filename = new File(path).getName();
        try {
        	documents = Document.load(path);
        } catch (FileNotFoundException ex) {
        	log("Could not find file " + filename);
        	return;
        }
        log("File " + filename + " opened with " + documents.size() +
        		" tiling" + (documents.size() > 1 ? "s" : "") + ".");
        for (String key : tInfoFields.keySet()) {
            setTInfo(key, "");
        }
        setTInfo("file", filename);
        tilingCounter = 0;
        doTiling(1);
    }
    
    private void doTiling(final int n) {
        if (documents == null || n < 1 || n > documents.size()) {
            return;
        }
        disableTilingChange();
        final Frame frame = viewerApp.getFrame();
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        new Thread(new Runnable() {
            public void run() {
                tilingCounter = n;
                processTiling(documents.get(tilingCounter - 1));
                Invoke.later(new Runnable() {
                    public void run() {
                        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        enableTilingChange();
    					viewerApp.getFrame().setVisible(true);
                    }});
            }
        }).start();
    }
    
    private Document doc() {
        return this.currentDocument;
    }
    
    private void processTiling(final Document doc) {
		final Stopwatch timer = new Stopwatch();

        for (String key : tInfoFields.keySet()) {
        	if (key != "file") {
        		setTInfo(key, "");
        	}
        }
		setTInfo("#", tilingCounter + " of " + documents.size());
		String name = doc.getName();
		if (name == null) {
			name = "--";
		}
		setTInfo("name", name);
		
		this.currentDocument = doc;
		// --- get the user options saved in the document
		if (doc.getProperties() != null) {
			try {
				Config.setProperties(this, doc.getProperties());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// --- set the callback for display list changes
		doc.removeEventLink(DisplayList.Event.class, this);
		doc.addEventLink(DisplayList.Event.class, this,
				"handleDisplayListEvent");
		
		log("Constructing geometry...");
		startTimer(timer);
		try {
			construct();
		} catch (final Exception ex) {
			clearSceneGraph();
			ex.printStackTrace();
		}
		log("  " + getTimer(timer));

		// --- set camera and viewing transformation as specified in document
		updateCamera();
		if (doc.getTransformation() != null) {
			setViewingTransformation(doc.getTransformation());
		} else {
			setViewingTransformation(new Vector(0,0,1), new Vector(0,1,0));
		}
		encompass();
		
		// --- update the info display
		final DSymbol ds = doc().getSymbol();
		setTInfo("size", ds.size());
		setTInfo("dim", ds.dim());
		setTInfo("trans", doc().getTransitivity());
		setTInfo("minimal", ds.isMinimal());
		setTInfo("dual", ds.equals(ds.dual()));
		setTInfo("sig", "pending...");
		setTInfo("group", "pending...");

        final Document oldDoc = doc();
        
		final Thread worker = new Thread(new Runnable() {
			public void run() {
				final String sig = oldDoc.getSignature();
				if (doc() == oldDoc) {
					setTInfo("sig", sig);
					viewerApp.getFrame().setVisible(true);
				}
				final String group = oldDoc.getGroupName();
				if (doc() == oldDoc) {
					setTInfo("group", group);
					viewerApp.getFrame().setVisible(true);
				}
			}
		});
		worker.setPriority(Thread.MIN_PRIORITY);
		worker.start();
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
				final double tmp[] = Rn.subtract(null, corners[j], center);
				final double f = getEdgeWidth() / Rn.euclideanNorm(tmp);
				Rn.linearCombination(p[j], 1 - f, corners[j], f, center);
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
        updateMaterial(a, c);
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
        updateMaterial(a, c);
        sgc.setAppearance(a);
        this.net.addChild(sgc);
        this.node2item.put(sgc, item);
        this.item2node.put(item, sgc);
    }
    
    private void recolorTile(final DisplayList.Item item, final Color color) {
    	final Appearance a;
		if (color != null) {
			a = new Appearance();
			updateMaterial(a, color);
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
        suspendRendering();
        makeCopies();
        resumeRendering();
        encompass();
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
        makeUnitCell();
        resumeRendering();
        encompass();
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
		updateMaterial(a, c);
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
			updateMaterial(this.materials[i], doc().getDefaultTileColor(i));
		}
	}
    
    private void updateMaterial(final Appearance a, final Color c) {
		a.setAttribute(CommonAttributes.DIFFUSE_COLOR, c);

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
    }
    
    private void updateMaterials() {
    	if (this.templates == null) {
    		return;
    	}
        final Stopwatch timer = new Stopwatch();
        log("    Updating materials...");
        startTimer(timer);
        
        for (final Tile b: doc().getTiles()) {
        	final int i = b.getIndex();
        	final SceneGraphComponent sgc = this.templates[b.getIndex()];
            updateMaterial(this.materials[i], doc().getDefaultTileColor(i));
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
						updateMaterial(a, c);
						child.setAppearance(a);
					}
					child.setVisible(!doc().isHiddenFacetClass(f));
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
        
    	for (int i = 0; i < this.templates.length; ++i) {
    		final SceneGraphComponent sgc = templates[i];
    		for (Iterator iter = sgc.getChildNodes().iterator(); iter.hasNext();) {
    			final Object node = iter.next();
    			if (node instanceof SceneGraphComponent) {
    				final SceneGraphComponent child = (SceneGraphComponent) node;
    				if (child.getName().startsWith("outline")) {
    					child.setVisible(getDrawEdges());
    					if (child.isVisible()) {
    						final Appearance a = new Appearance();
    						if (getUseEdgeColor()) {
    							a.setAttribute(CommonAttributes.DIFFUSE_COLOR,
										getEdgeColor());
    						}
    						child.setAppearance(a);
    					}
    				} else if (child.getName().startsWith("face:")) {
    					child.setVisible(getDrawFaces());
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
    	if (doc().size() == 0) {
            final Stopwatch timer = new Stopwatch();
            log("    Generating scene...");
            startTimer(timer);
            
    		clearSceneGraph();
    		final List<Vector> vecs = replicationVectors();
	        for (final Tile b: doc().getTiles()) {
	        	for (final Vector s: doc().centerIntoUnitCell(b)) {
	        		for (Vector v: vecs) {
	        			final Vector shift = (Vector) s.plus(v);
	        			doc().add(b, shift);
	        			//makeTileOutline(b, shift);
	        		}
	        	}
	        }
	        log("      " + getTimer(timer));
    	} else {
    		refreshScene();
    	}
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
    	encompass(viewerApp.getCurrentViewer(), scene);
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
        final Appearance a = this.viewerApp.getCurrentViewer().getSceneRoot().getAppearance();
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
					if (useLeopardWorkaround) {
						restoreViewer();
					}
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (useLeopardWorkaround) {
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
					if (useLeopardWorkaround) {
						restoreViewer();
					}
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (useLeopardWorkaround) {
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
					if (useLeopardWorkaround) {
						restoreViewer();
					}
				}

				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (useLeopardWorkaround) {
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
				vSwitch.getCurrentViewer().render();
				break;
			}
		}
	}
    
    @SuppressWarnings("unused")
	private void restoreViewer() {
    	if (this.previousViewer >= 0) {
			final ViewerSwitch vSwitch = viewerApp.getViewerSwitch();
			vSwitch.selectViewer(this.previousViewer);
			vSwitch.getCurrentViewer().render();
			this.previousViewer = -1;
    	}
    }
    
    private void updateViewerSize() {
    	final java.awt.Component viewer = viewerApp.getViewingComponent();
    	// -- make sure to trigger a property change event
    	viewer.setPreferredSize(null);
    	// -- now set to the desired size
    	viewer.setPreferredSize(new Dimension(getViewerWidth(),
				getViewerHeight()));
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
    	final Properties ourProps;
		try {
			ourProps = Config.getProperties(this);
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		if (doc() != null) {
			doc().setProperties(ourProps);
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
    		Config.setProperties(this, ourProps);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    
    private ColumnContainer emptyOptionsContainer() {
        final ColumnContainer options = new ColumnContainer();
        options.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
                new Insets(2, 5, 2, 5), null));
        options.setBackground(null);
        return options;
    }
    
    private Widget optionsDialog(final Widget options, final Widget buttons) {
        final BorderContainer dialog = new BorderContainer();
        dialog.setBackground(textColor);
        dialog.add(options, BorderContainer.NORTH, new LayoutInfo(
                LayoutInfo.NORTH, LayoutInfo.NONE, defaultInsets, null));
        dialog.add(buttons, BorderContainer.SOUTH, new LayoutInfo(
                LayoutInfo.SOUTH, LayoutInfo.NONE, defaultInsets, null));
        return dialog;
    }
    
    private Widget optionsGeometry() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
            options.add(new OptionInputBox("Edge Width", this, "edgeWidth"));
            options.add(new OptionInputBox("Surface Detail", this,
                    "subdivisionLevel"));
            options.add(new OptionInputBox("Edge Creasing", this,
					"edgeRoundingLevel"));
            options.add(new OptionCheckBox("Smooth Face Shading", this,
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
    
    private Widget optionsScene() {
    	final ColumnContainer options = emptyOptionsContainer();
    	try {
    		options.add(new OptionInputBox("x from", this, "minX"));
    		options.add(new OptionInputBox("x to", this, "maxX"));
    		options.add(new OptionInputBox("y from", this, "minY"));
    		options.add(new OptionInputBox("y to", this, "maxY"));
    		options.add(new OptionInputBox("z from", this, "minZ"));
    		options.add(new OptionInputBox("z to", this, "maxZ"));
        } catch (final Exception ex) {
            log(ex.toString());
            return null;
    	}
        
        final Object apply = new Object() {
			@SuppressWarnings("unused")
			public void call() {
				new Thread(new Runnable() {
					public void run() {
						suspendRendering();
						doc().removeAll();
						makeCopies();
						encompass();
						resumeRendering();
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
            options.add(new OptionCheckBox("Draw Edges", this, "drawEdges"));
            options.add(new OptionCheckBox("Use Edge Color", this,
            		"useEdgeColor"));
            options.add(new OptionColorBox("Edge Color", this, "edgeColor"));
            options.add(new OptionCheckBox("Draw Faces", this, "drawFaces"));
            options.add(new OptionInputBox("Relative Tile Size", this,
                    "tileSize"));
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
                updateMaterials();
                resumeRendering();
                saveOptions();
            }
        };
        return optionsDialog(options, makeButton("Apply", apply, "call"));
    }
    
    private Widget optionsGUI() {
        final ColumnContainer options = emptyOptionsContainer();
        try {
            options.add(new OptionInputBox("Viewer Width", this,
            		"viewerWidth"));
			options.add(new OptionInputBox("Viewer Height", this,
					"viewerHeight"));
            options.add(new OptionCheckBox("MacOS Context Menu Workaround",
					this, "useLeopardWorkaround"));
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
            options.add(new OptionInputBox("Edge Radius", this, "netEdgeRadius"));
            options.add(new OptionInputBox("Node Radius", this, "netNodeRadius"));
            options.add(new OptionColorBox("Edge Color", this, "netEdgeColor"));
            options.add(new OptionColorBox("Node Color", this, "netNodeColor"));
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
        	options.add(new OptionColorBox("Ambient Color", this,
					"ambientColor"));
			options.add(new OptionInputBox("Ambient Coefficient", this,
					"ambientCoefficient"));
			options.add(new OptionInputBox("Diffuse Coefficient", this,
					"diffuseCoefficient"));
			options.add(new OptionColorBox("Specular Color", this,
					"specularColor"));
			options.add(new OptionInputBox("Specular Coefficient", this,
					"specularCoefficient"));
			options.add(new OptionInputBox("Specular Exponent", this,
					"specularExponent"));
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
			options.add(new OptionInputBox("Field Of View (in degrees)", this,
					"fieldOfView"));
			options.add(new OptionColorBox("Background Color", this,
					"backgroundColor"));
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
		options.add(optionsGeometry(), "Geometry");
		options.add(optionsScene(), "Unit Cells");
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
				{ "file", "File:" }, { "#", "#" }, { "name", "Name:" },
				{ "dim", "Dimension:" },
				{ "size", "Complexity:" }, { "trans", "Transitivity:" },
				{ "dual", "Self-dual:" }, { "sig", "Signature:" },
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
		this.tInfoFields = new HashMap<String, BLabel>();
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

			top.setDividerLocation(300);
			content.setDividerLocation(350);
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
    
    // --- Property getters and setters
    
    public Color getEdgeColor() {
        return edgeColor;
    }

    public void setEdgeColor(Color edgeColor) {
    	if (edgeColor != this.edgeColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "edgeColor",
					this.edgeColor, edgeColor));
			this.edgeColor = edgeColor;
    	}
    }

    public double getEdgeWidth() {
        return edgeWidth;
    }

    public void setEdgeWidth(double edgeWidth) {
    	if (edgeWidth != this.edgeWidth) {
    		dispatchEvent(new PropertyChangeEvent(this, "edgeWidth",
    				this.edgeWidth, edgeWidth));
    		this.edgeWidth = edgeWidth;
    	}
    }

    public boolean getUseEdgeColor() {
        return useEdgeColor;
    }

    public void setUseEdgeColor(boolean useEdgeColor) {
    	if (useEdgeColor != this.useEdgeColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "useEdgeColor",
    				this.useEdgeColor, useEdgeColor));
    		this.useEdgeColor = useEdgeColor;
    	}
    }

    public boolean getDrawEdges() {
        return drawEdges;
    }

    public void setDrawEdges(boolean drawEdges) {
    	if (drawEdges != this.drawEdges) {
    		dispatchEvent(new PropertyChangeEvent(this, "drawEdges",
    				this.drawEdges, drawEdges));
    		this.drawEdges = drawEdges;
    	}
    }
    
    public boolean getDrawFaces() {
        return drawFaces;
    }

    public void setDrawFaces(boolean drawFaces) {
    	if (drawFaces != this.drawFaces) {
    		dispatchEvent(new PropertyChangeEvent(this, "drawFaces",
    				this.drawFaces, drawFaces));
    		this.drawFaces = drawFaces;
    	}
    }
    
    public double getTileSize() {
        return tileSize;
    }

    public void setTileSize(double tileSize) {
    	if (tileSize != this.tileSize) {
    		dispatchEvent(new PropertyChangeEvent(this, "tileSize",
    				this.tileSize, tileSize));
    		this.tileSize = tileSize;
    	}
    }
    
    public boolean getSmoothFaces() {
        return smoothFaces;
    }

    public void setSmoothFaces(boolean smoothFaces) {
    	if (smoothFaces != this.smoothFaces) {
    		dispatchEvent(new PropertyChangeEvent(this, "smoothFaces",
    				this.smoothFaces, smoothFaces));
    		this.smoothFaces = smoothFaces;
    	}
    }

    public int getSubdivisionLevel() {
        return subdivisionLevel;
    }

    public void setSubdivisionLevel(int subdivisionLevel) {
    	if (subdivisionLevel != this.subdivisionLevel) {
    		dispatchEvent(new PropertyChangeEvent(this, "subdivisionLevel",
    				this.subdivisionLevel, subdivisionLevel));
    		this.subdivisionLevel = subdivisionLevel;
    	}
    }

	public int getMinX() {
		return this.minX;
	}

	public void setMinX(int minX) {
    	if (minX != this.minX) {
    		dispatchEvent(new PropertyChangeEvent(this, "minX", this.minX, minX));
    		this.minX = minX;
    	}
	}

	public int getMaxX() {
		return this.maxX;
	}

	public void setMaxX(int maxX) {
    	if (maxX != this.maxX) {
    		dispatchEvent(new PropertyChangeEvent(this, "maxX", this.maxX, maxX));
    		this.maxX = maxX;
    	}
	}

	public int getMinY() {
		return this.minY;
	}

	public void setMinY(int minY) {
    	if (minY != this.minY) {
    		dispatchEvent(new PropertyChangeEvent(this, "minY", this.minY, minY));
    		this.minY = minY;
    	}
	}

	public int getMaxY() {
		return this.maxY;
	}

	public void setMaxY(int maxY) {
    	if (maxY != this.maxY) {
    		dispatchEvent(new PropertyChangeEvent(this, "maxY", this.maxY, maxY));
    		this.maxY = maxY;
    	}
	}

	public int getMinZ() {
		return this.minZ;
	}

	public void setMinZ(int minZ) {
    	if (minZ != this.minZ) {
    		dispatchEvent(new PropertyChangeEvent(this, "minZ", this.minZ, minZ));
    		this.minZ = minZ;
    	}
	}

	public int getMaxZ() {
		return this.maxZ;
	}

	public void setMaxZ(int maxZ) {
    	if (maxZ != this.maxZ) {
    		dispatchEvent(new PropertyChangeEvent(this, "maxZ", this.maxZ, maxZ));
    		this.maxZ = maxZ;
    	}
	}

	public int getEmbedderStepLimit() {
        return embedderStepLimit;
    }

    public void setEmbedderStepLimit(int embedderStepLimit) {
    	if (embedderStepLimit != this.embedderStepLimit) {
    		dispatchEvent(new PropertyChangeEvent(this, "embedderStepLimit",
    				this.embedderStepLimit, embedderStepLimit));
    		this.embedderStepLimit = embedderStepLimit;
    	}
    }

    public int getEqualEdgePriority() {
        return equalEdgePriority;
    }

    public void setEqualEdgePriority(int equalEdgePriority) {
    	if (equalEdgePriority != this.equalEdgePriority) {
    		dispatchEvent(new PropertyChangeEvent(this, "equalEdgePriority",
    				this.equalEdgePriority, equalEdgePriority));
    		this.equalEdgePriority = equalEdgePriority;
    	}
    }

	public boolean getUseBarycentricPositions() {
		return useBarycentricPositions;
	}

	public void setUseBarycentricPositions(boolean useBarycentricPositions) {
    	if (useBarycentricPositions != this.useBarycentricPositions) {
			dispatchEvent(new PropertyChangeEvent(this,
					"useBarycentricPositions", this.useBarycentricPositions,
					useBarycentricPositions));
			this.useBarycentricPositions = useBarycentricPositions;
		}
	}

	public double getAmbientCoefficient() {
		return ambientCoefficient;
	}

	public void setAmbientCoefficient(double ambientCoefficient) {
    	if (ambientCoefficient != this.ambientCoefficient) {
    		dispatchEvent(new PropertyChangeEvent(this, "ambientCoefficient",
    				this.ambientCoefficient, ambientCoefficient));
    		this.ambientCoefficient = ambientCoefficient;
    	}
	}

	public Color getAmbientColor() {
		return ambientColor;
	}

	public void setAmbientColor(Color ambientColor) {
    	if (ambientColor != this.ambientColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "ambientColor",
    				this.ambientColor, ambientColor));
    		this.ambientColor = ambientColor;
    	}
	}

	public double getDiffuseCoefficient() {
		return diffuseCoefficient;
	}

	public void setDiffuseCoefficient(double diffuseCoefficient) {
    	if (diffuseCoefficient != this.diffuseCoefficient) {
    		dispatchEvent(new PropertyChangeEvent(this, "diffuseCoefficient",
    				this.diffuseCoefficient, diffuseCoefficient));
    		this.diffuseCoefficient = diffuseCoefficient;
    	}
	}

	public double getSpecularCoefficient() {
		return specularCoefficient;
	}

	public void setSpecularCoefficient(double specularCoefficient) {
    	if (specularCoefficient != this.specularCoefficient) {
    		dispatchEvent(new PropertyChangeEvent(this, "specularCoefficient",
    				this.specularCoefficient, specularCoefficient));
    		this.specularCoefficient = specularCoefficient;
    	}
	}

	public Color getSpecularColor() {
		return specularColor;
	}

	public void setSpecularColor(Color specularColor) {
    	if (specularColor != this.specularColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "specularColor",
    				this.specularColor, specularColor));
    		this.specularColor = specularColor;
    	}
	}

	public double getSpecularExponent() {
		return specularExponent;
	}

	public void setSpecularExponent(double specularExponent) {
    	if (specularExponent != this.specularExponent) {
    		dispatchEvent(new PropertyChangeEvent(this, "specularExponent",
    				this.specularExponent, specularExponent));
    		this.specularExponent = specularExponent;
    	}
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
    	if (backgroundColor != this.backgroundColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "backgroundColor",
    				this.backgroundColor, backgroundColor));
    		this.backgroundColor = backgroundColor;
    	}
	}

	public String getLastInputDirectory() {
		return lastInputDirectory;
	}

	public void setLastInputDirectory(String lastInputDirectory) {
    	if (lastInputDirectory != this.lastInputDirectory) {
    		dispatchEvent(new PropertyChangeEvent(this, "lastInputDirectory",
    				this.lastInputDirectory, lastInputDirectory));
    		this.lastInputDirectory = lastInputDirectory;
    	}
	}

	public String getLastNetOutputDirectory() {
		return this.lastNetOutputDirectory;
	}

	public void setLastNetOutputDirectory(String lastNetOutputDirectory) {
    	if (lastNetOutputDirectory != this.lastNetOutputDirectory) {
    		dispatchEvent(new PropertyChangeEvent(this,
					"lastNetOutputDirectory", this.lastNetOutputDirectory,
					lastNetOutputDirectory));
    		this.lastNetOutputDirectory = lastNetOutputDirectory;
    	}
	}

	public String getLastSceneOutputDirectory() {
		return this.lastSceneOutputDirectory;
	}

	public void setLastSceneOutputDirectory(String lastSceneOutputDirectory) {
    	if (lastSceneOutputDirectory != this.lastSceneOutputDirectory) {
    		dispatchEvent(new PropertyChangeEvent(this,
					"lastSceneOutputDirectory", this.lastSceneOutputDirectory,
					lastSceneOutputDirectory));
    		this.lastSceneOutputDirectory = lastSceneOutputDirectory;
    	}
	}

	public String getLastTilingOutputDirectory() {
		return this.lastTilingOutputDirectory;
	}

	public void setLastTilingOutputDirectory(String lastTilingOutputDirectory) {
    	if (lastTilingOutputDirectory != this.lastTilingOutputDirectory) {
    		dispatchEvent(new PropertyChangeEvent(this,
					"lastTilingOutputDirectory",
					this.lastTilingOutputDirectory, lastTilingOutputDirectory));
    		this.lastTilingOutputDirectory = lastTilingOutputDirectory;
    	}
	}

	public String getLastSunflowRenderDirectory() {
		return this.lastSunflowRenderDirectory;
	}

	public void setLastSunflowRenderDirectory(String lastSunflowRenderDirectory) {
		if (lastSunflowRenderDirectory != this.lastSunflowRenderDirectory) {
			dispatchEvent(new PropertyChangeEvent(this,
					"lastSunflowRenderDirectory",
					this.lastSunflowRenderDirectory, lastSunflowRenderDirectory));
			this.lastSunflowRenderDirectory = lastSunflowRenderDirectory;
		}
	}

	public int getEdgeRoundingLevel() {
		return edgeRoundingLevel;
	}

	public void setEdgeRoundingLevel(int edgeRoundingLevel) {
    	if (edgeRoundingLevel != this.edgeRoundingLevel) {
    		dispatchEvent(new PropertyChangeEvent(this, "edgeRoundingLevel",
    				this.edgeRoundingLevel, edgeRoundingLevel));
    		this.edgeRoundingLevel = edgeRoundingLevel;
    	}
	}

	public Color getFogColor() {
		return fogColor;
	}

	public void setFogColor(Color fogColor) {
    	if (fogColor != this.fogColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "fogColor",
    				this.fogColor, fogColor));
    		this.fogColor = fogColor;
    	}
	}

	public double getFogDensity() {
		return fogDensity;
	}

	public void setFogDensity(double fogDensity) {
    	if (fogDensity != this.fogDensity) {
    		dispatchEvent(new PropertyChangeEvent(this, "fogDensity",
    				this.fogDensity, fogDensity));
    		this.fogDensity = fogDensity;
    	}
	}

	public boolean getFogToBackground() {
		return fogToBackground;
	}

	public void setFogToBackground(boolean fogToBackground) {
    	if (fogToBackground != this.fogToBackground) {
    		dispatchEvent(new PropertyChangeEvent(this, "fogToBackground",
    				this.fogToBackground, fogToBackground));
    		this.fogToBackground = fogToBackground;
    	}
	}

	public double getFieldOfView() {
		return this.fieldOfView;
	}

	public void setFieldOfView(double fieldOfView) {
    	if (fieldOfView != this.fieldOfView) {
    		fieldOfView = Math.max(fieldOfView, 0.1);
    		dispatchEvent(new PropertyChangeEvent(this, "fieldOfView",
    				this.fieldOfView, fieldOfView));
    		this.fieldOfView = fieldOfView;
    	}
	}

	public boolean getUseFog() {
		return useFog;
	}

	public void setUseFog(boolean useFog) {
    	if (useFog != this.useFog) {
    		dispatchEvent(new PropertyChangeEvent(this, "useFog", this.useFog,
					useFog));
    		this.useFog = useFog;
    	}
	}

	public boolean getShowUnitCell() {
		return this.showUnitCell;
	}

	public void setShowUnitCell(boolean showUnitCell) {
    	if (showUnitCell != this.showUnitCell) {
    		dispatchEvent(new PropertyChangeEvent(this, "showUnitCell",
    				this.showUnitCell, showUnitCell));
    		this.showUnitCell = showUnitCell;
    	}
	}

    public Color getUnitCellColor() {
        return this.unitCellColor;
    }

    public void setUnitCellColor(Color unitCellColor) {
    	if (unitCellColor != this.unitCellColor) {
    		dispatchEvent(new PropertyChangeEvent(this, "unitCellColor",
    				this.unitCellColor, unitCellColor));
    		this.unitCellColor = unitCellColor;
    	}
    }

    public double getUnitCellEdgeWidth() {
        return this.unitCellEdgeWidth;
    }

    public void setUnitCellEdgeWidth(double unitCellEdgeWidth) {
    	if (unitCellEdgeWidth != this.unitCellEdgeWidth) {
    		dispatchEvent(new PropertyChangeEvent(this, "unitCellEdgeWidth",
    				this.unitCellEdgeWidth, unitCellEdgeWidth));
    		this.unitCellEdgeWidth = unitCellEdgeWidth;
    	}
    }

    public int getViewerWidth() {
    	return this.viewerWidth;
    }
    
	public void setViewerWidth(int viewerWidth) {
		if (viewerWidth != this.viewerWidth) {
			dispatchEvent(new PropertyChangeEvent(this, "viewerWidth",
					this.viewerWidth, viewerWidth));
			this.viewerWidth = viewerWidth;
		}
	}

    public int getViewerHeight() {
    	return this.viewerHeight;
    }
    
	public void setViewerHeight(int viewerHeight) {
		if (viewerHeight != this.viewerHeight) {
			dispatchEvent(new PropertyChangeEvent(this, "viewerHeight",
					this.viewerHeight, viewerHeight));
			this.viewerHeight = viewerHeight;
		}
	}

	public boolean getUseLeopardWorkaround() {
		return this.useLeopardWorkaround;
	}

	public void setUseLeopardWorkaround(boolean useLeopardWorkaround) {
		if (useLeopardWorkaround != this.useLeopardWorkaround) {
			dispatchEvent(new PropertyChangeEvent(this, "useLeopardWorkaround",
					this.useLeopardWorkaround, useLeopardWorkaround));
			this.useLeopardWorkaround = useLeopardWorkaround;
		}
	}

	public boolean getShowNet() {
		return this.showNet;
	}

	public void setShowNet(boolean showNet) {
		if (showNet != this.showNet) {
			dispatchEvent(new PropertyChangeEvent(this, "showNet",
					this.showNet, showNet));
			this.showNet = showNet;
		}
	}

	public Color getNetEdgeColor() {
		return this.netEdgeColor;
	}

	public void setNetEdgeColor(Color netEdgeColor) {
		if (netEdgeColor != this.netEdgeColor) {
			dispatchEvent(new PropertyChangeEvent(this, "netEdgeColor",
					this.netEdgeColor, netEdgeColor));
			this.netEdgeColor = netEdgeColor;
		}
	}

	public Color getNetNodeColor() {
		return this.netNodeColor;
	}

	public void setNetNodeColor(Color netNodeColor) {
		if (netNodeColor != this.netNodeColor) {
			dispatchEvent(new PropertyChangeEvent(this, "netNodeColor",
					this.netNodeColor, netNodeColor));
			this.netNodeColor = netNodeColor;
		}
	}

	public double getNetEdgeRadius() {
		return this.netEdgeRadius;
	}

	public void setNetEdgeRadius(double netEdgeRadius) {
		if (netEdgeRadius != this.netEdgeRadius) {
			dispatchEvent(new PropertyChangeEvent(this, "netEdgeRadius",
					this.netEdgeRadius, netEdgeRadius));
			this.netEdgeRadius = netEdgeRadius;
		}
	}

	public double getNetNodeRadius() {
		return this.netNodeRadius;
	}

	public void setNetNodeRadius(double netNodeRadius) {
		if (netNodeRadius != this.netNodeRadius) {
			dispatchEvent(new PropertyChangeEvent(this, "netNodeRadius",
					this.netNodeRadius, netNodeRadius));
			this.netNodeRadius = netNodeRadius;
		}
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
}
