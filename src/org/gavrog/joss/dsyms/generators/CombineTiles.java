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

package org.gavrog.joss.dsyms.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.basic.Traversal;

/**
 * An iterator that takes a (d-1)-dimensional Delaney symbol encoding a
 * collection of tiles and combines them to a connected d-dimensional in every
 * possible way.
 * 
 * For each isomorphism class of extended symbols, only one representative is
 * produced. The order or naming of elements is not preserved.
 * 
 * @author Olaf Delgado
 * @version $Id: CombineTiles.java,v 1.8 2007/04/26 20:21:58 odf Exp $
 */
public class CombineTiles extends IteratorAdapter {
    // TODO test local euclidicity where possible

    // --- set to true to enable logging
    final private static boolean LOGGING = false;

    // --- the input symbol
    final DelaneySymbol original;

    // --- precomputed or extracted data used by the algorithm
    final private int dim;
    final private List<List<DSymbol>> componentTypes;
    final private Map<List, Integer> invarToIndex = new HashMap<List, Integer>();
    final private List<Map> indexToRepMap = new ArrayList<Map>();

    // --- the current state
    private final DynamicDSymbol current;
    private final LinkedList<Move> stack;
    private final List<Integer> unused;

    // --- auxiliary information applying to the current state
    private int size;
    private Map<Object, Pair> signatures;

    /**
     * The instances of this class represent individual moves of setting
     * d-neighbor values. These become the entries of the trial stack.
     */
    protected class Move {
        final public Object element;
        final public Object neighbor;
        final public int newType;
        final public int newForm;
        final public boolean isChoice;

        public Move(final Object element, final Object neighbor, int newType,
                int newForm, final boolean isChoice) {
            this.element = element;
            this.neighbor = neighbor;
            this.newType = newType;
            this.newForm = newForm;
            this.isChoice = isChoice;
        }

        @Override
		public String toString() {
            return "Move(" + element + ", " + neighbor + ", " + newType + ", "
                    + newForm + ", " + isChoice + ")";
        }
    }

