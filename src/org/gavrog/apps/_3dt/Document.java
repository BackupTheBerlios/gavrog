/**
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
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.gavrog.box.collections.Cache;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.NamedConstant;
import org.gavrog.box.simple.Tag;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Signature;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.embed.Embedder;
import org.gavrog.joss.pgraphs.io.GenericParser;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.tilings.FaceList;
import org.gavrog.joss.tilings.Tiling;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import de.jreality.scene.Transformation;

/**
 * @author Olaf Delgado
 * @version $Id: Document.java,v 1.44 2008/05/29 06:22:34 odf Exp $
 */
public class Document extends DisplayList {
    // --- the cache keys
	final protected static Object TILES = new Tag();
    final protected static Object CELL_TO_WORLD = new Tag();
    final protected static Object CELL_TO_EMBEDDER = new Tag();
    final protected static Object EMBEDDER = new Tag();
    final protected static Object EMBEDDER_OUTPUT = new Tag();
    final protected static Object FINDER = new Tag();
    final protected static Object SIGNATURE = new Tag();
    final protected static Object SPACEGROUP = new Tag();
    final protected static Object TILING = new Tag();
    final protected static Object WORLD_TO_CELL = new Tag();
    final protected static Object CENTERING_VECTORS = new Tag();
    
    // --- cache for this instance
    final protected Cache cache = new Cache();

    // --- possible document types
    final static public class Type extends NamedConstant {
        public Type(final String name) { super(name); }
    }
    final static public Object TILING_3D = new Type("3d Tiling");
    final static public Object TILING_2D = new Type("2d Tiling");
    
    // --- The type of this instance and its source data
    final private Object type;
    final private String name;
    private DSymbol symbol = null;
    private DSymbol effective_symbol = null;
    private GenericParser.Block data = null;
    
    // --- The tile and face colors set for this instance
    //TODO this should probably be moved to the DisplayList class.
    private Color[] tileClassColor = null;
    private Map<Tiling.Facet, Color> facetClassColor =
    	new HashMap<Tiling.Facet, Color>();
    private Set<Tiling.Facet> hiddenFacetClasses = new HashSet<Tiling.Facet>();
    
    // --- embedding options
    private int equalEdgePriority = 3;
    private int embedderStepLimit = 10000;
    private boolean useBarycentricPositions = false;

    // --- saved user options
    private Properties  properties = new Properties();
    
    // --- The last remembered viewing transformation
    private Transformation transformation = null;
    
    // --- random number generator
	private final static Random random = new Random();
	
	// --- convert a 2d symbol to 3d by extrusion
	private DSymbol extrusion(final DelaneySymbol ds) {
		if (ds.dim() != 2) {
			throw new UnsupportedOperationException("dimension must be 2");
		}
		final int s = ds.size();
		
		final DynamicDSymbol tmp = new DynamicDSymbol(3);
		final List elms_new = tmp.grow(s * 3);
		final List elms_old = Iterators.asList(ds.elements());
		
		for (int i = 0; i < ds.size(); ++i) {
			final Object Da = elms_new.get(i);
			final Object Db = elms_new.get(i + s);
			final Object Dc = elms_new.get(i + s + s);
			
			final Object D  = elms_old.get(i);
			final int i0 = elms_old.indexOf(ds.op(0, D));
			final int i1 = elms_old.indexOf(ds.op(1, D));
			final int i2 = elms_old.indexOf(ds.op(2, D));
			
			tmp.redefineOp(0, Da, elms_new.get(i0));
			tmp.redefineOp(1, Da, elms_new.get(i1));
			tmp.redefineOp(2, Da, Db);
			tmp.redefineOp(3, Da, Da);
			
			tmp.redefineOp(0, Db, elms_new.get(i0 + s));
			tmp.redefineOp(1, Db, Dc);
			tmp.redefineOp(2, Db, Da);
			tmp.redefineOp(3, Db, elms_new.get(i2 + s));
			
			tmp.redefineOp(0, Dc, Dc);
			tmp.redefineOp(1, Dc, Db);
			tmp.redefineOp(2, Dc, elms_new.get(i1 + s + s));
			tmp.redefineOp(3, Dc, elms_new.get(i2 + s + s));
		}
		
		for (int i = 0; i < ds.size(); ++i) {
			final Object Da = elms_new.get(i);
			final Object Db = elms_new.get(i + s);
			final Object Dc = elms_new.get(i + s + s);
			
			final Object D  = elms_old.get(i);
			tmp.redefineV(0, 1, Da, ds.v(0, 1, D));
			if (D.equals(ds.op(0, D))) {
				tmp.redefineV(0, 1, Db, 2);
			} else {
				tmp.redefineV(0, 1, Db, 1);
			}
			tmp.redefineV(1, 2, Da, 1);
			if (D.equals(ds.op(2, D))) {
				tmp.redefineV(2, 3, Da, 2);
			} else {
				tmp.redefineV(2, 3, Da, 1);
			}
			tmp.redefineV(2, 3, Dc, ds.v(1, 2, D));
		}
			
		return new DSymbol(tmp);
	}
	
