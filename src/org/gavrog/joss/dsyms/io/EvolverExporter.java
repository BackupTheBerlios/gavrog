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


package org.gavrog.joss.dsyms.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.embed.Embedder;
import org.gavrog.joss.tilings.Tiling;

/**
 * @author Olaf Delgado
 * @version $Id: EvolverExporter.java,v 1.1 2008/05/14 02:50:32 odf Exp $
 */
public class EvolverExporter {
	final private Tiling til;
	final private Tiling.Skeleton net;
	final private Embedder embedder;
	final private CoordinateChange embedderToWorld;
	final private Map pos;
	
	public EvolverExporter(final Tiling til) {
		this.til = til;
		this.net = til.getSkeleton();
		
		// --- compute an embedding
		this.embedder = new Embedder(this.net);
        this.embedder.reset();
        this.embedder.setPasses(1);
        if (this.net.isStable()) {
            this.embedder.setRelaxPositions(false);
            this.embedder.go(500);
        }
        this.embedder.setRelaxPositions(true);
        this.embedder.go(100000);
        this.embedder.normalize();
        this.embedderToWorld = new CoordinateChange(LinearAlgebra
				.orthonormalRowBasis(this.embedder.getGramMatrix()));
        this.pos = this.til.cornerPositions(this.embedder.getPositions());
        
        // --- compute an appropriate unit cell
		final int dim = til.getSymbol().dim();
		final double basis[][] = new double[dim][];
		for (int i = 0; i < dim; ++i) {
			final Vector v = (Vector) Vector.unit(dim, i).times(
					this.embedderToWorld);
			basis[i] = v.getCoordinates().asDoubleArray()[0];
		}
	}
	
	private double volume(final double c[][]) {
		return    c[0][0] * c[1][1] * c[2][2] - c[0][2] * c[1][1] * c[2][0]
				+ c[0][1] * c[1][2] * c[2][0] - c[0][0] * c[1][2] * c[2][1]
				+ c[0][2] * c[1][0] * c[2][1] - c[0][1] * c[1][0] * c[2][2];
	}
	
    public double[] cornerPosition(final int i, final Object D) {
        final Point p0 = (Point) this.pos.get(new DSPair(i, D));
        final Point p = (Point) p0.times(this.embedderToWorld);
        return p.getCoordinates().asDoubleArray()[0];
    }
    
	//TODO need some reduced lattice here instead
    private double[][] getPrimitiveCellVectors() {
		final int dim = til.getSymbol().dim();
		final double result[][] = new double[dim][];
		for (int i = 0; i < dim; ++i) {
			final Vector v = (Vector) Vector.unit(dim, i).times(
					this.embedderToWorld);
			result[i] = v.getCoordinates().asDoubleArray()[0];
		}
		return result;
	}