    /**
     * Creates a new instance.
     * 
     * @param ds the symbol to extend.
     */
    public CombineTiles(final DelaneySymbol ds) {
        this.dim = ds.dim() + 1;

        // --- check the argument
        if (ds == null) {
            throw new IllegalArgumentException("null argument");
        }
        try {
            ds.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        if (!ds.isComplete()) {
            throw new UnsupportedOperationException("symbol must be complete");
        }
        final List idcs = new IndexList(ds);
        for (final Iterator orbs = ds.orbitReps(idcs); orbs
                .hasNext();) {
            final DelaneySymbol sub = new Subsymbol(ds, idcs, orbs.next());
            if (this.dim == 3 && !sub.isSpherical2D()) {
                throw new IllegalArgumentException(
                        "components must be spherical");
            }
        }

        // --- remember the input symbol
        this.original = ds;

        // --- compute auxiliary information
        final DSymbol canonical = (DSymbol) ds.canonical();
        final Map<DelaneySymbol, Integer> multiplicities =
        	componentMultiplicities(canonical);
        final List types = new ArrayList<DelaneySymbol>(multiplicities.keySet());
        Collections.sort(types);
        final List<List<DSymbol>> forms = new ArrayList<List<DSymbol>>();
        final List<Integer> free = new ArrayList<Integer>();
        for (int i = 0; i < types.size(); ++i) {
            final DelaneySymbol type = (DelaneySymbol) types.get(i);
            forms.add(Collections.unmodifiableList(subCanonicalForms(type)));
            free.add(multiplicities.get(type));
            if (LOGGING) {
                System.out.println(free.get(i) + " copies and "
                        + ((List) forms.get(i)).size() + " forms for " + type
                        + " with invariant " + type.invariant() + "\n");
            }
        }
        this.componentTypes = Collections.unmodifiableList(forms);

        // --- initialize the state
        this.size = 0;
        this.signatures = new HashMap<Object, Pair>();
        this.stack = new LinkedList<Move>();
        this.unused = new ArrayList<Integer>(free);
        this.current = new DynamicDSymbol(this.dim);

        // --- add the component with the smallest invariant to the current
        // symbol
        final DSymbol start = this.componentTypes.get(0).get(0);
        this.current.append((DSymbol) start.canonical()); // --- MUST be made
        // canonical!
        this.unused.set(0, this.unused.get(0) - 1);
        this.size = this.current.size();
        this.signatures = elementSignatures(this.current, this.dim - 2);

        // --- push a dummy move on the stack as a fallback
        stack.addLast(new Move(1, 0, 0, 0, true));
    }

    /**
     * Repeatedly finds the next legal choice in the enumeration tree and
     * executes it, together with all its implications, until all 3-neighbors
     * are defined and in canonical form, in which case the resulting symbol is
     * returned.
     * 
     * Does appropriate backtracking in order to find the respective next
     * choice. Also backtracks if the partial symbol resulting from the latest
     * choice is not in canonical form.
     * 
     * To simplify the code, the algorithm makes use of "dummy moves", which are
     * put on the stack as fallback entries but do not have any effect on the
     * symbol. A dummy move is of the form
     * <code>Move(element, ..., false)</code> and effectively indicates that
     * the next neighbor to be set is for element.
     * 
     * @return the next symbol, if any.
     */
    @Override
	protected Object findNext() throws NoSuchElementException {
        if (LOGGING) {
            System.out.println("findNext(): stack size = " + this.stack.size());
        }
        while (true) {
            final Move choice = undoLastChoice();
            if (LOGGING) {
                System.out.println("  last choice was " + choice);
                System.out.println("  current symbol:\n"
                        + this.current.tabularDisplay());
            }
            if (choice == null) {
                throw new NoSuchElementException();
            }
            final Move move = nextMove(choice);
            if (move == null) {
                if (LOGGING) {
                    System.out.println("  no potential move");
                }
                continue;
            }
            if (LOGGING) {
                System.out.println("  found potential move " + move);
            }
            if (performMove(move)) {
                if (LOGGING) {
                    System.out.println("  new symbol after move:\n"
                            + this.current.tabularDisplay());
                }
                if (isCanonical()) {
                    final Object D = nextFreeElement(choice.element);
                    if (D == null) {
                        if (this.size == this.original.size()) {
                            final DSymbol ds = new DSymbol(this.current);
                            if (this.dim != 3
                                    || Utils.mayBecomeLocallyEuclidean3D(ds)) {
                                return new DSymbol(this.current);
                            }
                        }
                    } else {
                        this.stack.addLast(new Move(D, 0, -1, -1, true));
                    }
                } else {
                    if (LOGGING) {
                        System.out.println("  result of move is not canonical");
                    }
                }
            } else {
                if (LOGGING) {
                    System.out.println("  move was rejected");
                }
            }
        }
    }

    /**
     * Undoes the last choice and all its implications by popping moves from the
     * stack until one is found which is a choice. The corresponding neighbor
     * assignment are cleared and the last choice is returned. If there was no
     * choice left on the stack, a <code>null</code> result is returned.
     * 
     * @return the last choice or null.
     */
    private Move undoLastChoice() {
        final List idcs = new IndexList(this.current);
        Move last;
        do {
            if (stack.size() == 0) {
                return null;
            }
            last = this.stack.removeLast();

            if (LOGGING) {
                System.out.println("Undoing " + last);
            }
            if (this.current.hasElement(last.neighbor)) {
                this.current.undefineOp(this.dim, last.element);
            }
            if (last.newType >= 0 && (Integer) last.neighbor > 0) {
                final Iterator disposable = this.current.orbit(idcs,
                        last.neighbor);
                while (disposable.hasNext()) {
                    this.current.removeElement(disposable.next());
                }
                this.current.renumber();
                this.unused.set(last.newType, this.unused.get(last.newType) + 1);
                this.size = this.current.size();
                this.signatures = elementSignatures(this.current, this.dim - 2);
            }
        } while (!last.isChoice);

        return last;
    }

    /**
     * Finds the next legal move with the same element to connect.
     * 
     * @param choice describes the previous move.
     * @return the next move or null.
     */
    private Move nextMove(final Move choice) {
        final Object D = choice.element;
        final Object sigD = this.signatures.get(D);

        // --- look for a neighbor in the curently connected portion
        for (int E = (Integer) choice.neighbor + 1; E <= size; ++E) {
            if (!this.current.definesOp(this.dim, E)
                    && sigD.equals(this.signatures.get(E))) {
                return new Move(choice.element, E, -1, -1, true);
            }
        }

        // --- look for a new component to connect
        int type = Math.max(0, choice.newType);
        int form = Math.max(0, choice.newForm + 1);
        while (type < this.componentTypes.size()) {
            if (this.unused.get(type) > 0) {
                final List forms = this.componentTypes.get(type);
                while (form < forms.size()) {
                    final DSymbol candidate = (DSymbol) forms.get(form);
                    final Map sigs = elementSignatures(candidate, this.dim - 2);
                    if (sigD.equals(sigs.get(1))) {
                        return new Move(choice.element, this.size + 1, type,
								form, true);
                    }
                    ++form;
                }
            }
            ++type;
            form = 0;
        }

        // --- nothing found
        return null;
    }

    /**
     * Performs a move with all its implications. This includes setting the
     * neighbor relation described by the move, pushing the move on the stack
     * and as well performing all the deduced moves as dictated by the
     * orthogonality of the 3-neighbor operation with the 0- and 1-operations.
     * 
     * @param initial the move to try.
     * @return true if the move did not lead to a contradiction.
     */
    private boolean performMove(final Move initial) {
        // --- a little shortcut
        final DynamicDSymbol ds = this.current;

        // --- we maintain a queue of deductions, starting with the initial move
        final LinkedList<Move> queue = new LinkedList<Move>();
        queue.addLast(initial);

        while (queue.size() > 0) {
            // --- get some info on the next move in the queue
            Move move = queue.removeFirst();
            final int type = move.newType;
            final int form = move.newForm;
            final Object D = move.element;
            final Object E = move.neighbor;
            final int d = this.dim;

            // --- see if the move would contradict the current state
            if (ds.definesOp(d, D) || ds.definesOp(d, E)) {
                if (ds.definesOp(d, D) && ds.op(d, D).equals(E)) {
                    continue;
                } else {
                    if (LOGGING) {
                        System.out.println("Found contradiction at " + D
                                + "<-->" + E);
                    }
                    if (move == initial) {
                        // --- the initial move was impossible
                        throw new IllegalArgumentException(
                                "Internal error: received illegal move.");
                    }
                    return false;
                }
            }

            // --- perform the move
            if (type >= 0) {
                // --- connect a new component
                final DSymbol component = (DSymbol) ((List) this.componentTypes
                        .get(type)).get(form);
                this.current.append(component);
                this.unused.set(type, this.unused.get(type) - 1);
                this.size = this.current.size();
                this.signatures = elementSignatures(this.current, this.dim - 2);
            }
            ds.redefineOp(d, D, E);

            // --- record the move we have performed
            this.stack.addLast(move);

            // --- handle deductions or contradictions specified by a derived
            // class
            final List<Move> extraDeductions = getExtraDeductions(ds, move);
            if (extraDeductions == null) {
                return false;
            } else {
                if (LOGGING) {
                    for (final Iterator iter = extraDeductions.iterator(); iter
                            .hasNext();) {
                        final Move ded = (Move) iter.next();
                        System.err.println("    found extra deduction " + ded);
                    }
                }
                queue.addAll(extraDeductions);
            }

            // --- check for any problems with that move
            if (!this.signatures.get(D).equals(this.signatures.get(E))) {
                if (LOGGING) {
                    System.out
                            .println("Bad connection " + D + "<-->" + E + ".");
                }
                return false;
            }

            // --- finally, find deductions
            for (int i = 0; i <= d - 2; ++i) {
                final Object Di = ds.op(i, D);
                final Object Ei = ds.op(i, E);
                if (LOGGING) {
                    System.out.println("Found deduction " + Di + "<-->" + Ei);
                }
                queue.addLast(new Move(Di, Ei, -1, -1, false));
            }
        }

        // --- everything went smoothly
        return true;
    }

    /**
     * Tests if the current symbol is in canonical form with respect to this
     * generator class. That means it is the form of the symbol that should be
     * reported.
     * 
     * @return true if the symbol is canonical.
     */
    private boolean isCanonical() {
        final DSymbol flat = new DSymbol(this.current);
        return flat.getMapToCanonical().get(1).equals(1);
    }

    /**
     * Finds the next symbol element with an undefined d-neighbor, starting from
     * the given element.
     * 
     * @param element to start the search at.
     * @return the next unconnected element.
     */
    private Object nextFreeElement(final Object element) {
        int D = (Integer) element;
        do {
            if (++D > this.size) {
                return null;
            }
        } while (this.current.definesOp(this.dim, D));

        return D;
    }

    /**
     * Computes signatures for the elements of symbol.
     * 
     * @param ds the symbol to compute signatures for.
     * @param dim
     * @return a map assigning signatures to the symbol's elements.
     */
    public Map<Object, Pair> elementSignatures(final DelaneySymbol ds,
			final int dim) {
        final Map<Object, Pair> signatures = new HashMap<Object, Pair>();
        final List<Integer> idcs = new ArrayList<Integer>();
        for (int i = 0; i <= dim; ++i) {
            idcs.add(i);
        }

        for (final Iterator reps = ds.orbitReps(idcs); reps
                .hasNext();) {
            final Object D = reps.next();
            final DelaneySymbol face = new Subsymbol(ds, idcs, D);
            final List invariant = face.invariant();
            if (!this.invarToIndex.containsKey(invariant)) {
                final int i = this.indexToRepMap.size();
                this.invarToIndex.put(invariant, i);
                final DSymbol canon = (DSymbol) face.canonical();
                this.indexToRepMap.add(mapToFirstRepresentatives(canon));
            }
            final Integer i = this.invarToIndex.get(invariant);
            final Map toRep = this.indexToRepMap.get(i);
            final Map toCanon = face.getMapToCanonical();
            for (final Iterator iter = face.elements(); iter.hasNext();) {
                final Object E = iter.next();
                final Object rep = toRep.get(toCanon.get(E));
                signatures.put(E, new Pair(i, rep));
            }
        }

        return signatures;
    }

    /**
     * Collects the isomorphism types of connected components of a symbol and
     * counts how many times each type occurs. The result is a map with
     * {@link DSymbol} instances as its keys and the associated multiplicities
     * as values.
     * 
     * @param ds the input symbol.
     * @return a map assigning to each component type the number of occurences.
     */
    public static Map<DelaneySymbol, Integer> componentMultiplicities(
			final DelaneySymbol ds) {
        final Map<DelaneySymbol, Integer> type2number =
        	new HashMap<DelaneySymbol, Integer>();
        final List idcs = new IndexList(ds);
        for (final Iterator reps = ds.orbitReps(idcs); reps
                .hasNext();) {
            final DelaneySymbol sub = new Subsymbol(ds, idcs, reps.next())
                    .canonical();
            if (type2number.containsKey(sub)) {
                type2number.put(sub, type2number .get(sub) + 1);
            } else {
                type2number.put(sub, 1);
            }
        }
        return type2number;
    }

    /**
     * Takes a connected symbol and computes the first representative of each
     * equivalence class of its elements with respect to its automorphism group.
     * 
     * @param ds the symbol to use.
     * @return the list of first representatives.
     */
    public static List<Object> firstRepresentatives(final DelaneySymbol ds) {
        final Map map = mapToFirstRepresentatives(ds);
        final List<Object> res = new ArrayList<Object>();
        for (final Iterator elms = ds.elements(); elms.hasNext();) {
            final Object D = elms.next();
            if (D.equals(map.get(D))) {
                res.add(D);
            }
        }
        return res;
    }

    /**
     * Takes a connected symbol and returns a map that to each element assigns
     * the first representative of its equivalence class with respect to the
     * symbol's automorphism group.
     * 
     * @param ds the symbol to use.
     * @return the map from elements to first representatives.
     */
    public static Map<Object, Object> mapToFirstRepresentatives(
			final DelaneySymbol ds) {
        if (!ds.isConnected()) {
            throw new UnsupportedOperationException("symbol must be connected");
        }

        final Map<Object, Object> res = new HashMap<Object, Object>();
        final Iterator elms = ds.elements();
        if (elms.hasNext()) {
            final Object first = elms.next();
            final Partition classes = new Partition();
            while (elms.hasNext()) {
                final Object D = elms.next();
                if (classes.areEquivalent(first, D)) {
                    continue;
                }
                final Map morphism;
                try {
                    morphism = new DSMorphism(ds, ds, first, D);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                for (final Iterator iter = ds.elements(); iter.hasNext();) {
                    final Object E = iter.next();
                    classes.unite(E, morphism.get(E));
                }
            }

            for (final Iterator iter = ds.elements(); iter.hasNext();) {
                final Object D = iter.next();
                final Object E = classes.find(D);
                if (!res.containsKey(E)) {
                    res.put(D, D);
                    res.put(E, D);
                } else {
                    res.put(D, res.get(E));
                }
            }
        }
        return res;
    }

    /**
     * Returns the list of distinct subcanonical forms for a given connected
     * symbol. A subcanonical form is obtained by assigning numbers to the
     * elements in the order they are visited by a standard traversal starting
     * from an arbitrary element.
     * 
     * @param ds the input symbol.
     * @return the list of subcanonical forms.
     */
    public static List<DSymbol> subCanonicalForms(final DelaneySymbol ds) {
        if (!ds.isConnected()) {
            throw new UnsupportedOperationException("symbol must be connected");
        }
        if (!ds.hasStandardIndexSet()) {
            throw new UnsupportedOperationException(
                    "symbol must have indices 0..dim");
        }

        final List<DSymbol> res = new LinkedList<DSymbol>();

        final int size = ds.size();
        final int dim = ds.dim();
        final List idcs = new IndexList(ds);

        final List reps = firstRepresentatives(ds);
        for (final Iterator iter = reps.iterator(); iter.hasNext();) {
            final Object seed = iter.next();
            final Traversal trav = new Traversal(ds, idcs, seed, true);

            // --- elements will be numbered in the order they appear
            final HashMap<Object, Integer> old2new =
            	new HashMap<Object, Integer>();

            // --- follow the traversal and assign the new numbers
            int nextE = 1;
            while (trav.hasNext()) {
                // --- retrieve the next edge
                final DSPair e = (DSPair) trav.next();
                final Object D = e.getElement();

                // --- determine a running number E for the target element D
                final Integer tmp = old2new.get(D);
                if (tmp == null) {
                    // --- element D is encountered for the first time
                    old2new.put(D, nextE);
                    ++nextE;
                }
            }

            // --- construct the new symbol
            int op[][] = new int[dim + 1][size + 1];
            int v[][] = new int[dim][size + 1];

            for (final Iterator elms = ds.elements(); elms.hasNext();) {
                final Object E = elms.next();
                final int D = old2new.get(E);
                for (int i = 0; i <= dim; ++i) {
                    op[i][D] = old2new.get(ds.op(i, E));
                    if (i < dim) {
                        v[i][D] = ds.v(i, i + 1, E);
                    }
                }
            }

            // --- add it to the list
            res.add(new DSymbol(op, v));
        }

        // --- finis
        return res;
    }

    /**
     * Hook for derived classes to specify additional deductions of a move.
     * 
     * @param ds the current symbol.
     * @param move the last move performed.
     * @return the list of deductions (may be empty) or null in case of a
     *         contradiction.
     */
    protected List<Move> getExtraDeductions(final DelaneySymbol ds,
			final Move move) {
		return new ArrayList<Move>();
	}

    public static void main(String[] args) {
        int i = 0;
        final DSymbol ds = new DSymbol(args[i]);
        final Iterator iter = new CombineTiles(ds);

        int count = 0;
        try {
            while (iter.hasNext()) {
                final DSymbol out = (DSymbol) iter.next();
                System.out.println(out);
                ++count;
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        System.err.println("produced " + count + " symbols");
    }
}