    /**
     * Constructs a tiling instance.
     * @param ds the Delaney symbol for the tiling.
     * @param name the name of this instance.
     */
    public Document(final DSymbol ds, final String name) {
        if (ds.dim() == 2) {
        	this.symbol = ds;
            this.effective_symbol = extrusion(ds);
            this.type = TILING_2D;
        } else if (ds.dim() == 3) {
            this.symbol = ds;
            this.effective_symbol = ds;
            this.type = TILING_3D;
        } else {
        	final String msg = "only dimensions 2 and 3 supported";
            throw new UnsupportedOperationException(msg);
        }
        this.name = name;
    }
    
    public Document(final GenericParser.Block block, final String defaultName) {
    	if (! block.getType().equalsIgnoreCase("TILING")) {
    		throw new UnsupportedOperationException("only type TILING supported");
    	}
    	this.type = TILING_3D;
    	final String name = block.getEntriesAsString("name");
    	if (name == null || name.length() == 0) {
    		this.name = defaultName;
    	} else {
    		this.name = name;
    	}
    	this.data = block;
    }
    
    public void clearCache() {
        this.cache.clear();
    }

	public String getName() {
		return this.name;
	}
	
    public Object getType() {
        return this.type;
    }
    
    public DSymbol getSymbol() {
    	if (this.symbol == null) {
    		if (this.data != null) {
                this.symbol = new FaceList(this.data).getSymbol();
    		}
    	}
        return this.symbol;
    }
    
    private DSymbol getEffectiveSymbol() {
    	if (this.effective_symbol == null) {
    		if (this.data != null) {
                this.effective_symbol = getSymbol();
    		}
    	}
        return this.effective_symbol;
    }
    
    public Tiling getTiling() {
        try {
            return (Tiling) cache.get(TILING);
        } catch (Cache.NotFoundException ex) {
            return (Tiling) cache.put(TILING, new Tiling(getEffectiveSymbol()));
        }
    }

    public Tiling.Skeleton getNet() {
        return getTiling().getSkeleton();
    }
    
	@SuppressWarnings("unchecked")
	public List<Tiling.Tile> getTiles() {
		try {
			return (List) cache.get(TILES);
		} catch (Cache.NotFoundException ex) {
			return (List) cache.put(TILES, getTiling()
					.getTiles());
		}
	}
    
	public Tiling.Tile getTile(final int k) {
		return (Tiling.Tile) getTiles().get(k);
	}
    
    private SpaceGroupFinder getFinder() {
		try {
			return (SpaceGroupFinder) cache.get(FINDER);
		} catch (Cache.NotFoundException ex) {
			return (SpaceGroupFinder) cache.put(FINDER, new SpaceGroupFinder(
					getTiling().getSpaceGroup()));
		}
	}
    
    @SuppressWarnings("unchecked")
	private List<Vector> getCenteringVectors() {
		try {
			return (List<Vector>) cache.get(CENTERING_VECTORS);
		} catch (Cache.NotFoundException ex) {
			final int dim = getEffectiveSymbol().dim();
			final String name = getFinder().getExtendedGroupName();
			final CoordinateChange fromStd = getFinder().getFromStd();
			final List<Vector> result = new ArrayList<Vector>();
			for (final Operator op : (List<Operator>) SpaceGroupCatalogue
					.operators(dim, name)) {
				if (op.linearPart().isOne()) {
					result.add((Vector) op.translationalPart().times(fromStd));
				}
			}
			return (List<Vector>) cache.put(CENTERING_VECTORS, result);
		}
	}
    
    private Embedder getEmbedder() {
        try {
            return (Embedder) cache.get(EMBEDDER);
        } catch (Cache.NotFoundException ex) {
            return (Embedder) cache.put(EMBEDDER, new Embedder(getNet()));
        }
    }

    public void initializeEmbedder() {
        getEmbedder();
    }
    
    public void invalidateEmbedding() {
        cache.remove(EMBEDDER_OUTPUT);
    }
    
