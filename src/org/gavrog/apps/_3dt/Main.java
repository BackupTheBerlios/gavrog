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
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.gavrog.box.gui.Config;
import org.gavrog.box.gui.ExtensionFilter;
import org.gavrog.box.gui.Invoke;
import org.gavrog.box.gui.OptionCheckBox;
import org.gavrog.box.gui.OptionColorBox;
import org.gavrog.box.gui.OptionInputBox;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.graphics.Surface;
import org.gavrog.joss.pgraphs.io.Output;
import org.gavrog.joss.tilings.Tiling;
import org.gavrog.joss.tilings.Tiling.Facet;
import org.gavrog.joss.tilings.Tiling.Tile;

import buoy.event.CommandEvent;
import buoy.event.EventSource;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
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
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedLineSetFactory;
import de.jreality.io.JrScene;
import de.jreality.jogl.JOGLRenderer;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
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
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.ui.viewerapp.ViewerAppMenu;
import de.jreality.ui.viewerapp.ViewerSwitch;
import de.jreality.ui.viewerapp.actions.AbstractJrAction;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class Main extends EventSource {
	// --- some constants used in the GUI
    final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);
	final private static Insets defaultInsets = new Insets(5, 5, 5, 5);

	private final static String TILING_MENU = "Tiling";
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
	
	private String lastInputDirectory = System.getProperty("3dt.home") + "/Data";
	private String lastNetOutputDirectory = System.getProperty("user.home");
	private String lastTilingOutputDirectory = System.getProperty("user.home");
	private String lastSceneOutputDirectory = System.getProperty("user.home");
    
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
    private boolean perspective = true;
    private Color backgroundColor = Color.WHITE;
    private boolean useFog = false;
    private double fogDensity = 0.1;
    private Color fogColor = Color.WHITE;
    private boolean fogToBackground = true;
    private double fieldOfView = 30.0;
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
	private SceneGraphNode selectedBodyNode = null;
	private int selectedFace = -1;
    
    // --- gui elements
	private BFrame aboutFrame;
	private BFrame controlsFrame;
	private HashMap<String, BLabel> tInfoFields;

	// --- scene graph components
	final private ViewerApp viewerApp;
	private int previousViewer = -1;
    final private JrScene scene;
    final private SceneGraphComponent world;

    private SceneGraphComponent tiling;
	private SceneGraphComponent unitCell;
    private SceneGraphComponent templates[];
    private Appearance materials[];
    
    /**
     * Constructs an instance.
     * @param args command-line arguments
     */
    public Main(final String[] args) {
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

		// --- create the viewing infrastructure
		this.world = SceneGraphUtility.createFullSceneGraphComponent("world");
		viewerApp = new ViewerApp(world);
		this.scene = viewerApp.getJrScene();
        viewerApp.setAttachNavigator(false);
        viewerApp.setAttachBeanShell(false);
        
        // --- create a root node for the tiling
        this.tiling = new SceneGraphComponent("tiling");
        final Appearance a = new Appearance();
        this.tiling.setAppearance(a);
        a.setAttribute(CommonAttributes.EDGE_DRAW, false);
        a.setAttribute(CommonAttributes.VERTEX_DRAW, false);
        this.tiling.addTool(new SelectionTool());
        this.unitCell = new SceneGraphComponent("unitCell");
        
        // --- remove the encompass tool (we'll have a menu entry for that)
        final SceneGraphComponent root = this.viewerApp.getCurrentViewer().getSceneRoot();
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
        root.addTool(new ClickWheelCameraZoomTool());
        
        // --- change the menu
        modifyDefaultMenu(viewerApp.getMenu());
        
        // --- set up the viewer window
        updateCamera();
        viewerApp.update();
        viewerApp.display();
        viewerApp.getFrame().setTitle("3dt Viewer");
        
        // --- show the controls window
        Invoke.andWait(new Runnable() { public void run() { showControls(); }});
        
        // --- open a file if specified on the command line
        if (args.length > 0) {
        	openFile(args[0]);
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
        k += 2; // jump over export submenu

        // --- modify the View menu
        for (int i = 0; i < 10; ++i) {
            menu.removeMenuItem(ViewerAppMenu.VIEW_MENU, 0);
        }
        k = 0;
        menu.addAction(actionEncompass(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionXView(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionYView(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionZView(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(action111View(), ViewerAppMenu.VIEW_MENU, k++);
        menu.addSeparator(ViewerAppMenu.VIEW_MENU, k++);
        menu.addAction(actionShowControls(), ViewerAppMenu.VIEW_MENU, k++);

        // --- create a help menu
        menu.addMenu(new JMenu(HELP_MENU));
        menu.addAction(actionAbout(), HELP_MENU);
    }
    
    private AbstractJrAction _aboutAction = null;
    
    private Action actionAbout() {
        if (_aboutAction == null) {
            _aboutAction = new AbstractJrAction("About 3dt") {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    showAbout();
                }
            };
        }
        return _aboutAction;
    }
    
    private AbstractJrAction _showControlsAction = null;
    
    private Action actionShowControls() {
        if (_showControlsAction == null) {
            _showControlsAction = new AbstractJrAction("Show Controls") {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    showControls();
                }
            };
        }
        return _showControlsAction;
    }
    
    private AbstractJrAction _openAction = null;
    
    private Action actionOpen() {
        if (_openAction == null) {
            _openAction = new AbstractJrAction("Open...") {
                private static final long serialVersionUID = 1L;

				@Override
                public void actionPerformed(ActionEvent e) {
                    inFileChooser
                            .setDirectory(new File(getLastInputDirectory()));
                    final boolean success = inFileChooser.showDialog(null);
                    if (!success) {
                        return;
                    }
                    setLastInputDirectory(inFileChooser.getDirectory()
							.getAbsolutePath());
                    saveOptions();
                    openFile(inFileChooser.getSelectedFile().getAbsolutePath());
                }
            };
            _openAction.setShortDescription("Open a tiling file");
            _openAction.setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                    InputEvent.CTRL_MASK));
        }
        return _openAction;
    }
    
    private AbstractJrAction _saveTilingAction = null;
    
    private Action actionSaveTiling() {
        if (_saveTilingAction == null) {
            _saveTilingAction = new AbstractJrAction("Save tiling...") {
                private static final long serialVersionUID = 1L;

				@Override
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
            };
            _saveTilingAction.setShortDescription(
            		"Save the raw tiling as a Delaney-Dress symbol");
        }
        return _saveTilingAction;
    }
    
    private AbstractJrAction _saveNetAction = null;
    
    private Action actionSaveNet() {
        if (_saveNetAction == null) {
            _saveNetAction = new AbstractJrAction("Save net...") {
                private static final long serialVersionUID = 1L;

				@Override
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
            };
            _saveNetAction.setShortDescription(
            		"Save the raw net as a Systre file");
        }
        return _saveNetAction;
    }
    
    private AbstractJrAction _saveSceneAction = null;
    
    private Action actionSaveScene() {
        if (_saveSceneAction == null) {
            _saveSceneAction = new AbstractJrAction("Save scene...") {
                private static final long serialVersionUID = 1L;

				@Override
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
            };
            _saveSceneAction.setShortDescription("Save the scene");
        }
        return _saveSceneAction;
    }
    
    private AbstractJrAction _firstAction = null;
    
    private Action actionFirst() {
		if (_firstAction == null) {
			_firstAction = new AbstractJrAction("First") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
                    doTiling(1);
				}
			};
			_firstAction.setShortDescription("Display the first tiling");
			_firstAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_F, 0));
		}

		return _firstAction;
	}
    
    private AbstractJrAction _nextAction = null;
    
    private Action actionNext() {
        if (_nextAction == null) {
            _nextAction = new AbstractJrAction("Next") {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    doTiling(tilingCounter + 1);
                }
            };
            _nextAction.setShortDescription("Display the next tiling");
            _nextAction.setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                    0));
        }
        
        return _nextAction;
    }
    
    private AbstractJrAction _previousAction = null;
    
    private Action actionPrevious() {
		if (_previousAction == null) {
			_previousAction = new AbstractJrAction("Previous") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
                    doTiling(tilingCounter - 1);
				}
			};
			_previousAction.setShortDescription("Display the previous tiling");
			_previousAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_P, 0));
		}
        
        return _previousAction;
    }
    
    private AbstractJrAction _lastAction = null;
    
    private Action actionLast() {
		if (_lastAction == null) {
			_lastAction = new AbstractJrAction("Last") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
                    doTiling(documents.size());
				}
			};
			_lastAction.setShortDescription("Display the last tiling");
			_lastAction.setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_L,
					0));
		}

		return _lastAction;
	}
    
    private AbstractJrAction _jumpAction = null;
    final private BStandardDialog jumpDialog = new BStandardDialog("3dt Jump To",
    		"Number of tiling to jump to:", BStandardDialog.PLAIN);
    
    private Action actionJump() {
		if (_jumpAction == null) {
			_jumpAction = new AbstractJrAction("Jump To...") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
                    final String input = jumpDialog.showInputDialog(null, null,
                            "");
                    final int n;
                    try {
                        n = Integer.parseInt(input);
                    } catch (final NumberFormatException ex) {
                        return;
                    }
                    doTiling(n);
				}
			};
			_jumpAction.setShortDescription("Jump to a specific tiling");
			_jumpAction.setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_J,
					0));
		}

		return _jumpAction;
	}
    
    private AbstractJrAction _searchAction = null;
	final private BStandardDialog searchDialog = new BStandardDialog(
			"3dt Search", "Type a tiling's name or part of it:",
			BStandardDialog.PLAIN);
	final private BStandardDialog searchNotFound = new BStandardDialog(
			"3dt Search", "No tiling found.", BStandardDialog.INFORMATION);
    
    private Action actionSearch() {
		if (_searchAction == null) {
			_searchAction = new AbstractJrAction("Search...") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
                    final String input = searchDialog.showInputDialog(null, null,
                            "");
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
			};
			_searchAction.setShortDescription("Search for a tiling by name");
			_searchAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_F, InputEvent.CTRL_MASK));
		}

		return _searchAction;
	}
    
    private AbstractJrAction _dualizeAction = null;
    
    private Action actionDualize() {
    	if (_dualizeAction == null) {
    		_dualizeAction = new AbstractJrAction("Dualize") {
    			private static final long serialVersionUID = 1L;
    			
    			@Override
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
    		};
        	_dualizeAction.setShortDescription("Dualize the current tiling");
    	}
    	
    	return _dualizeAction;
    }
    
    private AbstractJrAction _symmetrizeAction = null;
    
    private Action actionSymmetrize() {
    	if (_symmetrizeAction == null) {
    		_symmetrizeAction = new AbstractJrAction("Symmetrize") {
    			private static final long serialVersionUID = 1L;
    			
    			@Override
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
    		};
        	_symmetrizeAction.setShortDescription("symmetrize the current tiling");
    	}
    	
    	return _symmetrizeAction;
    }
    
    private AbstractJrAction _recolorAction = null;
    
    private Action actionRecolor() {
        if (_recolorAction == null) {
            _recolorAction = new AbstractJrAction("Recolor") {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    doc().randomlyRecolorTiles();
                    suspendRendering();
                    updateMaterials();
                    resumeRendering();
                }
            };
            _recolorAction
                    .setShortDescription("Pick new random colors for tiles");
            _recolorAction.setAcceleratorKey(KeyStroke.getKeyStroke(
                    KeyEvent.VK_C, InputEvent.CTRL_MASK));
        }
        return _recolorAction;
    }
    
    private AbstractJrAction _tileAddAction = null;
    
    private Action actionAddTile() {
    	if (_tileAddAction == null) {
    		_tileAddAction = new AbstractJrAction("Add Tile") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBodyNode == null) {
						return;
					}
					doc().addNeighbor(node2item.get(selectedBodyNode),
							selectedFace);
				}
    		};
    		_tileAddAction.setShortDescription("Add a tile at the picked face.");
    		_tileAddAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_A, 0));
    	}
    	return _tileAddAction;
    }
    
    private AbstractJrAction _tileRemoveAction = null;
    
    private Action actionRemoveTile() {
    	if (_tileRemoveAction == null) {
    		_tileRemoveAction = new AbstractJrAction("Remove Tile") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBodyNode == null) {
						return;
					}
					doc().remove(node2item.get(selectedBodyNode));
				}
    		};
    		_tileRemoveAction.setShortDescription("Remove the picked tile.");
    		_tileRemoveAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_D, 0));
    	}
    	return _tileRemoveAction;
    }
    
    private AbstractJrAction _tileKindRemoveAction = null;
    
    private Action actionRemoveTileClass() {
    	if (_tileKindRemoveAction == null) {
    		_tileKindRemoveAction = new AbstractJrAction("Remove Tile Class") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBodyNode == null) {
						return;
					}
					doc().removeKind(node2item.get(selectedBodyNode));
				}
    		};
    		_tileKindRemoveAction.setShortDescription(
    				"Remove the picked tile with all its symmetric images.");
    		_tileKindRemoveAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK));
    	}
    	return _tileKindRemoveAction;
    }
    
    private AbstractJrAction _tileClassRecolorAction = null;
    
    private Action actionRecolorTileClass() {
		if (_tileClassRecolorAction == null) {
			_tileClassRecolorAction = new AbstractJrAction("Recolor Tile Class") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBodyNode == null) {
						return;
					}
					final int kind =
						node2item.get(selectedBodyNode).getTile().getKind();
					final Color picked = 
						JColorChooser.showDialog(viewerApp.getFrame(),
								"Set Tile Color", doc().getTileColor(kind));
					if (picked == null) {
						return;
					}
					doc().setTileKindColor(kind, picked);
                    suspendRendering();
                    updateMaterials();
                    resumeRendering();
				}
			};
			final String txt = "Set the color for all tiles of one kind.";
			_tileClassRecolorAction.setShortDescription(txt);
    		_tileClassRecolorAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK));
		}
		return _tileClassRecolorAction;
	}
    
    private AbstractJrAction _tileRecolorAction = null;
    
    private Action actionRecolorTile() {
		if (_tileRecolorAction == null) {
			_tileRecolorAction = new AbstractJrAction(
					"Recolor Tile") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBodyNode == null) {
						return;
					}
					final DisplayList.Item item = node2item
							.get(selectedBodyNode);
					final Color picked = 
						JColorChooser.showDialog(viewerApp.getFrame(),
								"Set Tile Color", doc().color(item));
					if (picked == null) {
						return;
					}
                    suspendRendering();
					doc().recolor(item, picked);
                    resumeRendering();
				}
			};
			final String txt = "Set the color for this tile.";
			_tileRecolorAction.setShortDescription(txt);
    		_tileRecolorAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_C, 0));
		}
		return _tileRecolorAction;
	}
    
    private AbstractJrAction _tileUncolorAction = null;
    
    private Action actionUncolorTile() {
		if (_tileUncolorAction == null) {
			_tileUncolorAction = new AbstractJrAction(
					"Uncolor Tile") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBodyNode == null) {
						return;
					}
					final DisplayList.Item item = node2item
							.get(selectedBodyNode);
                    suspendRendering();
                    doc().recolor(item, null);
                    resumeRendering();
				}
			};
			final String txt = "Remove the individual color for this tile.";
			_tileUncolorAction.setShortDescription(txt);
    		_tileUncolorAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_U, 0));
		}
		return _tileUncolorAction;
	}
    
    private AbstractJrAction _encompassAction = null;
    
    private Action actionEncompass() {
        if (_encompassAction == null) {
            _encompassAction = new AbstractJrAction("Fit To Scene") {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    encompass();
                }
            };
            _encompassAction.setShortDescription(
            		"Adjust camera to fit scene to window");
            _encompassAction.setAcceleratorKey(KeyStroke.getKeyStroke(
                    KeyEvent.VK_E, 0));
        }
        return _encompassAction;
    }

    private AbstractJrAction _xViewAction = null;

	private Action actionXView() {
		if (_xViewAction == null) {
			_xViewAction = new AbstractJrAction("View along X") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					rotateScene(new Vector(1, 0, 0), new Vector(0, 1, 0));
					encompass();
				}
			};
			_xViewAction
					.setShortDescription("View the scene along the X axis.");
			_xViewAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_X, 0));
		}
		return _xViewAction;
	}

    private AbstractJrAction _yViewAction = null;

	private Action actionYView() {
		if (_yViewAction == null) {
			_yViewAction = new AbstractJrAction("View along Y") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					rotateScene(new Vector(0, 1, 0), new Vector(0, 0, 1));
					encompass();
				}
			};
			_yViewAction
					.setShortDescription("View the scene along the Y axis.");
			_yViewAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_Y, 0));
		}
		return _yViewAction;
	}

    private AbstractJrAction _zViewAction = null;

	private Action actionZView() {
		if (_zViewAction == null) {
			_zViewAction = new AbstractJrAction("View along Z") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					rotateScene(new Vector(0, 0, 1), new Vector(1, 0, 0));
					encompass();
				}
			};
			_zViewAction
					.setShortDescription("View the scene along the Z axis.");
			_zViewAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_Z, 0));
		}
		return _zViewAction;
	}
    
    private AbstractJrAction _111ViewAction = null;

	private Action action111View() {
		if (_111ViewAction == null) {
			_111ViewAction = new AbstractJrAction("View along 111") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					rotateScene(new Vector(1, 1, 1), new Vector(0, 0, 1));
					encompass();
				}
			};
			_111ViewAction
					.setShortDescription("View the scene along the 111 vector.");
			_111ViewAction.setAcceleratorKey(KeyStroke.getKeyStroke(
					KeyEvent.VK_D, 0));
		}
		return _111ViewAction;
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
//		} else if (name.length() > 37) {
//			name = name.substring(0, 37) + "...";
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
			SceneGraphUtility.removeChildren(this.tiling);
			ex.printStackTrace();
		}
		log("  " + getTimer(timer));

		// --- set camera and viewing transformation as specified in document
		updateCamera();
		if (doc.getTransformation() != null) {
			setViewingTransformation(doc.getTransformation());
		} else {
			rotateScene(new Vector(0,0,1), new Vector(1,0,0));
			encompass();
		}
		
		// --- update the info display
		final DSymbol ds = doc().getSymbol();
		setTInfo("size", ds.size());
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
    }
    
	public void handleDisplayListEvent(final Object event) {
		final DisplayList.Event e = (DisplayList.Event) event;
		//log(e);
		if (e.getEventType() == DisplayList.BEGIN) {
		} else if (e.getEventType() == DisplayList.END) {
		} else if (e.getEventType() == DisplayList.ADD) {
			addTile(e.getInstance());
		} else if (e.getEventType() == DisplayList.DELETE) {
			final DisplayList.Item item = e.getInstance();
			final SceneGraphNode node = item2node.get(item);
			item2node.remove(item);
			node2item.remove(node);
			SceneGraphUtility.removeChildNode(tiling, node);
		} else if (e.getEventType() == DisplayList.RECOLOR) {
			recolorTile(e.getInstance(), e.getNewColor());
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
        updateMaterials();
        updateDisplayProperties();
        suspendRendering();
        makeCopies();
        makeUnitCell();
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
        updateMaterials();
        updateDisplayProperties();
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
        
        this.unitCell.setGeometry(ilsf.getIndexedLineSet());
        updateUnitCellAppearance();
    }
    
    private void updateUnitCellAppearance() {
        final double r = getUnitCellEdgeWidth() / 2;
        final Appearance a = new Appearance();
        a.setAttribute(CommonAttributes.EDGE_DRAW, true);
        a.setAttribute(CommonAttributes.TUBE_RADIUS, r);
        a.setAttribute(CommonAttributes.VERTEX_DRAW, true);
        a.setAttribute(CommonAttributes.POINT_RADIUS, r);
        a.setAttribute(CommonAttributes.SPECULAR_COEFFICIENT, 0.0);
        a.setAttribute(CommonAttributes.DIFFUSE_COEFFICIENT, 1.0);
        a.setAttribute(CommonAttributes.AMBIENT_COEFFICIENT, 0.0);
        a.setAttribute(CommonAttributes.DIFFUSE_COLOR, getUnitCellColor());
        this.unitCell.setAppearance(a);
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
        for (final DisplayList.Item item: doc()) {
        	addTile(item);
        	if (doc().color(item) != null) {
        		recolorTile(item, doc().color(item));
        	}
        }
    }

    private void makeMaterials() {
    	if (doc() == null) {
    		return;
    	}

    	this.materials = new Appearance[doc().getTiles().size()];
		for (int i = 0; i < this.templates.length; ++i) {
			this.materials[i] = new Appearance();
			updateMaterial(this.materials[i], doc().getTileColor(i));
		}
	}
    
    private void updateMaterial(final Appearance a, final Color c) {
		a.setAttribute(CommonAttributes.DIFFUSE_COLOR, c);

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
        
    	for (int i = 0; i < this.templates.length; ++i) {
            updateMaterial(this.materials[i], doc().getTileColor(i));
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
    	
    	this.unitCell.setVisible(getShowUnitCell());
        updateUnitCellAppearance();
        log("      " + getTimer(timer));
    }
    
    private void makeCopies() {
    	if (doc().size() == 0) {
    		clearSceneGraph();
	        for (final Tile b: doc().getTiles()) {
	        	for (final Vector s: doc().centerIntoUnitCell(b)) {
		            doc().add(b, s);
	        	}
	        }
    	} else {
    		refreshScene();
    	}
    }

    private void encompass() {
    	encompass(this.viewerApp.getCurrentViewer(), this.scene);
    }
    
	public static void encompass(final Viewer viewer, final JrScene scene) {
		// --- extract parameters from viewer
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
		
		// --- adjust the avatar position to make scene fit
		final Matrix camMatrix = new Matrix();
		cameraPath.getInverseMatrix(camMatrix.getArray(), avatarPath
				.getLength());
		final SceneGraphComponent avatar = avatarPath.getLastComponent();
		final Matrix m = new Matrix(avatar.getTransformation());
		MatrixBuilder.init(m, signature).translate(c).translate(
				camMatrix.getColumn(3)).assignTo(avatar);
	}

	public void rotateScene(final Vector eye, final Vector up) {
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
	
    private void updateCamera() {
    	final Camera cam = CameraUtility.getCamera(this.viewerApp.getCurrentViewer());
    	boolean re_encompass = false;
    	if (getPerspective() != cam.isPerspective()) {
    		cam.setPerspective(getPerspective());
            re_encompass = true;
    	}
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
    	SceneGraphUtility.removeChildren(this.world);
    }
    
    private void resumeRendering() {
    	this.world.addChild(this.tiling);
    	this.world.addChild(this.unitCell);
    	this.viewerApp.getCurrentViewer().render();
    }

    private Transformation getViewingTransformation() {
		return this.scene.getPath("emptyPickPath").getLastComponent()
				.getTransformation();
    }
    
    private void setViewingTransformation(final Transformation trans) {
    	this.scene.getPath("emptyPickPath").getLastComponent()
				.setTransformation(trans);
    }
    
    private JPopupMenu _selectionPopup = null;
    
    private JPopupMenu selectionPopup() {
    	if (_selectionPopup == null) {
    		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    		_selectionPopup = new JPopupMenu("Actions");
    		_selectionPopup.setLightWeightPopupEnabled(false);
    		_selectionPopup.add(actionAddTile());
    		_selectionPopup.add(actionRemoveTile());
    		_selectionPopup.add(actionRemoveTileClass());
    		_selectionPopup.addSeparator();
    		_selectionPopup.add(actionRecolorTile());
    		_selectionPopup.add(actionUncolorTile());
    		_selectionPopup.add(actionRecolorTileClass());
    		
    		_selectionPopup.addPopupMenuListener(new PopupMenuListener() {
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
    	return _selectionPopup;
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
				selectedBodyNode = null;
				for (Iterator path = selection.iterator(); path.hasNext();) {
                    final SceneGraphNode node = (SceneGraphNode) path.next();
                    final String name = node.getName();
                    if (name.startsWith("body")) {
                        selectedBodyNode = node;
                    } else if (name.startsWith("face:")) {
                        selectedFace = Integer.parseInt(name.substring(5));
                    } else if (name.startsWith("outline:")) {
                        selectedFace = Integer.parseInt(name.substring(8));
                    }
                }
                try {
					if (tc.getAxisState(addSlot).isPressed()) {
						actionAddTile().actionPerformed(null);
					} else if (tc.getAxisState(removeSlot).isPressed()) {
						actionRemoveTile().actionPerformed(null);
					} else {
						final java.awt.Component comp = viewerApp.getContent();
						final java.awt.Point pos = comp.getMousePosition();
						Invoke.andWait(new Runnable() {
							public void run() {
								selectionPopup().show(comp, pos.x, pos.y);
							}
						});
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
				vSwitch.getCurrentViewer().renderAsync();
				break;
			}
		}
	}
    
    @SuppressWarnings("unused")
	private void restoreViewer() {
    	if (this.previousViewer >= 0) {
			final ViewerSwitch vSwitch = viewerApp.getViewerSwitch();
			vSwitch.selectViewer(this.previousViewer);
			vSwitch.getCurrentViewer().renderAsync();
			this.previousViewer = -1;
    	}
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
						updateMaterials();
						updateDisplayProperties();
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
            options.add(new OptionCheckBox("MacOS Context Menu Workaround", this,
            		"useLeopardWorkaround"));
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
            options.add(new OptionCheckBox("Perspective View", this,
					"perspective"));
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
		options.add(optionsDisplay(), "Display");
		options.add(optionsMaterial(), "Material");
		options.add(optionsEmbedding(), "Embedding");
        options.add(optionsCamera(), "Camera");
		return options;
    }
    
    private Widget tilingInfo() {
		final RowContainer info = new RowContainer();
		info.setBackground(null);
		info.setDefaultLayout(new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.BOTH,
				new Insets(10, 10, 10, 10), null));

		final String[][] items = new String[][] {
				{ "file", "File:" }, { "#", "#" }, { "name", "Name:" },
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
			this.controlsFrame = new BFrame("3dt Controls");
			this.controlsFrame.addEventLink(WindowClosingEvent.class, this,
					"hideControls");
			final BSplitPane content = new BSplitPane();
			content.setOrientation(BSplitPane.HORIZONTAL);
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
			content.add(scrollA, 0);
			content.add(scrollB, 1);
			this.controlsFrame.setContent(content);
			
			final JFrame jf = this.controlsFrame.getComponent();
			jf.setSize(500, 400);
			jf.validate();
			content.setDividerLocation(0.667);
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
			this.aboutFrame = new BFrame("About 3dt");
			this.aboutFrame.addEventLink(WindowClosingEvent.class, this,
					"hideAbout");
			final BLabel label = new BLabel("<html><center><h2>Gavrog 3dt</h2>"
					+ "<p>Version " + Version.full + "</p>"
					+ "<p>by Olaf Delgado-Friedrichs 1997-2007<p>"
					+ "</center></html>");
			final BOutline content = BOutline.createEmptyBorder(label, 20);
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

    public boolean getPerspective() {
        return perspective;
    }

    public void setPerspective(boolean perspective) {
    	if (perspective != this.perspective) {
    		dispatchEvent(new PropertyChangeEvent(this, "perspective",
    				this.perspective, perspective));
    		this.perspective = perspective;
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
		this.fieldOfView = fieldOfView;
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
}
