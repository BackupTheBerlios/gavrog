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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * @version $Id: FaceTransitive.java,v 1.23 2006/12/08 01:01:19 odf Exp $
 */
public class FaceTransitive extends IteratorAdapter {

    /**
     * Generates all feasible 1-dimensional symbols of a given size.
     */
    public static class Faces extends IteratorAdapter {
		final private int minVert;
        final Iterator sets;
        int v;
        DynamicDSymbol currentSet = null;
        
        public Faces(final int size, int minVert) {
        	this.minVert = minVert;
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
                    if (ds.m(0, 1, D) < 3) {
                    	continue;
                    }
                    if (this.minVert >= 3 && ds.m(0, 1, D) > 5) {
                    	continue;
                    }
                    return new DSymbol(ds);
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

    private class Augmenter extends IteratorAdapter {
        final private DSymbol base;
        final private int targetSize;
        final Object orbRep[];
        final int orbSize[];
        final int orbAdded[];
        int currentSize;

        /**
         * Constructs an instance.
         * @param base
         * @param size
         */
        public Augmenter(final DSymbol base, final int size) {
            // --- store parameters
            this.base = base;
            this.targetSize = size;
            this.currentSize = base.size();
            
            // --- collect (0,2)-orbits
            final List idcs = new IndexList(0, 2);
            final List orbits = new ArrayList();
            for (final Iterator reps = base.orbitRepresentatives(idcs); reps
                    .hasNext();) {
                final Object D = reps.next();
                final List orbit = Iterators.asList(base.orbit(idcs, D));
                orbits.add(orbit);
            }

            // --- sort by decreasing size
            Collections.sort(orbits, new Comparator() {
                public int compare(final Object arg0, final Object arg1) {
                    final List l0 = (List) arg0;
                    final List l1 = (List) arg1;
                    return l1.size() - l0.size();
                }
            });
            
            // --- create arrays
            final int n = orbits.size();
            this.orbRep = new Object[n];
            this.orbSize = new int[n];
            this.orbAdded = new int[n];
            
            // --- fill arrays
            for (int i = 0; i < n; ++i) {
                final List orb = (List) orbits.get(i);
                this.orbRep[i] = orb.get(0);
                this.orbSize[i] = orb.size();
                this.orbAdded[i] = 0;
            }
            
            // --- a little trick to make findNext() code simpler
            if (this.currentSize == this.targetSize) {
                this.orbAdded[n-1] = -this.orbSize[n-1];
                this.currentSize -= this.orbSize[n-1];
            }
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                final int target = this.targetSize;
                int i = this.orbRep.length - 1;
                while (i >= 0 && this.currentSize + this.orbSize[i] > target) {
                    this.currentSize -= this.orbAdded[i];
                    this.orbAdded[i] = 0;
                    --i;
                }
                if (i < 0) {
                    throw new NoSuchElementException("at end");
                }
                this.orbAdded[i] += this.orbSize[i];
                this.currentSize += this.orbSize[i];

                if (this.currentSize == this.targetSize) {
                    return augmented();
                }
            }
        }

        /**
         * @return
         */
        private DSymbol augmented() {
            final DSymbol ds = this.base;
            // TODO augment the tile and return it
            return ds;
        }
    }
    
    // --- used by SingleFaceTiles
    final static Map bases = new HashMap();

    /**
     * Generates all feasible 2-dimensional symbols extending a given
     * 1-dimensional one. This version works by introducing extra degree 2
     * vertices into tiles with a smaller face in all possible ways. The base
     * tiles for this process are generated by the direct method of {@link
     * #BaseSingleFaceTiles}
     */
    private class SingleFaceTiles extends IteratorAdapter {
        final private int minVert;
        final private int targetSize;
        final private Iterator baseFaces;
        private Iterator baseTiles;
        private Iterator augmented;
        
        public SingleFaceTiles(final DSymbol face, final int minVert) {
            this.minVert = minVert;
            this.targetSize = face.size();
            this.baseFaces = baseFaces(face).iterator();
            this.baseTiles = Iterators.empty();
            this.augmented = Iterators.empty();
        }

        /**
         * Determines the type of 1-dimensional symbol. The type is -1 for a
         * cycle and coincides with the number of 1-loops for a chain.
         * 
         * @param face the 1-dimensional input symbol.
         * @return the type of the input symbol.
         */
        private int type(final DSymbol face) {
            if (face.isLoopless()) {
                return -1;
            } else {
                int oneLoops = 0;
                for (final Iterator elms = face.elements(); elms.hasNext();) {
                    final Object D = elms.next();
                    if (face.op(1, D).equals(D)) {
                        ++oneLoops;
                    }
                }
                return oneLoops;
            }
        }
        