    private class EmbedderOutput {
        final private Map positions;
        final private CoordinateChange change;
        
        private EmbedderOutput(final Map pos, final CoordinateChange change) {
            this.positions = pos;
            this.change = change;
        }
    }
    
    private EmbedderOutput getEmbedderOutput() {
        try {
            return (EmbedderOutput) cache.get(EMBEDDER_OUTPUT);
        } catch (Cache.NotFoundException ex) {
            final Embedder embedder = getEmbedder();
            embedder.reset();
            embedder.setPasses(getEqualEdgePriority());
            if (embedder.getGraph().isStable() || getUseBarycentricPositions()) {
                embedder.setRelaxPositions(false);
                embedder.go(500);
            }
            if (!getUseBarycentricPositions()) {
                embedder.setRelaxPositions(true);
                embedder.go(getEmbedderStepLimit());
            }
            embedder.normalize();
            final CoordinateChange change = new CoordinateChange(LinearAlgebra
                    .orthonormalRowBasis(embedder.getGramMatrix()));
            final Map pos = getTiling().cornerPositions(embedder.getPositions());
            
            return (EmbedderOutput) cache.put(EMBEDDER_OUTPUT,
                    new EmbedderOutput(pos, change));
        }
    }
    
    private Map getPositions() {
        return getEmbedderOutput().positions;
    }
    
    public CoordinateChange getEmbedderToWorld() {
    	return getEmbedderOutput().change;
    }
    
    public double[] cornerPosition(final int i, final Object D) {
        final Point p0 = (Point) getPositions().get(new DSPair(i, D));
        final Point p = (Point) p0.times(getEmbedderToWorld());
        return p.getCoordinates().asDoubleArray()[0];
    }
    
    public Point nodePoint(final INode v) {
    	final Object D = getNet().chamberAtNode(v);
        final Point p = (Point) getPositions().get(new DSPair(0, D));
        return (Point) p.times(getEmbedderToWorld());
    }
    
    public Point edgeSourcePoint(final IEdge e) {
    	final Object D = getNet().chamberAtNode(e.source());
        final Point p = (Point) getPositions().get(new DSPair(0, D));
        return (Point) p.times(getEmbedderToWorld());
    }
    
    public Point edgeTargetPoint(final IEdge e) {
    	final Object D = getNet().chamberAtNode(e.target());
    	final Vector s = getNet().getShift(e);
        final Point q0 =
        	(Point) ((Point) getPositions().get(new DSPair(0, D))).plus(s);
        return (Point) q0.times(getEmbedderToWorld());
    }
    
    public List<Vector> centerIntoUnitCell(final Tiling.Tile t) {
    	final int dim = getEffectiveSymbol().dim();
    	final DSPair c = new DSPair(dim, t.getChamber());
    	return pointIntoUnitCell((Point) getPositions().get(c));
    }
    
    public List<Vector> centerIntoUnitCell(final IEdge e) {
    	final Tiling.Skeleton net = getNet();
    	final Object C = net.chamberAtNode(e.source());
    	final Object D = net.chamberAtNode(e.target());
    	final Vector s = net.getShift(e);
        final Point p = (Point) getPositions().get(new DSPair(0, C));
        final Point q =
        	(Point) ((Point) getPositions().get(new DSPair(0, D))).plus(s);
    	return pointIntoUnitCell(
    			(Point) p.plus(((Vector) q.minus(p)).times(0.5)));
    }
    
    public List<Vector> centerIntoUnitCell(final INode v) {
    	final Object D = getNet().chamberAtNode(v);
    	return pointIntoUnitCell((Point) getPositions().get(new DSPair(0, D)));
    }
    
    public List<Vector> pointIntoUnitCell(final Point p0) {
    	final int dim = p0.getDimension();
    	final CoordinateChange toStd = getFinder().getToStd();
    	final CoordinateChange fromStd = getFinder().getFromStd();
    	
    	final List<Vector> result = new ArrayList<Vector>();
    	for (final Vector s : getCenteringVectors()) {
			final Point p = (Point) p0.plus(s).times(toStd);
			final Real a[] = new Real[dim];
			for (int i = 0; i < dim; ++i) {
				a[i] = (Real) p.get(i).plus(0.001).mod(Whole.ONE);
			}
			final Vector v = (Vector) new Point(a).minus(p).times(fromStd);
			final Whole b[] = new Whole[dim];
			for (int i = 0; i < dim; ++i) {
				b[i] = (Whole) v.get(i).round();
			}
			result.add((Vector) new Vector(b).plus(s));
		}
    	return result;
    }
    
