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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.gavrog.box.collections.Cache;
import org.gavrog.box.simple.NamedConstant;
import org.gavrog.box.simple.Tag;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Signature;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
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
    private GenericParser.Block data = null;
    
    // --- The tile kind colors set for this instance
    private Color[] palette = null;
    
    // --- embedding options
    private int equalEdgePriority = 3;
    private int embedderStepLimit = 10000;
    private boolean useBarycentricPositions = false;

    // --- saved user options
    private Properties  properties = null;
    
    // --- The last remembered viewing transformation
    private Transformation transformation = null;
    
    // --- random number generator
	private final static Random random = new Random();
	
    /**
     * Constructs a tiling instance.
     * @param name the name of this instance.
     * @param ds the Delaney symbol for the tiling.
     */
    public Document(final String name, final DSymbol ds) {
        if (ds.dim() == 2) {
            this.type = TILING_2D;
        } else if (ds.dim() == 3) {
            this.type = TILING_3D;
        } else {
        	final String msg = "only dimensions 2 and 3 supported";
            throw new UnsupportedOperationException(msg);
        }
        this.name = name;
        this.symbol = ds;
    }
    
    public Document(final GenericParser.Block block) {
    	if (! block.getType().equalsIgnoreCase("TILING")) {
    		throw new UnsupportedOperationException("only type TILING supported");
    	}
    	this.type = TILING_3D;
    	this.name = block.getEntriesAsString("name");
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
    
    public Tiling getTiling() {
        try {
            return (Tiling) cache.get(TILING);
        } catch (Cache.NotFoundException ex) {
            return (Tiling) cache.put(TILING, new Tiling(getSymbol()));
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
			final int dim = getSymbol().dim();
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
    
    public List<Vector> centerIntoUnitCell(final Tiling.Tile t) {
    	final int dim = getSymbol().dim();
    	final CoordinateChange toStd = getFinder().getToStd();
    	final CoordinateChange fromStd = getFinder().getFromStd();
    	final DSPair c = new DSPair(dim, t.getChamber());
    	final Point p0 = (Point) getPositions().get(c);
    	
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
    	if (this.palette == null) {
	    	final int n = getSymbol().numberOfOrbits(new IndexList(0, 1, 2));
	        this.palette = new Color[n];
	        fillPalette(this.palette);
    	}
    	return this.palette;
    }
    
    private void fillPalette(final Color[] palette) {
    	final int n = palette.length;
        final float offset = random.nextFloat();
        final float s = 0.6f;
        final float b = 1.0f;
        for (int i = 0; i < n; ++i) {
            final float h = (i / (float) n + offset) % 1.0f;
            palette[i] = Color.getHSBColor(h, s, b);
        }
    }
    
    public Color getTileKindColor(final int i) {
    	return getPalette()[i];
    }
    
    public Color getTileColor(final Tiling.Tile t) {
    	return getTileKindColor(t.getKind());
    }
    
    public Color getTileColor(final int i) {
    	return getTileColor(getTile(i));
    }
    
    public void setTileKindColor(final int i, final Color c) {
    	getPalette()[i] = c;
    }
    
    public void randomlyRecolorTiles() {
    	fillPalette(getPalette());
    }
    
    public String getSignature() {
        try {
            return (String) cache.get(SIGNATURE);
        } catch (Cache.NotFoundException ex) {
            final String sig = Signature.ofTiling(getTiling().getCover());
            return (String) cache.put(SIGNATURE, sig);
        }
    }
    
    public String getGroupName() {
    	try {
    		return (String) cache.get(SPACEGROUP);
    	} catch (Cache.NotFoundException ex) {
    		return (String) cache.put(SPACEGROUP, getFinder().getGroupName());
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
    
    public double[][] getUnitCellVectors() {
		final int dim = getSymbol().dim();
		final double result[][] = new double[dim][];
		for (int i = 0; i < dim; ++i) {
			final Vector v = (Vector) Vector.unit(dim, i).times(
					getCellToWorld());
			result[i] = v.getCoordinates().asDoubleArray()[0];
		}
		return result;
	}

	public double[] getOrigin() {
		final int dim = getSymbol().dim();
		final Point o = (Point) Point.origin(dim).times(getCellToWorld());
		return o.getCoordinates().asDoubleArray()[0];
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
		return this.properties;
	}

	public void setProperties(final Properties properties) {
		this.properties = properties;
	}

	public Transformation getTransformation() {
		return this.transformation;
	}

	public void setTransformation(final Transformation transformation) {
		this.transformation = transformation;
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
					result.add(new Document(data));
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
						result.add(new Document(name, ds));
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
					writer.startNode("tile");
					writer.addAttribute("templateNr",
							String.valueOf(item.getTile().getIndex()));
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
						dlist.add(new Object[] { number, shift, color });
					}
					reader.moveUp();
				}

				if (symbol != null) {
					doc = new Document(name, symbol);
					doc.setProperties(props);
					for (int i = 0; i < palette.size(); ++i) {
						doc.setTileKindColor(i, palette.get(i));
					}
					for (final Object val[]: dlist) {
						final int k = (Integer) val[0];
						final Tiling.Tile t = doc.getTile(k);
						final Vector s = (Vector) val[1];
						final Color c = (Color) val[2];
						doc.add(t, s);
						doc.recolor(t, s, c);
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
}