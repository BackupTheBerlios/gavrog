/*
   Copyright 2006 Olaf Delgado-Friedrichs

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


package org.gavrog.joss.dsyms.generators;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 * @author Olaf Delgado
 * @version $Id: FaceTransitive.java,v 1.14 2006/11/22 01:36:11 odf Exp $
 */
public class FaceTransitive extends IteratorAdapter {

    /**
     * Generates all feasible 1-dimensional symbols of a given size.
     */
    public static class Faces extends IteratorAdapter {
        final Iterator sets;
        int v;
        DynamicDSymbol currentSet = null;
        
        public Faces(final int size) {
            if (size % 2 == 1) {
                final DynamicDSymbol ds = new DynamicDSymbol(1);
                final List elms = ds.grow(size);
                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(0, elms.get(i-1), elms.get(i));
                    ds.redefineOp(1, elms.get(i), elms.get(i+1));
                }
                ds.redefineOp(1, elms.get(0), elms.get(0));
                ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
                this.sets = Iterators.singleton(new DSymbol(ds));
            } else {
                final List tmp = new LinkedList();
                final DynamicDSymbol ds = new DynamicDSymbol(1);
                final List elms = ds.grow(size);
                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(0, elms.get(i-1), elms.get(i));
                    ds.redefineOp(1, elms.get(i), elms.get((i+1) % size));
                }
                tmp.add(new DSymbol(ds));

                ds.redefineOp(1, elms.get(0), elms.get(0));
                ds.redefineOp(1, elms.get(size-1), elms.get(size-1));
                tmp.add(new DSymbol(ds));

                for (int i = 1; i < size; i += 2) {
                    ds.redefineOp(1, elms.get(i-1), elms.get(i));
                    ds.redefineOp(0, elms.get(i), elms.get((i+1) % size));
                }
                ds.redefineOp(0, elms.get(0), elms.get(0));
                ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
                tmp.add(new DSymbol(ds));