    private Color[] getPalette() {
    	if (this.tileClassColor == null) {
	    	int n = getEffectiveSymbol().numberOfOrbits(new IndexList(0, 1, 2));
	        this.tileClassColor = new Color[n];
	        fillPalette(this.tileClassColor);
    	}
    	return this.tileClassColor;
    }
    
    private void fillPalette(final Color[] palette) {
    	final int n = palette.length;
    	final int map[] = randomPermutation(n);
        final float offset = random.nextFloat();
        final float s = 0.6f;
        final float b = 1.0f;
        for (int i = 0; i < n; ++i) {
            final float h = (i / (float) n + offset) % 1.0f;
            palette[map[i]] = Color.getHSBColor(h, s, b);
        }
    }
    
    private int[] randomPermutation(final int n) {
    	final int result[] = new int[n];
    	final List<Integer> free = new ArrayList<Integer>();
    	for (int i = 0; i < n; ++i) {
    		free.add(i);
    	}
    	for (int i = 0; i < n; ++i) {
    		final int j = random.nextInt(n - i);
    		result[i] = free.remove(j);
    	}
    	return result;
    }
    
    public Color getTileClassColor(final int i) {
    	return getPalette()[i];
    }
    
    public Color getDefaultTileColor(final Tiling.Tile t) {
    	return getTileClassColor(t.getKind());
    }
    
    public Color getDefaultTileColor(final int i) {
    	return getDefaultTileColor(getTile(i));
    }
    
    public void setTileClassColor(final int i, final Color c) {
    	getPalette()[i] = c;
    }

    public Color getFacetClassColor(final Tiling.Facet f) {
    	return this.facetClassColor.get(f);
    }

    public void setFacetClassColor(final Tiling.Facet f, final Color c) {
    	this.facetClassColor.put(f, c);
    }

    public void removeFacetClassColor(final Tiling.Facet f) {
    	this.facetClassColor.remove(f);
    }
    
    public boolean isHiddenFacetClass(final Tiling.Facet f) {
    	return this.hiddenFacetClasses.contains(f);
    }
    
    public void hideFacetClass(final Tiling.Facet f) {
    	this.hiddenFacetClasses.add(f);
    }
    
    public void showFacetClass(final Tiling.Facet f) {
    	this.hiddenFacetClasses.remove(f);
    }
    
    public void randomlyRecolorTiles() {
    	fillPalette(getPalette());
    }
    
    public String getSignature() {
        try {
            return (String) cache.get(SIGNATURE);
        } catch (Cache.NotFoundException ex) {
        	final int dim = getSymbol().dim();
        	final String sig;
        	if (dim == 2) {
        		 sig = Signature.ofTiling(getSymbol());
        	} else {
        		sig = Signature.ofTiling(getTiling().getCover());
        	}
            return (String) cache.put(SIGNATURE, sig);
        }
    }
    
    public String getGroupName() {
    	try {
    		return (String) cache.get(SPACEGROUP);
    	} catch (Cache.NotFoundException ex) {
    		final int dim = getSymbol().dim();
    		final SpaceGroupFinder finder;
    		if (dim == 2) {
    			finder = new SpaceGroupFinder(new Tiling(getSymbol())
						.getSpaceGroup());
    		} else {
    			finder = getFinder();
    		}
    		return (String) cache.put(SPACEGROUP, finder.getGroupName());
    	}
    }
    
    public CoordinateChange getCellToWorld() {
		try {
			return (CoordinateChange) cache.get(CELL_TO_WORLD);
		} catch (Cache.NotFoundException ex) {
			return (CoordinateChange) cache.put(CELL_TO_WORLD, getFinder()
					.getToStd().inverse().times(getEmbedderToWorld()));
		}
	}
    
    public CoordinateChange getWorldToCell() {
    	try {
    		return (CoordinateChange) cache.get(WORLD_TO_CELL);
    	} catch (Cache.NotFoundException ex) {
    		return (CoordinateChange) cache.put(WORLD_TO_CELL, getCellToWorld()
					.inverse());
    	}
    }
    
    public CoordinateChange getCellToEmbedder() {
		try {
			return (CoordinateChange) cache.get(CELL_TO_EMBEDDER);
		} catch (Cache.NotFoundException ex) {
			return (CoordinateChange) cache.put(CELL_TO_EMBEDDER, getFinder()
					.getToStd().inverse());
		}
    }
    
