/*
   Copyright 2005 Olaf Delgado-Friedrichs

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

package org.gavrog.systre;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.Embedding;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.IGraph;
import org.gavrog.joss.pgraphs.basic.IGraphElement;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.basic.SpringEmbedder;
import org.gavrog.joss.pgraphs.io.NetParser;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.behaviors.keyboard.KeyNavigatorBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;
import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * Test for displaying periodic graphs, or nets, using Java3D. Constructs a
 * finite portion of the barycentrically embedded net from its representing
 * quotient graph and displays it in a Canvas3D object embedded in a Swing
 * JPanel.
 * 
 * Mouse behaviours are used to rotate and move the model.
 * 
 * Node and edges of the net can be cummulatively selected and deselected by
 * clicking on them. Selected elements are shown in yellow and the last change
 * is displayed symbolically.
 * 
 * @author Olaf Delgado
 * @version $Id: NetViewer.java,v 1.17 2006/02/16 22:53:19 odf Exp $
 */
public class NetViewer extends Applet {
    // --- color constants
    final static Color3f red = new Color3f(1, 0, 0);
    final static Color3f blue = new Color3f(0, 0, 1);
    final static Color3f yellow = new Color3f(1, 1, 0);
    final static Color3f black = new Color3f(0, 0, 0);
    final static Color3f white = new Color3f(1, 1, 1);
    