        /**
         * Determines a list of 1-dimensional symbols such that every
         * 2-dimensional symbol extending the given 1-dimensional can be
         * obtained by introducing degree 2 vertices into a 2-dimensional
         * extension of some symbol in the list.
         * 
         * @param face the 1-dimensional input symbol.
         * @return the list of base symbols for the input symbol.
         */
        private List baseFaces(final DSymbol face) {
            final List results = new LinkedList();
            if (this.minVert >= 3) {
                results.add(face);
            } else {
                final int v = face.v(0, 1, face.elements().next());
                final int type = type(face);
                final int d = v * (type >= 0 ? 2 : 1);
                for (int n = 4; n <= 10; n += 2) {
                    if (n % d != 0) {
                        continue;
                    }
                    final int size = n / d;
                    if (size > face.size()) {
                        continue;
                    }
                    for (final Iterator iter = new Faces(size, 3); iter
                            .hasNext();) {
                        final DSymbol ds = (DSymbol) iter.next();
                        if (ds.v(0, 1, ds.elements().next()) == v
                                && type(ds) == type) {
                            results.add(ds);
                        }
                    }
                }
            }
            return results;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.gavrog.box.collections.IteratorAdapter#findNext()
         */
        protected Object findNext() throws NoSuchElementException {
            while (true) {
                if (this.augmented.hasNext()) {
                    return this.augmented.next();
                } else if (this.baseTiles.hasNext()) {
                    final DSymbol tile = (DSymbol) this.baseTiles.next();
                    if (this.minVert >= 3) {
                        if (this.minVert <= minVert(tile)) {
                            return tile;
                        } else {
                            continue;
                        }
                    } else {
                        this.augmented = new Augmenter(tile, this.targetSize);
                    }
                } else if (this.baseFaces.hasNext()) {
                    final DSymbol face = (DSymbol) this.baseFaces.next();
                    final List invariant = face.invariant();
                    if (!bases.containsKey(invariant)) {
                        final Iterator tiles = new BaseSingleFaceTiles(face, 3);
                        bases.put(invariant, Iterators.asList(tiles));
                    }
                    this.baseTiles = ((List) bases.get(invariant)).iterator();
                } else {
                    throw new NoSuchElementException("at end");
                }
            }
        }

        /**
         * Returns the minimum vertex degree present in a tile.
         * 
         * @param tile a 2-dimensional symbol encoding the tile.
         * @return the minimum vertex degree for the input tile.
         */
        private int minVert(final DSymbol tile) {
            final List idcs = new IndexList(1, 2);
            int min = Integer.MAX_VALUE;
            for (final Iterator iter = tile.orbitRepresentatives(idcs); iter
                    .hasNext();) {
                min = Math.min(min, tile.m(1, 2, iter.next()));
            }
            return min;
        }
    }

    /**
     * Generates all feasible 2-dimensional symbols extending a given
     * 1-dimensional one. This class uses a direct method and is used by {@link
     * #SingleFaceTiles} to generate base tiles with minimal vertex degree 3.
     */
    private class BaseSingleFaceTiles extends IteratorAdapter {
        final private Rational minCurv = new Fraction(1, 12);
        final private int minVert;
        Iterator preTiles = Iterators.empty();
        Iterator tiles = Iterators.empty();

        public BaseSingleFaceTiles(final DSymbol face, final int minVert) {
            time4SingleFaced.start();
            this.minVert = minVert;
            this.preTiles = new CombineTiles(face);
            time4SingleFaced.stop();
        }

        /*
         * (non-Javadoc)
         * 
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
                            ++countSingleFaced;
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
                            ++countDoubleFaced;
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
	private int countSingleFaced = 0;
	private int countDoubleFaced = 0;
    
    private Stopwatch time4euclid = new Stopwatch();
    private Stopwatch time4One = new Stopwatch();
    private Stopwatch time4Two = new Stopwatch();
    private Stopwatch time4Double = new Stopwatch();
    private Stopwatch time4SingleFaced = new Stopwatch();
    private Stopwatch time4DoubleFaced = new Stopwatch();
    private Stopwatch time4Final = new Stopwatch();

    public FaceTransitive(final int minSize, final int maxSize,
            final int minVertexDegree) {
    	if (minVertexDegree >= 3 && (maxSize > 20 || maxSize < 1)) {
    		this.maxSize = 20;
    	} else {
    		this.maxSize = maxSize;
    	}
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
                    this.faces = new Faces(this.size, minVert);
                } else {
                    this.faces = new Faces(this.size / 2, minVert);
                }
            } else if (this.maxSize < 1 || this.size < this.maxSize) {
                ++this.size;
                this.type = -1;
            } else {
                throw new NoSuchElementException("at end");
            }
        }
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
    
    public int getBadVertices() {
        return this.badVertices;
    }

    public int getNonEuclidean() {
        return this.nonEuclidean;
    }

    public int getNonMinimal() {
        return this.nonMinimal;
    }
    
	public int getCountDoubleFaced() {
		return this.countDoubleFaced;
	}

	public int getCountSingleFaced() {
		return this.countSingleFaced;
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

        System.out.println("# Generated by Gavrog on "
				+ Calendar.getInstance().getTime());
        System.out.println("#");
        System.out.println("# Class: FaceTransitive");
        System.out.print("# Command line arguments:");
        for (int i = 0; i < n; ++i) {
        	System.out.print(" " + args[i]);
        }
        System.out.println();
        System.out.println("#");
        System.out.println("# Running Java "
				+ System.getProperty("java.version") + " on "
				+ System.getProperty("os.name"));
        System.out.println("#");
        final Stopwatch total = new Stopwatch();
        total.start();
		final int count = Iterators.print(System.out, symbols, "\n");
		total.stop();
        System.out.println();
        System.out.println("#");
		System.out.println("# Generated " + count + " symbols.");
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
				+ " to generate " + symbols.getCountSingleFaced()
				+ " single-faced tiles.");
        System.out.println("#     Used " + symbols.timeForDoubleFacedTiles()
                + " to generate " + symbols.getCountDoubleFaced()
                + " double-faced tiles.");
        System.out.println("#");
        System.out.println("#     Used " + symbols.timeForFinalBranching()
                + " to generate final branching.");
        System.out.println("#");
        System.out.println("# Program finished on "
				+ Calendar.getInstance().getTime());
    }
}