    public double[][] getUnitCellVectors() {
    	final int dim = getEffectiveSymbol().dim();
		final double result[][] = new double[dim][];
		for (int i = 0; i < dim; ++i) {
			final Vector v = (Vector) Vector.unit(dim, i).times(
					getCellToWorld());
			result[i] = v.getCoordinates().asDoubleArray()[0];
		}
		return result;
	}

    public Vector[] getUnitCellVectorsInEmbedderCoordinates() {
    	final int dim = getEffectiveSymbol().dim();
		final Vector result[] = new Vector[dim];
		for (int i = 0; i < dim; ++i) {
			result[i] = (Vector) Vector.unit(dim, i).times(getCellToEmbedder());
		}
		return result;
	}

	public double[] getOrigin() {
		final int dim = getEffectiveSymbol().dim();
		final Point o = (Point) Point.origin(dim).times(getCellToWorld());
		return o.getCoordinates().asDoubleArray()[0];
	}
    
	private static void add(final StringBuffer buf, final String key,
			final Object val) {
    	final Class cl = val == null ? null : val.getClass();
		final boolean quote = !(cl == Integer.class || cl == Boolean.class);
    	buf.append(key);
    	buf.append(": ");
    	if (quote) buf.append('"');
    	buf.append(val);
    	if (quote) buf.append('"');
    	buf.append('\n');
    }
    
    public String info() {
    	final DSymbol ds = getSymbol();
    	final StringBuffer buf = new StringBuffer(500);
    	buf.append("---\n");
    	if (getName() == null) {
    		add(buf, "name", "unnamed");
    	} else {
    		add(buf, "name", getName().split("\\W+")[0]);
    	}
    	add(buf, "full_name", getName());
    	add(buf, "dsymbol", getSymbol().canonical().toString());
    	add(buf, "symbol_size", ds.size());
    	add(buf, "dimension", ds.dim());
    	add(buf, "transitivity", getTransitivity());
    	add(buf, "minimal", ds.isMinimal());
		add(buf, "self_dual", ds.equals(ds.dual()));
		add(buf, "signature", getSignature());
		add(buf, "spacegroup", getGroupName());
    	return buf.toString();
    }
    
	public String getTransitivity() {
		final StringBuffer buf = new StringBuffer(10);
		final DelaneySymbol ds = getSymbol();
		for (int i = 0; i <= ds.dim(); ++i) {
			buf.append(showNumber(ds.numberOfOrbits(IndexList.except(ds, i))));
		}
		return buf.toString();
	}

	private static String showNumber(final int n) {
		if (n >= 0 && n < 10) {
			return String.valueOf(n);
		} else {
			return "(" + n + ")";
		}
	}

	public static List<Document> load(final String path)
			throws FileNotFoundException {
		final String ext = path.substring(path.lastIndexOf('.') + 1)
				.toLowerCase();
		return load(new FileReader(path), ext);
	}