    // --- cursors
    final static Cursor defaultCursor = Cursor.getDefaultCursor();
    final static Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);

    // --- appearances for various elements in various modes
    final Appearance selectedAppearance = makeAppearance(yellow);
    final Appearance nodeAppearance = makeAppearance(red);
    final Appearance edgeAppearance = makeAppearance(blue);

    // --- the parent node of the net model in the scene graph
    final TransformGroup viewer;
    
    // --- contains the string specification for the current graph
    final JTextArea inputArea;
    
    // --- for status messages from the program
    final private JTextField status;
    
    // --- an archive of known structures
    private Archive rcsr = null;
    
    // --- the net to display
    private PeriodicGraph net;
    
    // --- embedder used to find node positions and metric
    private SpringEmbedder embedder;
    
    // --- the default radius for a new net
    private int defaultRadius = 2;
    
    // --- the current displayed radius
    private int radius = 3;
    
    // --- the currently displayed portion of the net
    private PeriodicGraph.EmbeddedPortion graph;
    
    // --- maps pickable shapes to elements of the net
    final Map objectToGraphAdress = new HashMap();
    
    // --- the currently selected elements of the net
    final Set selection = new HashSet();
    
    /**
     * Constructs an instance.
     * 
     * @param standalone if true, indicates that the class is not run as an applet
     */
    public NetViewer(final boolean standalone) {
        // --- create global bounds for the scene
        final Bounds sceneBounds = new BoundingSphere(new Point3d(0,0,0), 100);

        // --- set up a default simple universe
        final Canvas3D canvas3D = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        final SimpleUniverse simpleUniverse = new SimpleUniverse(canvas3D);
        simpleUniverse.getViewingPlatform().setNominalViewingTransform();
        simpleUniverse.getViewer().getView()
                .setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
        
        // --- create a top-level branch group
        final BranchGroup objRoot = new BranchGroup();
        
        // --- add a TransformGroup with some behaviours to hold the model
        viewer = makeViewer(sceneBounds);
        viewer.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        objRoot.addChild(viewer);
        
        // --- add a background
        final Background bg = new Background(1, 1, 1);
        bg.setApplicationBounds(sceneBounds);
        objRoot.addChild(bg);

        // --- add a light
        final Vector3f v = new Vector3f(-1, -1, -1);
        final DirectionalLight light = new DirectionalLight(true, white, v);
        light.setInfluencingBounds(sceneBounds);
        objRoot.addChild(light);

        // --- plug everything into the universe
        simpleUniverse.addBranchGraph(objRoot);

        // --- put our 3d scene into a Swing panel
        final JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BorderLayout());
        graphPanel.setOpaque(false);
        graphPanel.add(BorderLayout.CENTER, canvas3D);
        
        // --- add the panel to the main frame
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, graphPanel);
        
        status = new JTextField();
        status.setColumns(30);
        status.setEditable(false);
        add(BorderLayout.SOUTH, status);

        // --- add a text area for graph input
        final Box inputBox = Box.createVerticalBox();
        inputArea = new JTextArea();
        inputArea.setColumns(25);
        inputArea.setBackground(Color.LIGHT_GRAY);
        inputArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory
                .createEmptyBorder(10, 10, 10, 10), BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED), BorderFactory
                        .createEmptyBorder(5, 5, 5, 5))));
        inputBox.add(inputArea);
        final JButton updateButton = new JButton();
        updateButton.setAlignmentX(0.5f);
        updateButton.add(new JLabel("Update"));
        updateButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    changeNet(inputArea.getText(), defaultRadius);
                } catch (Exception ex) {
                    status.setText(String.valueOf(ex));
                }
            }
        });
        inputBox.add(updateButton);
        add(BorderLayout.EAST, inputBox);
        
        // --- make a box for buttons
        final Box buttonBox = Box.createVerticalBox();

        // --- add some buttons
        final JButton moreButton = new JButton();
        moreButton.add(new JLabel("More"));
        moreButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                setRadius(radius + 1);
            }
        });
        buttonBox.add(moreButton);
        final JButton lessButton = new JButton();
        lessButton.add(new JLabel("Less"));
        lessButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                setRadius(radius - 1);
            }
        });
        buttonBox.add(lessButton);
        
        final JButton relaxButton = new JButton();
        relaxButton.add(new JLabel("Relax"));
        relaxButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                setPositions(true);
                setRadius(radius);
            }
        });
        buttonBox.add(relaxButton);
        
        final JCheckBox orthographicChBox = new JCheckBox();
        orthographicChBox.setBackground(buttonBox.getBackground());
        orthographicChBox.setAlignmentX(0);
        orthographicChBox.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                if (orthographicChBox.isSelected()) {
                    simpleUniverse.getViewer().getView().setProjectionPolicy(
                            View.PARALLEL_PROJECTION);
                } else {
                    simpleUniverse.getViewer().getView().setProjectionPolicy(
                            View.PERSPECTIVE_PROJECTION);
                }
            }
        });
        buttonBox.add(orthographicChBox);
        buttonBox.add(new JLabel("orthographic"));
        
        final JButton loadButton = new JButton();
        loadButton.add(new JLabel("Load..."));
        loadButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                loadFile();
            }
        });
        buttonBox.add(loadButton);
        
        final JButton advanceButton = new JButton();
        advanceButton.add(new JLabel(">>"));
        advanceButton.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                nextInFile();
            }
        });
        buttonBox.add(advanceButton);
        
        // --- add the button panel to the main frame
        add(BorderLayout.WEST, buttonBox);
        
        // --- display the initial graph
        changeNet("dia", defaultRadius);

        // --- create an object to use for picking
        final PickCanvas pickCanvas = new PickCanvas(canvas3D, objRoot);
        pickCanvas.setMode(PickTool.GEOMETRY);
        pickCanvas.setTolerance(0);
        
        // --- enable a user to select and deselect displayed elements
        canvas3D.addMouseListener(new MouseListener() {
            
            // --- method invoked when a user clicks at the 3d scene
            public void mouseClicked(final MouseEvent me) {
                // --- collect the objects that have been clicked on
                // TODO avoid selecting objects too close to be visible - how?
                pickCanvas.setShapeLocation(me);
                final PickResult picks[] = pickCanvas.pickAllSorted();
                
                // --- don't waste time if nothing was
                if (picks == null) {
                    return;
                }

                // --- look for the topmost item of interest
                for (int i = 0; i < picks.length; ++i) {
                    // --- find the shape that was picked
                    final Shape3D object = (Shape3D) picks[i].getNode(PickResult.SHAPE3D);
                    
                    // --- if pick was not on a net element, proceed to next pick
                    if (!objectToGraphAdress.containsKey(object)) {
                        continue;
                    }
                    
                    // --- retrieve the corresponding element
                    final IGraphElement picked = (IGraphElement) objectToGraphAdress
                            .get(object);

                    // --- get the representation address of the net element
                    final IGraphElement rep = graph.getRepresentative(picked);
                    final Vector shift = graph.getShift(picked);
                    
                    // --- display the address in the status field
                    final StringBuffer buf = new StringBuffer(20);
                    if (selection.contains(picked)) {
                        buf.append("Deselected: ");
                    } else {
                        buf.append("Selected: ");
                    }
                    buf.append('(');
                    buf.append(rep);
                    buf.append(')');
                    if (!shift.equals(shift.zero())) {
                        buf.append(" + (");
                        for (int j = 0; j < shift.getDimension(); ++j) {
                            if (j > 0) {
                                buf.append(',');
                            }
                            buf.append(shift.get(j));
                        }
                        buf.append(')');
                    }
                    status.setText(buf.toString());
                    
                    // --- update the current selection and the element's appearance
                    if (selection.contains(picked)) {
                        // --- element was in the current selection, so remove it
                        selection.remove(picked);
                        
                        // --- change back to its default appearance
                        if (picked instanceof INode) {
                            object.setAppearance(nodeAppearance);
                        } else if (picked instanceof IEdge) {
                            object.setAppearance(edgeAppearance);
                        }
                    } else {
                        // --- add the element to the current selection
                        selection.add(picked);
                        // --- show that it is in the selection
                        object.setAppearance(selectedAppearance);
                    }
                    break;
                }
            }

            // --- other methods required by the MouseListener interface
            public void mouseEntered(final MouseEvent me) {
            }

            public void mouseExited(final MouseEvent me) {
            }

            public void mousePressed(final MouseEvent me) {
            }

            public void mouseReleased(final MouseEvent me) {
            }
        });        
    }
    
    /**
     * Constructs an instance for use as an applet.
     */
    public NetViewer() {
        this(false);
    }
    
    private void busy() {
        setCursor(waitCursor);
    }
    
    private void done() {
        setCursor(defaultCursor);
    }
    
    /**
     * Our applet destroy method removes the model from the scene. This is necessary
     * because some Java3D implementations refuse to change capatibilities in cached
     * geometries when some incarnations of them are considered live.
     */
    public void destroy() {
        viewer.removeChild(0);
    }
    
    private final  static DecimalFormat formatter = new DecimalFormat("0.000000");
    private final JFileChooser chooser = new JFileChooser();
    private NetParser parser = null;
    
    private static String format(final double x) {
        return formatter.format(x);
    }
    
    private void loadFile() {
        final int returnVal =chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            busy();
            final String filename = chooser.getSelectedFile().getName();
            final String dirname = chooser.getCurrentDirectory().getPath();
            final String path = new File(dirname, filename).getAbsolutePath();
            status.setText("Loading " + filename + "...");
            try {
                parser = new NetParser(new FileReader(path));
            } catch (FileNotFoundException ex) {
                status.setText("Error - Could not find file \"" + path + "\".");
                done();
                return;
            }
            nextInFile();
            done();
        }
    }
    
    private void nextInFile() {
        if (parser == null) {
            status.setText("No file open.");
            return;
        }
        busy();
        final PeriodicGraph G;
        try {
            G = parser.parseNet();
        } catch (Exception ex) {
            status.setText("Error - " + ex);
            done();
            return;
        }
        if (G == null) {
            status.setText("Reached end of file.");
            parser = null;
        } else {
            inputArea.setText(parser.getName());
            changeNet(G, this.defaultRadius);
        }
        done();
    }
    
    /**
     * Changes the net currently displayed.
     * 
     * @param spec new net or its string specification.
     * @param radius the radius of the portion to be displayed.
     */
    private void changeNet(final Object spec, final int radius) {
        busy();
        // --- parse the specification
        PeriodicGraph G;
        if (spec instanceof String) {
            try {
                G = NetParser.stringToNet((String) spec);
            } catch (Exception ex) {
                if (this.rcsr == null) {
                    final Package pkg = Archive.class.getPackage();
                    final String packagePath = pkg.getName().replaceAll("\\.", "/");
                    final String archivePath = packagePath + "/rcsr.arc";
                    this.rcsr = new Archive("1.0");
                    final InputStream inStream = ClassLoader
                            .getSystemResourceAsStream(archivePath);
                    final BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inStream));
                    this.rcsr.addAll(reader);
                }
                final Archive.Entry entry = this.rcsr.getByName(((String) spec).trim());
                if (entry == null) {
                    done();
                    throw new IllegalArgumentException("don't understand structure spec");
                } else {
                    G = PeriodicGraph.reconstructFromInvariantString(entry.getKey());
                }
            }
            // --- show the graph specification
            inputArea.setText((String) spec);
        } else {
            G = (PeriodicGraph) spec;
        }
        
        // --- save the net for later reference
        this.net = G;
        
        // --- determine graph positions
        setPositions(false);
        
        // --- set the radius
        setRadius(radius);
        done();
    }
     
    private void setPositions(final boolean relax) {
        busy();
        final PeriodicGraph G = this.net;
        
        // --- relax the atom configuration
        SpringEmbedder relaxer = new SpringEmbedder(G);
        boolean error = false;
        try {
            relaxer.setOptimizePositions(false);
            relaxer.steps(200);
            if (relax) {
                relaxer.setOptimizePositions(true);
                relaxer.steps(500);
            }
        } catch (Exception ex) {
            status.setText("Could not relax unit cell parameters");
            relaxer = new SpringEmbedder(G);
            if (relax) {
                relaxer.setOptimizeCell(false);
                relaxer.steps(200);
            }
        }
        relaxer.normalize();
        final double stats[] = relaxer.edgeStatistics();
        final double min = stats[0];
        final double max = stats[1];
        final double avg = stats[2];
        final Matrix gr = relaxer.getGramMatrix();
        final double det = ((Real) gr.determinant()).doubleValue();
        final double vol = Math.sqrt(det) / G.numberOfNodes();
        if (!error) {
            status.setText("Edge lengths: min = " + format(min) + ", max = "
                           + format(max) + ", avg = " + format(avg)
                           + ";  volume/vertex = " + format(vol));
        }
        
        // --- store embedder for later reference
        this.embedder = relaxer;
        done();
    }

    private void setRadius(final int radius) {
        busy();
        final SpringEmbedder relaxer = this.embedder;
        // --- construct an embedded portion of the net using the relaxed
        // configuration
        final Map pos = relaxer.getPositions();
        final Matrix A = LinearAlgebra.orthonormalRowBasis(relaxer.getGramMatrix());
        final CoordinateChange B = new CoordinateChange(A);
        
        final INode v0 = (INode) this.net.nodes().next();
        final Embedding E = this.net.embeddedNeighborhood(v0, radius, pos, B);
        
        // --- store it for later reference
        this.graph = (PeriodicGraph.EmbeddedPortion) E;
        
        // --- remove the old model - see method destroy() above
        viewer.removeChild(0);

        // --- construct a scene subgraph to represent that portion of the net
        final BranchGroup model = makeGraphModel(E);
        
        // --- make sure the model can be detached later
        model.setCapability(BranchGroup.ALLOW_DETACH);
        
        // --- insert it into the scene graph
        viewer.insertChild(model, 0);
        
        // --- clear the current selection of nodes and edges
        selection.clear();
        
        // --- save the current radius
        this.radius = radius;
        done();
    }
    
    /**
     * Creates an appearance instance with our default settings.
     * 
     * @param color the diffuse color to use.
     * @return the new appearance.
     */
    private Appearance makeAppearance(final Color3f color) {
        final Appearance appearance = new Appearance();
        appearance.setMaterial(new Material(black, black, color, white, 5f));

        final PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        pa.setBackFaceNormalFlip(true);
        appearance.setPolygonAttributes(pa);
        
        // the following does not seem to work correctly
//        final TransparencyAttributes ta = new TransparencyAttributes();
//        ta.setTransparencyMode(TransparencyAttributes.BLENDED);
//        ta.setTransparency(0.5f);
//        appearance.setTransparencyAttributes(ta);
        
        return appearance;
    }
    
    /**
     * Constructs a transform group with an empty slot for a model and some
     * behaviors attached that allow for moving the model around with the mouse
     * or the keyboard.
     * 
     * @param bounds the scene bounds to use for the behaviors.
     * @return the newly constructed transform group.
     */
    private TransformGroup makeViewer(final Bounds bounds) {
        // --- create a transform group
        final TransformGroup objTrans = new TransformGroup();
        
        // --- allow for changes of the attached model
        objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        // --- add a dummy model and make sure it can be removed
        final BranchGroup dummy = new BranchGroup();
        dummy.setCapability(BranchGroup.ALLOW_DETACH);
        objTrans.addChild(dummy);

        // --- add a behavior for rotating the model (mouse drag left)
        final MouseRotate mouseRotate = new MouseRotate(objTrans);
        mouseRotate.setSchedulingBounds(bounds);
        objTrans.addChild(mouseRotate);

        // --- add a behavior for translating the model (mouse drag right)
        final MouseTranslate mouseTranslate = new MouseTranslate(objTrans);
        mouseTranslate.setSchedulingBounds(bounds);
        objTrans.addChild(mouseTranslate);

        // --- add a behavior for zooming the model (mouse drag middle)
        final MouseZoom mouseZoom = new MouseZoom(objTrans);
        mouseZoom.setSchedulingBounds(bounds);
        objTrans.addChild(mouseZoom);

        // --- add a behaviour for moving the model around with the keyboard
        final KeyNavigatorBehavior knb = new KeyNavigatorBehavior(objTrans);
        knb.setSchedulingBounds(bounds);
        objTrans.addChild(knb);
        
        // --- return the new transform group
        return objTrans;
    }
    
    /**
     * Create a partial scene graph to display an embedded subject graph.
     * 
     * @param E the embedded graph to display.
     * @return the root of the constructed partial scene graph.
     */
    private BranchGroup makeGraphModel(final Embedding E) {
        // --- get the underlying graph for the given embedding
        final IGraph H = E.getGraph();
        
        // --- create the root node for the partial scene graph
        final BranchGroup obj = new BranchGroup();
        
        // --- add representations for the nodes of the subject graph
        for (final Iterator nodes = H.nodes(); nodes.hasNext();) {
            // --- retrieve the next node
            final INode v = (INode) nodes.next();
            
            // --- find its position
            final Point p = E.getPosition(v);
            final double x = ((Real) p.get(0)).doubleValue();
            final double y = ((Real) p.get(1)).doubleValue();
            final double z = ((Real) p.get(2)).doubleValue();
            
            // --- create a small sphere to represent the node
            final Sphere sphere = new Sphere(0.1f, nodeAppearance);
            
            // --- retrieve the underlying shape node
            final Shape3D body = sphere.getShape();
            
            // --- set its capabilitied for being picked
            setCapabilities(body);
            
            // --- associate it with the node it represents
            objectToGraphAdress.put(body, v);
            
            // --- create a transform group to shift the sphere to the node position
            final Transform3D t = new Transform3D();
            t.setTranslation(new Vector3d(x, y, z));
            final TransformGroup tg = new TransformGroup(t);
            
            // --- connect it with the new sphere and the model root
            tg.addChild(sphere);
            obj.addChild(tg);
        }
        
        // --- add representations for the edges of the subject graph
        for (final Iterator edges = H.edges(); edges.hasNext();) {
            // TODO continue code documentation from here
            final IEdge e = (IEdge) edges.next();
            final INode v = e.source();
            final INode w = e.target();
            final Point pp = E.getPosition(v);
            final Point qq = E.getPosition(w);
            final Vector3d p = new Vector3d(
                    ((Real) pp.get(0)).doubleValue(),
                    ((Real) pp.get(1)).doubleValue(),
                    ((Real) pp.get(2)).doubleValue()
            );

            final Vector dd = (Vector) qq.minus(pp);
            final Vector3d k = new Vector3d(
                    ((Real) dd.get(0)).doubleValue(),
                    ((Real) dd.get(1)).doubleValue(),
                    ((Real) dd.get(2)).doubleValue()
            );
            final Vector3d l = new Vector3d();
            l.cross(k, new Vector3d(0, 0, 1));
            if (l.length() < 1e-6) {
                l.cross(k, new Vector3d(0, 1, 0));
            }
            l.normalize();
            final Vector3d j = new Vector3d();
            j.cross(k, l);
            j.normalize();
            
            final Matrix3d mat = new Matrix3d();
            mat.setColumn(0, j);
            mat.setColumn(1, k);
            mat.setColumn(2, l);
            final Vector3d shift = new Vector3d(p.x + k.x/2, p.y + k.y/2, p.z + k.z/2);
            final Transform3D t = new Transform3D(mat, shift, 1);
            final TransformGroup tg = new TransformGroup(t);
            final Cylinder cylinder = new Cylinder(0.05f, 1.0f, edgeAppearance);

            final Shape3D body = cylinder.getShape(Cylinder.BODY);
            setCapabilities(body);
            setCapabilities(cylinder.getShape(Cylinder.TOP));
            setCapabilities(cylinder.getShape(Cylinder.BOTTOM));

            objectToGraphAdress.put(body, e);
            tg.addChild(cylinder);
            obj.addChild(tg);
        }
        
        return obj;
    }

    private void setCapabilities(final Shape3D shape) {
        PickTool.setCapabilities(shape, PickTool.INTERSECT_TEST);
        shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
    }
    
    /**
     * Main method to call when this class is used as an application.
     * 
     * @param args currently not used
     */
    public static void main(final String[] args) {
        final MainFrame frame = new MainFrame(new NetViewer(true), 800, 600);
        frame.setLocation(400, 200);

        // --- end process if main frame is closed
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
    }
}