                this.sets = tmp.iterator();
            }
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.currentSet != null && this.v <= 4) {
                    final DynamicDSymbol ds = this.currentSet;
                    final Object D = ds.elements().next();
                    ds.redefineV(0, 1, D, this.v);
                    ++this.v;
                    if (ds.m(0, 1, D) >= 3) {
                        return new DSymbol(ds);
                    }
                } else if (this.sets.hasNext()) {
                    final DSymbol ds = (DSymbol) this.sets.next();
                    this.currentSet = new DynamicDSymbol(ds);
                    this.v = 1;
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
     * Generates all feasible 2-dimensional symbols extending a given
     * 1-dimensional one.
     */
    private class SingleFaceTiles extends IteratorAdapter {
        final private Rational minCurv = new Fraction(1, 12);
		final private int minVert;
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public SingleFaceTiles(final DSymbol face, final int minVert) {
            time4SingleFaced.start();
            this.minVert = minVert;
            this.preTiles = new CombineTiles(face);
            time4SingleFaced.stop();
        }

        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            time4SingleFaced.start();
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final Iterator elms = ds.elements(); elms.hasNext();) {
                        if (ds.m(1, 2, elms.next()) > 2) {
                            time4SingleFaced.stop();
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 3, minVert, minCurv);
                } else {
                    time4SingleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
     * Generates all feasible 2-dimensional symbols made from two copies of a
     * given 1-dimensional one.
     */
    private class DoubleFaceTiles extends IteratorAdapter {
		final private int minVert;
        final private Rational minCurv = new Fraction(1, 12);
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();
        
        public DoubleFaceTiles(final DSymbol face, final int minVert) {
            time4DoubleFaced.start();
        	this.minVert = minVert;
            final DynamicDSymbol ds = new DynamicDSymbol(face);
            ds.append(face);
            this.preTiles = new CombineTiles(new DSymbol(ds));
            time4DoubleFaced.stop();
        }

        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            time4DoubleFaced.start();
            while (true) {
                if (this.tiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.tiles.next();
                    if (!ds.isSpherical2D()) {
                        continue;
                    }
                    for (final Iterator elms = ds.elements(); elms.hasNext();) {
                        if (ds.m(1, 2, elms.next()) > 2) {
                            time4DoubleFaced.stop();
                            return ds;
                        }
                    }
                } else if (this.preTiles.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTiles.next();
                    this.tiles = new DefineBranching2d(ds, 3, minVert, minCurv);
                } else {
                    time4DoubleFaced.stop();
                    throw new NoSuchElementException("at end");
                }
            }
        }
    }
    
    /**
	 * Generates all minimal, euclidean, 3-dimensional symbols extending a given
	 * 1-dimensional symbol.
	 */
    private class ONE extends IteratorAdapter {
    	Iterator tiles = Iterators.empty();
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public ONE(final DSymbol face, final int minVert) {
            time4One.start();
    		this.tiles = new SingleFaceTiles(face, minVert);
            time4One.stop();
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            time4One.start();
            while (true) {
                time4Final.start();
                boolean moreTilings = this.tilings.hasNext();
                time4Final.stop();
                if (moreTilings) {
                    time4Final.start();
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    time4Final.stop();
                    if (isGood(ds)) {
                        time4One.stop();
                        return ds;
                    }
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    if (!hasTrivialVertices(ds)) {
                        time4Final.start();
                        this.tilings = new DefineBranching3d(ds);
                        time4Final.stop();
                    }
                } else if (this.tiles.hasNext()) {
                	final DSymbol ds = (DSymbol) this.tiles.next();
                	this.preTilings = new CombineTiles(ds);
                } else {
                    time4One.stop();
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    /**
	 * Generates all minimal, euclidean, 3-dimensional symbols containing two
	 * 2-dimensional symbols each made from the single given 1-dimensional one.
	 */
    private class TWO extends IteratorAdapter {
    	final List tiles;
    	int i, j;
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public TWO(final DSymbol face, final int minVert) {
            time4Two.start();
    		this.tiles = Iterators.asList(new SingleFaceTiles(face, minVert));
    		i = j = 0;
            time4Two.stop();
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            time4Two.start();
            while (true) {
                time4Final.start();
                boolean moreTilings = this.tilings.hasNext();
                time4Final.stop();
                if (moreTilings) {
                    time4Final.start();
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    time4Final.stop();
                    if (isGood(ds)) {
                        time4Two.stop();
                        return ds;
                    }
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    if (!hasTrivialVertices(ds)) {
                        time4Final.start();
                        this.tilings = new DefineBranching3d(ds);
                        time4Final.stop();
                    }
                } else if (this.i < this.tiles.size()) {
                	final DSymbol t1 = (DSymbol) this.tiles.get(i);
                	final DSymbol t2 = (DSymbol) this.tiles.get(j);
                	++j;
                	if (j >= this.tiles.size()) {
                		j = ++i;
                	}
                	final DynamicDSymbol ds = new DynamicDSymbol(t1);
                	ds.append(t2);
                	this.preTilings = new CombineTiles(new DSymbol(ds));
                } else {
                    time4Two.stop();
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }
    
    /**
	 * Generates all minimal, euclidean, tile- and face-transitive 3-dimensional
	 * symbols made from two copies of a given 1-dimensional symbol.
	 */
    private class DOUBLE extends IteratorAdapter {
    	final List idcs = new IndexList(0, 1, 3);
    	Iterator tiles = Iterators.empty();
        Iterator preTilings = Iterators.empty();
        Iterator tilings = Iterators.empty();
        
    	public DOUBLE(final DSymbol face, final int minVert) {
            time4Double.start();
    		this.tiles = new DoubleFaceTiles(face, minVert);
            time4Double.stop();
    	}

		/* (non-Javadoc)
		 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
		 */
		protected Object findNext() throws NoSuchElementException {
            time4Double.start();
            while (true) {
                time4Final.start();
                boolean moreTilings = this.tilings.hasNext();
                time4Final.stop();
                if (moreTilings) {
                    time4Final.start();
                    final DSymbol ds = (DSymbol) this.tilings.next();
                    time4Final.stop();
                    if (isGood(ds)) {
                        time4Double.stop();
                        return ds;
                    }
                } else if (this.preTilings.hasNext()) {
                    final DSymbol ds = (DSymbol) this.preTilings.next();
                    if (ds.numberOfOrbits(idcs) == 1 && !hasTrivialVertices(ds)) {
                        time4Final.start();
                        this.tilings = new DefineBranching3d(ds);
                        time4Final.stop();
                    }
                } else if (this.tiles.hasNext()) {
                	final DSymbol ds = (DSymbol) this.tiles.next();
                	this.preTilings = new CombineTiles(ds);
                } else {
                    time4Double.stop();
                    throw new NoSuchElementException("at end");
                }
            }
		}
    }

    final private int maxSize;
    final private int minVert;

    private int size;
    private int type;
    private Iterator faces = Iterators.empty();
    private Iterator tilings = Iterators.empty();

    private int badVertices = 0;
    private int nonMinimal = 0;
    private int nonEuclidean = 0;
    private Stopwatch time4euclid = new Stopwatch();
    private Stopwatch time4One = new Stopwatch();
    private Stopwatch time4Two = new Stopwatch();
    private Stopwatch time4Double = new Stopwatch();
    private Stopwatch time4SingleFaced = new Stopwatch();
    private Stopwatch time4DoubleFaced = new Stopwatch();
    private Stopwatch time4Final = new Stopwatch();

    public FaceTransitive(final int minSize, final int maxSize,
            final int minVertexDegree) {
        this.maxSize = maxSize;
        this.minVert = minVertexDegree;
        
        this.size = minSize - 1;
        this.type = 3;
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.box.collections.IteratorAdapter#findNext()
     */
    protected Object findNext() throws NoSuchElementException {
        while (true) {
            if (this.tilings.hasNext()) {
                return this.tilings.next();
            } else if (this.faces.hasNext()) {
                final DSymbol face = (DSymbol) faces.next();
                switch (this.type) {
                case 0:
                    this.tilings = new ONE(face, this.minVert);
                    break;
                case 1:
                    this.tilings = new TWO(face, this.minVert);
                    break;
                case 2:
                    this.tilings = new DOUBLE(face, this.minVert);
                    break;
                }
            } else if ((this.type < 2 && this.size % 2 == 0) || this.type < 0) {
            	++this.type;
                if (this.type == 0) {
                    this.faces = new Faces(this.size);
                } else {
                    this.faces = new Faces(this.size / 2);
                }
            } else if (this.size < this.maxSize) {
                ++this.size;
                this.type = -1;
            } else {
                throw new NoSuchElementException("at end");
            }
        }
    }
    
    public int getBadVertices() {
        return this.badVertices;
    }

    public int getNonEuclidean() {
        return this.nonEuclidean;
    }

    public int getNonMinimal() {
        return this.nonMinimal;
    }
    
    private boolean hasTrivialVertices(final DSymbol ds) {
        final List idcs = new IndexList(1, 2, 3);

        for (final Iterator reps = ds.orbitRepresentatives(idcs); reps
                .hasNext();) {
            boolean bad = true;
            for (final Iterator elms = ds.orbit(idcs, reps.next()); elms
                    .hasNext();) {
                if (ds.m(1, 2, elms.next()) > 2) {
                    bad = false;
                    break;
                }
            }
            if (bad) {
                ++this.badVertices;
                return true;
            }
        }
        return false;
    }
        
    private boolean isGood(final DSymbol ds) {
        if (!ds.isMinimal()) {
            ++this.nonMinimal;
            return false;
        }
        this.time4euclid.start();
        final boolean bad = new EuclidicityTester(ds).isBad();
        this.time4euclid.stop();
        if (bad) {
            ++this.nonEuclidean;
            return false;
        }

        return true;
    }
    
    public String timeForEuclidicityTest() {
        return this.time4euclid.format();
    }
    
    public String timeForCaseOne() {
        return this.time4One.format();
    }
    
    public String timeForCaseTwo() {
        return this.time4Two.format();
    }
    
    public String timeForCaseDouble() {
        return this.time4Double.format();
    }
    
    public String timeForSingleFacedTiles() {
        return this.time4SingleFaced.format();
    }
    
    public String timeForDoubleFacedTiles() {
        return this.time4DoubleFaced.format();
    }
    
    public String timeForFinalBranching() {
        return this.time4Final.format();
    }
    
    /**
     * Main method.
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        final int n = args.length;
        final int minSize = (n > 0 ? Integer.parseInt(args[0]) : 1);
        final int maxSize = (n > 1 ? Integer.parseInt(args[1]) : 8);
        final int minVert = (n > 2 ? Integer.parseInt(args[2]) : 2);

        final FaceTransitive symbols = new FaceTransitive(minSize, maxSize,
				minVert);

        final Stopwatch total = new Stopwatch();
        total.start();
		final int count = Iterators.print(System.out, symbols, "\n");
		total.stop();
		System.out.println("\n# Generated " + count + " symbols.");
		System.out.println("# Rejected " + symbols.getBadVertices()
				+ " symbols with trivial vertices,");
		System.out.println("#          " + symbols.getNonMinimal()
				+ " non-minimal symbols and");
		System.out.println("#          " + symbols.getNonEuclidean()
				+ " non-Euclidean symbols");
		System.out.println("# Execution time was " + total.format() + ".");
        System.out.println("#     Used " + symbols.timeForEuclidicityTest()
                + " for Euclidicity tests.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForCaseOne()
                + " for case ONE.");
        System.out.println("#     Used " + symbols.timeForCaseTwo()
                + " for case TWO.");
        System.out.println("#     Used " + symbols.timeForCaseDouble()
                + " for case DOUBLE.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForSingleFacedTiles()
                + " to generate single-faced tiles.");
        System.out.println("#     Used " + symbols.timeForDoubleFacedTiles()
                + " to generate double-faced tiles.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForFinalBranching()
                + " to generate final branching.");
    }
}