	public static List<Document> load(final Reader input, final String ext) {
		final BufferedReader reader = new BufferedReader(input);
		final List<Document> result = new ArrayList<Document>();

		if (ext.equals("cgd")) {
			final GenericParser parser = new NetParser(reader);
			int discarded = 0;
			while (!parser.atEnd()) {
				final GenericParser.Block data = parser.parseDataBlock();
				if (data.getType().equalsIgnoreCase("TILING")) {
					result.add(new Document(data, "#" + (result.size() + 1)));
				} else {
					++discarded;
				}
			}
		} else if (ext.equals("ds")){
			final StringBuffer buffer = new StringBuffer(200);
			String name = null;
			while (true) {
				String line;
				try {
					line = reader.readLine();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				if (line.charAt(0) == '#') {
					if (line.charAt(1) == '@') {
						line = line.substring(2).trim();
						if (line.startsWith("name ")) {
							name = line.substring(5);
						}
					}
				} else {
					int i = line.indexOf('#');
					if (i >= 0) {
						line = line.substring(0, i);
					}
					buffer.append(' ');
					buffer.append(line);
					if (buffer.toString().trim().endsWith(">")) {
						final DSymbol ds = new DSymbol(buffer.toString().trim());
						buffer.delete(0, buffer.length());
						if (name == null) {
							name = "#" + (result.size() + 1);
						}
						result.add(new Document(ds, name));
						name = null;
					}
				}
			}
		} else if (ext.equals("gsl")) {
			try {
				final ObjectInputStream ostream = getXStream()
						.createObjectInputStream(reader);
				while (true) {
					final Document doc = (Document) ostream.readObject();
					if (doc != null) {
						result.add(doc);
					}
				}
			} catch (EOFException ex) {
				; // End of stream reached
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		return result;
	}
	
	private static XStream getXStream() {
    	XStream xstream = new XStream();
    	xstream.setMode(XStream.NO_REFERENCES);

    	xstream.alias("scene", Document.class);
    	xstream.alias("color", Color.class);
    	
    	xstream.registerConverter(new SingleValueConverter() {
			public boolean canConvert(final Class clazz) {
				return clazz == DSymbol.class;
			}
			public String toString(final Object obj) {
				final String code = obj.toString();
				return code.substring(5, code.length() - 1);
			}
			public Object fromString(final String spec) {
				return new DSymbol(spec);
			}
    	});
    	
    	xstream.registerConverter(new Converter() {
			public boolean canConvert(final Class clazz) {
				return clazz == Color.class;
			}
			public void marshal(final Object value,
					final HierarchicalStreamWriter writer,
					final MarshallingContext context) {
				final Color c = (Color) value;
				writer.addAttribute("red", String.valueOf(c.getRed()));
				writer.addAttribute("green", String.valueOf(c.getGreen()));
				writer.addAttribute("blue", String.valueOf(c.getBlue()));
			}
			public Object unmarshal(final HierarchicalStreamReader reader,
					final UnmarshallingContext context) {
				final int red = Integer.parseInt(reader.getAttribute("red"));
				final int green = Integer.parseInt(reader.getAttribute("green"));
				final int blue = Integer.parseInt(reader.getAttribute("blue"));
				
				return new Color(red, green, blue);
			}
    	});
    	
    	xstream.registerConverter(new SingleValueConverter() {
			public boolean canConvert(final Class clazz) {
				return clazz == Vector.class;
			}
			public String toString(final Object value) {
				final Vector v = (Vector) value;
				final StringBuffer buf = new StringBuffer(12);
				for (int i = 0; i < v.getDimension(); ++i) {
					if (i > 0) {
						buf.append(' ');
					}
					buf.append(v.get(i).toString());
				}
				return buf.toString();
			}
			public Object fromString(final String spec) {
				final String fields[] = spec.trim().split("\\s+");
				
				final int a[] = new int[fields.length];
				for (int i = 0; i < fields.length; ++i) {
					a[i] = Integer.parseInt(fields[i]);
				}
				return new Vector(a);
			}
    	});
    	
    	xstream.registerConverter(new SingleValueConverter() {
			public boolean canConvert(final Class clazz) {
				return clazz == Transformation.class;
			}
			public String toString(final Object value) {
				final double v[] = ((Transformation) value).getMatrix();
				final StringBuffer buf = new StringBuffer(40);
				for (int i = 0; i < v.length; ++i) {
					if (i > 0) {
						buf.append(' ');
					}
					buf.append(v[i]);
				}
				return buf.toString();
			}
			public Object fromString(final String spec) {
				final String fields[] = spec.trim().split("\\s+");
				
				final double v[] = new double[fields.length];
				for (int i = 0; i < fields.length; ++i) {
					v[i] = Double.parseDouble(fields[i]);
				}
				return new Transformation(v);
			}
    	});
    	
    	xstream.registerConverter(new Converter() {
			public boolean canConvert(final Class clazz) {
				return clazz == Properties.class;
			}
			public void marshal(final Object value,
					final HierarchicalStreamWriter writer,
					final MarshallingContext context) {
				final Properties props = (Properties) value;
				for (final Object key: props.keySet()) {
					writer.startNode("property");
					writer.addAttribute("key", (String) key);
					writer.setValue((String) props.getProperty((String) key));
					writer.endNode();
				}
			}
			public Object unmarshal(final HierarchicalStreamReader reader,
					final UnmarshallingContext context) {
				final Properties props = new Properties();
				while (reader.hasMoreChildren()) {
					reader.moveDown();
					final String key = reader.getAttribute("key");
					final String val = reader.getValue();
					props.setProperty(key, val);
					reader.moveUp();
				}
				return props;
			}
    	});
    	
    	xstream.registerConverter(new Converter() {
			public boolean canConvert(final Class clazz) {
				return clazz == Document.class;
			}
			public void marshal(final Object value,
					final HierarchicalStreamWriter writer,
					final MarshallingContext context) {
				final Document doc = (Document) value;
				final Tiling til = doc.getTiling();
				final DSCover cov = til.getCover();
				
				if (doc.getName() != null) {
					writer.addAttribute("name", doc.getName());
				}
				writer.startNode("symbol");
				context.convertAnother(til.getSymbol().flat());
				writer.endNode();
				writer.startNode("cover");
				context.convertAnother(til.getCover().flat());
				writer.endNode();
				for (final Iterator idcs = cov.indices(); idcs.hasNext();) {
					final int i = (Integer) idcs.next();
					for (final Iterator elms = cov.elements(); elms.hasNext();) {
						final Object D = (Integer) elms.next();
						final Vector s = til.edgeTranslation(i, D);
						if (!s.isZero()) {
							writer.startNode("edgeShift");
							writer.addAttribute("index", String.valueOf(i));
							writer.addAttribute("element", String.valueOf(D));
							context.convertAnother(s);
							writer.endNode();
						}
					}
				}
				
				writer.startNode("palette");
				context.convertAnother(doc.getPalette());
				writer.endNode();
				for (final DisplayList.Item item: doc) {
					if (item.isTile()) {
						writer.startNode("tile");
						writer.addAttribute("templateNr",
								String.valueOf(item.getTile().getIndex()));
					} else if (item.isNode()) {
						writer.startNode("node");
						writer.addAttribute("id",
								String.valueOf(item.getNode().id()));
					} else if (item.isEdge()) {
						final IEdge e = item.getEdge();
						writer.startNode("edge");
						writer.addAttribute("source",
								String.valueOf(e.source().id()));
						writer.addAttribute("target",
								String.valueOf(e.target().id()));
						writer.startNode("label");
						context.convertAnother(((PeriodicGraph) e.owner())
								.getShift(e));
						writer.endNode();
					}
					writer.startNode("shift");
					context.convertAnother(item.getShift());
					writer.endNode();
					if (doc.color(item) != null) {
						writer.startNode("color");
						context.convertAnother(doc.color(item));
						writer.endNode();
					}
					writer.endNode();
				}
				for (final Tiling.Facet f: doc.facetClassColor.keySet()) {
					final Color c = doc.facetClassColor.get(f);
					if (c != null) {
						writer.startNode("facet");
						writer.addAttribute("templateNr",
								String.valueOf(f.getTile()));
						writer.addAttribute("index",
								String.valueOf(f.getIndex()));
						writer.startNode("color");
						context.convertAnother(c);
						writer.endNode();
						writer.endNode();
					}
				}
				for (final Tiling.Facet f: doc.hiddenFacetClasses) {
					writer.startNode("facet");
					writer.addAttribute("templateNr",
							String.valueOf(f.getTile()));
					writer.addAttribute("index", String.valueOf(f.getIndex()));
					writer.addAttribute("hidden", "true");
					writer.endNode();
				}
				writer.startNode("options");
				context.convertAnother(doc.getProperties());
				writer.endNode();
				writer.startNode("transformation");
				context.convertAnother(doc.getTransformation());
				writer.endNode();
			}
			public Object unmarshal(final HierarchicalStreamReader reader,
					final UnmarshallingContext context) {
				Document doc = null;
				final String name = reader.getAttribute("name");
				DSymbol symbol = null;
				final List<Color> palette = new LinkedList<Color>();
				final List<Object[]> dlist = new LinkedList<Object[]>();
				final Map<Pair, Color> fcolors = new HashMap<Pair, Color>();
				final Set<Pair> fhidden = new HashSet<Pair>();
				Properties props = null;
				Transformation trans = null;
				
				while (reader.hasMoreChildren()) {
					reader.moveDown();
					if ("symbol".equals(reader.getNodeName())) {
						symbol = (DSymbol) context.convertAnother(null,
								DSymbol.class);
					} else if ("palette".equals(reader.getNodeName())) {
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							palette.add((Color) context.convertAnother(null,
									Color.class));
							reader.moveUp();
						}
					} else if ("options".equals(reader.getNodeName())) {
						props = (Properties) context.convertAnother(null,
								Properties.class);
					} else if ("transformation".equals(reader.getNodeName())) {
						trans = (Transformation) context.convertAnother(null,
								Transformation.class);
					} else if ("tile".equals(reader.getNodeName())) {
						final Integer number = new Integer(reader
								.getAttribute("templateNr"));
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("shift".equals(reader.getNodeName())) {
								shift = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						dlist.add(new Object[] { "tile", shift, color, number });
					} else if ("node".equals(reader.getNodeName())) {
						final Long number = new Long(reader.getAttribute("id"));
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("shift".equals(reader.getNodeName())) {
								shift = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						dlist.add(new Object[] { "node", shift, color, number });
					} else if ("edge".equals(reader.getNodeName())) {
						final Long source =
							new Long(reader.getAttribute("source"));
						final Long target =
							new Long(reader.getAttribute("target"));
						Vector label = null;
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("label".equals(reader.getNodeName())) {
								label = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("shift".equals(reader.getNodeName())) {
									shift = (Vector) context.convertAnother(null,
											Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						dlist.add(new Object[] { "edge", shift, color,
								source, target, label });
					} else if ("facet".equals(reader.getNodeName())) {
						final int tile =
							new Integer(reader.getAttribute("templateNr"));
						final int index =
							new Integer(reader.getAttribute("index"));
						final String hidden = reader.getAttribute("hidden");
						if ("true".equalsIgnoreCase(hidden)) {
							fhidden.add(new Pair(tile, index));
						}
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						fcolors.put(new Pair(tile, index), color);
					}
					reader.moveUp();
				}

				if (symbol != null) {
					doc = new Document(symbol, name);
					doc.setProperties(props);
					for (int i = 0; i < palette.size(); ++i) {
						doc.setTileClassColor(i, palette.get(i));
					}
					for (final Object val[]: dlist) {
						final String kind = (String) val[0];
						final Vector s = (Vector) val[1];
						final Color c = (Color) val[2];
						final Item item;
						if (kind.equals("tile")) {
							final int id = (Integer) val[3];
							final Tiling.Tile t = doc.getTile(id);
							item = doc.add(t, s);
						} else if (kind.equals("node")) {
							INode v = (INode) doc.getNet().getElement(val[3]);
							item = doc.add(v, s);
						} else if (kind.equals("edge")) {
							final Tiling.Skeleton net = doc.getNet();
							final INode v = (INode) net.getElement(val[3]);
							final INode w = (INode) net.getElement(val[4]);
							final Vector t = (Vector) val[5];
							final IEdge e = (IEdge) net.getEdge(v, w, t);
							item = doc.add(e, s);
						} else {
							item = null;
						}
						if (item != null) {
							doc.recolor(item, c);
						}
					}
					for (final Pair item: fcolors.keySet()) {
						final Color c = fcolors.get(item);
						if (c != null) {
							final int tile = (Integer) item.getFirst();
							final int index = (Integer) item.getSecond();
							final Tiling.Tile t = doc.getTiles().get(tile);
							final Tiling.Facet f = t.facet(index);
							doc.setFacetClassColor(f, c);
						}
					}
					for (final Pair item: fhidden) {
						final int tile = (Integer) item.getFirst();
						final int index = (Integer) item.getSecond();
						final Tiling.Tile t = doc.getTiles().get(tile);
						final Tiling.Facet f = t.facet(index);
						doc.hideFacetClass(f);
					}
					doc.setTransformation(trans);
				}
				
				return doc;
			}
    	});
    	
    	return xstream;
	}
	
	public String toXML() {
		//TODO hack!
		return "<object-stream>\n" + getXStream().toXML(this)
				+ "\n</object-stream>\n";
	}
	
	public static void main(final String args[]) {
		final String path = args[0];
		try {
			final List<Document> syms = load(path);
			for (final Document doc: syms) {
            	if (doc.getName() != null) {
            		System.out.println("#@ name " + doc.getName());
            	}
				System.out.println(doc.getSymbol().canonical());
			}
		} catch (final FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}
	
    // --- getters and setters for options
    public int getEmbedderStepLimit() {
        return this.embedderStepLimit;
    }

    public void setEmbedderStepLimit(int embedderStepLimit) {
        if (embedderStepLimit != this.embedderStepLimit) {
            invalidateEmbedding();
            this.embedderStepLimit = embedderStepLimit;
        }
    }

    public int getEqualEdgePriority() {
        return this.equalEdgePriority;
    }

    public void setEqualEdgePriority(int equalEdgePriority) {
        if (equalEdgePriority != this.equalEdgePriority) {
            invalidateEmbedding();
            this.equalEdgePriority = equalEdgePriority;
        }
    }

    public boolean getUseBarycentricPositions() {
        return this.useBarycentricPositions;
    }

    public void setUseBarycentricPositions(boolean useBarycentricPositions) {
        if (useBarycentricPositions != this.useBarycentricPositions) {
            invalidateEmbedding();
            this.useBarycentricPositions = useBarycentricPositions;
        }
    }

    public Properties getProperties() {
    	return (Properties) this.properties.clone();
	}

	public void setProperties(final Properties properties) {
		this.properties.clear();
		this.properties.putAll(properties);
	}

	public Transformation getTransformation() {
		return this.transformation;
	}

	public void setTransformation(final Transformation transformation) {
		this.transformation = transformation;
	}
}