	public void writeTo(final Writer writer) throws IOException {
		final BufferedWriter outf = new BufferedWriter(writer);
		final double cell[][] = getPrimitiveCellVectors();
	    final List tiles = this.til.getTiles();
	    final double vol = volume(cell) / tiles.size();
		final double scale;
		if (vol > 0) {
			scale = Math.pow(vol, -1.0/3.0);
		} else {
			scale = Math.pow(-vol, -1.0/3.0);
		}
		
		// --- write the initial unit cell vectors
	    outf.write("parameter p1x = " + cell[0][0] * scale + '\n');
	    outf.write("parameter p1y = " + cell[0][1] * scale + '\n');
	    outf.write("parameter p1z = " + cell[0][2] * scale + '\n');
	    outf.write("parameter p2x = " + cell[1][0] * scale + '\n');
	    outf.write("parameter p2y = " + cell[1][1] * scale + '\n');
	    outf.write("parameter p2z = " + cell[1][2] * scale + '\n');
	    outf.write("parameter p3x = " + cell[2][0] * scale + '\n');
	    outf.write("parameter p3y = " + cell[2][1] * scale + '\n');
	    outf.write("parameter p3z = " + cell[2][2] * scale + '\n');
	    outf.write('\n');
	    outf.write("periods\n");
	    outf.write("p1x p1y p1z\n");
	    outf.write("p2x p2y p2z\n");
	    outf.write("p3x p3y p3z\n");
	    outf.write('\n');
	    
	    // --- include an optional header file
	    outf.write("#include \"foam.eh\"\n\n");
	    
	    // --- write the vertices
	    outf.write("vertices\n");
	    final List nodes = new ArrayList();
	    nodes.add(null);
	    Iterators.addAll(nodes, this.net.nodes());
	    final Map nodeNumbers = new HashMap();
	    int i = 0;
	    for (final Iterator iter = nodes.iterator(); iter.hasNext();) {
	    	final INode v = (INode) iter.next();
	    	if (v == null) {
	    		continue;
	    	}
	    	nodeNumbers.put(v, new Integer(++i));
	    	final Object D = this.net.chamberAtNode(v);
	    	final double p[] = cornerPosition(0, D);
	    	outf.write(i + "  ");
	    	for (int k = 0; k < 3; ++k) {
	    		outf.write(" " + p[k] * scale);
	    	}
	    	outf.write('\n');
	    }
	    outf.write('\n');
	    
	    // --- write the edges
	    outf.write("edges\n");
	    final List edges = new ArrayList();
	    edges.add(null);
	    Iterators.addAll(edges, this.net.edges());
	    final Map edgeNumbers = new HashMap();
	    i = 0;
	    for (final Iterator iter = edges.iterator(); iter.hasNext();) {
	    	final IEdge e = (IEdge) iter.next();
	    	if (e == null) {
	    		continue;
	    	}
	    	edgeNumbers.put(e, new Integer(++i));
	    	final int v = ((Integer) nodeNumbers.get(e.source())).intValue();
	    	final int w = ((Integer) nodeNumbers.get(e.target())).intValue();
	    	outf.write(i + "  " + v + ' ' + w + ' ');
	    	final Vector s = this.net.getShift(e);
	    	for (int k = 0; k < 3; ++k) {
	    		final IArithmetic x = s.get(k);
	    		if (x.isZero()) {
	    			outf.write(" *");
	    		} else if (x.isOne()) {
	    			outf.write(" +");
	    		} else if (x.negative().isOne()) {
	    			outf.write(" -");
	    		} else {
	    			throw new RuntimeException("Illegal shift vector " + s);
	    		}
	    	}
	    	outf.write('\n');
	    }
	    outf.write('\n');
	    
	    // --- write the faces
	    outf.write("faces\n");
	    final DSymbol cover = this.til.getCover();
	    final Map ch2faceNr = new HashMap();
	    final Iterator iterF = cover.orbitReps(new IndexList(0, 1, 3));
	    i = 0;
	    while (iterF.hasNext()) {
	    	final Object D0 = iterF.next();
	    	outf.write(++i + " ");
	    	Object D = D0;
	    	while (true) {
	    		final IEdge e = this.net.edgeForChamber(D);
	    		final int k = ((Integer) edgeNumbers.get(e)).intValue();
	    		if (e.oriented().equals(((IEdge) edges.get(k)).oriented())) {
		    		outf.write(" " + k);
	    		} else {
		    		outf.write(" " + (-k));
	    		}
	    		ch2faceNr.put(D, new Integer(i));
	    		ch2faceNr.put(cover.op(3, D), new Integer(-i));
	    		D = cover.op(0, D);
	    		ch2faceNr.put(D, new Integer(i));
	    		ch2faceNr.put(cover.op(3, D), new Integer(-i));
	    		D = cover.op(1, D);
	    		if (D0.equals(D)) {
	    			break;
	    		}
	    	}
		    outf.write('\n');
	    }
	    outf.write('\n');
	    
	    // --- write the bodies
	    outf.write("bodies\n");
	    i = 0;
	    for (Iterator iter = this.til.getTiles().iterator(); iter.hasNext();) {
	    	final Tiling.Tile t = (Tiling.Tile) iter.next();
	    	outf.write(++i + " ");
	    	for (int k = 0; k < t.size(); ++k) {
	    		outf.write(" " + ch2faceNr.get(t.facet(k).getChamber()));
	    	}
	    	outf.write("   volume 1.0\n");
	    }
	    outf.write('\n');
	    
	    // --- include an optional tail file
	    outf.write("#include \"foam.et\"\n");
	    
	    // --- we're using a buffered writer, so flushing is crucial
	    outf.flush();
	}
	
	public static void main(final String argv[]) {
		final String name = argv[0];
		final String base;
		if (name.endsWith(".ds")) {
			base = name.substring(0, name.length() - 3);
		} else {
			base = name;
		}
		int i = 0;
		for (Iterator it = new InputIterator(name); it.hasNext();) {
			final DSymbol ds = (DSymbol) it.next();
			final Tiling til = new Tiling(ds);
			
			final EvolverExporter exporter = new EvolverExporter(til);
			final String outname = String.format("%s-%03d.fe", new Object[] {
					base, new Integer(++i) });
			try {
				final FileWriter out = new FileWriter(outname);
				exporter.writeTo(out);
				out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
