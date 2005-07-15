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

package org.gavrog.joss.dsyms.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.IteratorAdapter;
import org.gavrog.box.Partition;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.Edge;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.Morphism;


/**
 * An iterator that takes a two-dimensional Delaney Symbol with spherical
 * components (a collection of tiles) and defines a 3-neighbor operation on it
 * in every possible way.
 * 
 * For each isomorphism class of extended symbols, only one representative is
 * produced. The order or naming of elements is not preserved.
 * 
 * TODO test symbols (unfinished and finished) for being locally euclidean
 * 
 * @author Olaf Delgado
 * @version $Id: ExtendTo3d.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public class ExtendTo3d extends IteratorAdapter {
    // --- set to true to enable logging
    final private static boolean LOGGING = false;
    
    // --- the input symbol
    final DelaneySymbol original;

    // --- precomputed or extracted data used by the algorithm
    final private List componentTypes;

    // --- the current state
    private final DynamicDSymbol current;
    private final LinkedList stack;
    private final List unused;
    
    // --- auxiliary information applying to the current state
    private int size;
    private Map signatures;

    /**
     * The instances of this class represent individual moves of setting
     * 3-neighbor values. These become the entries of the trial stack.
     */
    private class Move {
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
    public ExtendTo3d(final DSymbol ds) {
        // --- check the argument
        if (ds == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (ds.dim() != 2) {
            throw new IllegalArgumentException("dimension must be 2");
        }
        try {
            ds.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        final List idcs = new IndexList(ds);
        for (final Iterator orbs = ds.orbitRepresentatives(idcs); orbs.hasNext();) {
            final DelaneySymbol sub = new Subsymbol(ds, idcs, orbs.next());
            if (!sub.isSpherical2D()) {
                throw new IllegalArgumentException("components must be spherical");
            }
        }

        // --- remember the input symbol
        this.original = ds;
        
        // --- compute auxiliary information
        final DSymbol canonical = (DSymbol) ds.canonical();
        final Map multiplicities = componentMultiplicities(canonical);
        final List types = new ArrayList(multiplicities.keySet());
        Collections.sort(types);
        final List forms = new ArrayList();
        final List free = new ArrayList();
        for (int i = 0; i < types.size(); ++i) {
            final DelaneySymbol type = (DelaneySymbol) types.get(i);
            forms.add(Collections.unmodifiableList(subCanonicalForms(type)));
            free.add(multiplicities.get(type));
            if (LOGGING) {
                System.out.println(free.get(i) + " copies and " + ((List) forms.get(i)).size()
                        + " forms for " + type + " with invariant " + type.invariant()
                        + "\n");
            }
        }
        this.componentTypes = Collections.unmodifiableList(forms);

        // --- initialize the state
        this.size = 0;
        this.signatures = new HashMap();
        this.stack = new LinkedList();
        this.unused = new ArrayList(free);
        this.current = new DynamicDSymbol(3);

        // --- add the component with the smallest invariant to the current symbol
        final DSymbol start = (DSymbol) ((List) this.componentTypes.get(0)).get(0);
        this.current.append((DSymbol) start.canonical()); // --- MUST be made canonical!
        this.unused.set(0, new Integer(((Integer) this.unused.get(0)).intValue() - 1));
        this.size = this.current.size();
        this.signatures = elementSignatures(this.current);
        
        // --- push a dummy move on the stack as a fallback
        stack.addLast(new Move(new Integer(1), new Integer(0), 0, 0, true));
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
     * symbol. A dummy move is of the form <code>Move(element, ..., false)</code>
     * and effectively indicates that the next neighbor to be set is for
     * element.
     * 
     * @return the next symbol, if any.
     */
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
                            if (Utils.mayBecomeLocallyEuclidean3D(ds)) {
                                return new DSymbol(this.current);
                            }
                        }
                    } else {
                        this.stack.addLast(new Move(D, new Integer(0), -1, -1, true));
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
            last = (Move) this.stack.removeLast();
            
            if (LOGGING) {
                System.out.println("Undoing " + last);
            }
            if (this.current.hasElement(last.neighbor)) {
                this.current.undefineOp(3, last.element);
            }
            if (last.newType >= 0 && ((Integer) last.neighbor).intValue() > 0) {
                final Iterator disposable = this.current.orbit(idcs, last.neighbor);
                while (disposable.hasNext()) {
                    this.current.removeElement(disposable.next());
                }
                this.current.renumber();
                this.unused.set(last.newType, new Integer(((Integer) this.unused
                        .get(last.newType)).intValue() + 1));
                this.size = this.current.size();
                this.signatures = elementSignatures(this.current);
            }
        } while (!last.isChoice);
    
        return last;
    }

    /**
     * Finds the next legal move with the same element to connect.
     * @param choice describes the previous move.
     * @return the next move or null.
     */
    private Move nextMove(final Move choice) {
        final Object D = choice.element;
        final List sigD = (List) this.signatures.get(D);
        
        // --- look for a neighbor in the curently connected portion
        for (int E = ((Integer) choice.neighbor).intValue() + 1; E <= size; ++E) {
            if (!this.current.definesOp(3, new Integer(E)) && sigD.equals(this.signatures.get(new Integer(E)))) {
                return new Move(choice.element, new Integer(E), -1, -1, true);
            }
        }
        
        // --- look for a new component to connect
        int type = Math.max(0, choice.newType);
        int form = Math.max(0, choice.newForm + 1);
        while (type < this.componentTypes.size()) {
            if (((Integer) this.unused.get(type)).intValue() > 0) {
                final List forms = (List) this.componentTypes.get(type);
                while (form < forms.size()) {
                    final DSymbol candidate = (DSymbol) forms.get(form);
                    final Map sigs = elementSignatures(candidate);
                    if (sigD.equals(sigs.get(new Integer(1)))) {
                        return new Move(choice.element, new Integer(this.size + 1), type, form, true);
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
        final LinkedList queue = new LinkedList();
        queue.addLast(initial);

        while (queue.size() > 0) {
            // --- get some info on the next move in the queue
            Move move = (Move) queue.removeFirst();
            final int type = move.newType;
            final int form = move.newForm;
            final Object D = move.element;
            final Object E = move.neighbor;
            
            // --- see if the move would contradict the current state
            if (ds.definesOp(3, D) || ds.definesOp(3, E)) {
                if (ds.definesOp(3, D) && ds.op(3, D).equals(E)) {
                    continue;
                } else {
                    if (LOGGING) {
                        System.out.println("Found contradiction at " + D + "<-->" + E);
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
                final DSymbol component = (DSymbol) ((List) this.componentTypes.get(type)).get(form);
                this.current.append(component);
                this.unused.set(type, new Integer(((Integer) this.unused.get(type)).intValue() - 1));
                this.size = this.current.size();
                this.signatures = elementSignatures(this.current);
            }
            ds.redefineOp(3, D, E);
            
            // --- record the move we have performed
            this.stack.addLast(move);
            
            // --- check for any problems with that move
            if (!this.signatures.get(D).equals(this.signatures.get(E))) {
                if (LOGGING) {
                    System.out.println("Bad connection " + D + "<-->" + E + ".");
                }
                return false;
            }
            
            // --- finally, find deductions
            for (int i = 0; i <= 1; ++i) {
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
        return flat.getMapToCanonical().get(new Integer(1)).equals(new Integer(1));
    }

    /**
     * Finds the next symbol element with an undefined 3-neighbor, starting from
     * the given element.
     * 
     * @param element to start the search at.
     * @return the next unconnected element.
     */
    private Object nextFreeElement(final Object element) {
        int D = ((Integer) element).intValue();
        do {
            if (++D > this.size) {
                return null;
            }
        } while (this.current.definesOp(3, new Integer(D)));
        
        return new Integer(D);
    }

    /**
     * Computes signatures for the elements of a symbol. Two elements can only
     * be 3-neighbors if they have equal signatures.
     * 
     * @param ds the symbol to compute signatures for.
     * @return a map assigning signatures to the symbol's elements.
     */
    public static Map elementSignatures(final DelaneySymbol ds) {
        final Map signatures = new HashMap();
        final Set seen = new HashSet();

        for (final Iterator elms = ds.elements(); elms.hasNext();) {
            final Object D = elms.next();
            if (seen.contains(D)) {
                continue;
            }

            final int v = ds.v(0, 1, D);
            final List cuts0 = new LinkedList();
            final List cuts1 = new LinkedList();
            Object E = D;
            int r = 0;

            do {
                if (!ds.definesOp(0, E) || ds.op(0, E).equals(E)) {
                    cuts0.add(E);
                } else {
                    E = ds.op(0, E);
                }
                seen.add(E);
                if (!ds.definesOp(1, E) || ds.op(1, E).equals(E)) {
                    cuts1.add(E);
                } else {
                    E = ds.op(1, E);
                }
                seen.add(E);
                ++r;
            } while (!E.equals(D));

            if (cuts0.size() == 0 && cuts1.size() == 0) {
                final List sig = Arrays.asList(new Object[] { new Integer(0),
                        new Integer(r), new Integer(v), new Integer(0) });
                E = D;
                for (int i = 0; i < r; ++i) {
                    signatures.put(E, sig);
                    E = ds.op(0, E);
                    signatures.put(E, sig);
                    E = ds.op(1, E);
                }
            } else if (cuts0.size() == 2) {
                E = cuts0.get(0);
                Object F = cuts0.get(1);
                for (int i = 0; i < r / 2; ++i) {
                    final List sig = Arrays.asList(new Object[] { new Integer(1),
                            new Integer(r), new Integer(v), new Integer(i) });
                    signatures.put(E, sig);
                    signatures.put(F, sig);
                    E = ds.op(1 - i % 2, E);
                    F = ds.op(1 - i % 2, F);
                }
            } else if (cuts1.size() == 2) {
                E = cuts1.get(0);
                Object F = cuts1.get(1);
                for (int i = 0; i < r / 2; ++i) {
                    final List sig = Arrays.asList(new Object[] { new Integer(2),
                            new Integer(r), new Integer(v), new Integer(i) });
                    signatures.put(E, sig);
                    signatures.put(F, sig);
                    E = ds.op(i % 2, E);
                    F = ds.op(i % 2, F);
                }
            } else if (cuts0.size() == 1 && cuts1.size() == 1) {
                E = cuts0.get(0);
                for (int i = 0; i < r; ++i) {
                    final List sig = Arrays.asList(new Object[] { new Integer(3),
                            new Integer(r), new Integer(v), new Integer(i) });
                    signatures.put(E, sig);
                    E = ds.op(1 - i % 2, E);
                }
            } else {
                throw new RuntimeException("this cannot happen");
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
    public static Map componentMultiplicities(final DelaneySymbol ds) {
        final Map type2number = new HashMap();
        final List idcs = new IndexList(ds);
        for (final Iterator reps = ds.orbitRepresentatives(idcs); reps.hasNext();) {
            final DelaneySymbol sub = new Subsymbol(ds, idcs, reps.next()).canonical();
            if (type2number.containsKey(sub)) {
                type2number.put(sub, new Integer(((Integer) type2number.get(sub)).intValue() + 1));
            } else {
                type2number.put(sub, new Integer(1));
            }
        }
        return type2number;
    }
    
    /**
     * Takes a connected symbol and computes the first representative of each
     * equivalence class with respect to its automorphism group.
     * 
     * @param ds the symbol to use.
     * @return the list of first representatives.
     */
    public static List firstRepresentatives(final DelaneySymbol ds) {
        if (!ds.isConnected()) {
            throw new UnsupportedOperationException("symbol must be connected");
        }
        
        final List res = new LinkedList();
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
                    morphism = new Morphism(ds, ds, first, D);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                for (final Iterator iter = ds.elements(); iter.hasNext();) {
                    final Object E = iter.next();
                    classes.unite(E, morphism.get(E));
                }
            }

            final Set seen = new HashSet();
            for (final Iterator iter = ds.elements(); iter.hasNext();) {
                final Object D = iter.next();
                final Object E = classes.find(D);
                if (!seen.contains(E)) {
                    seen.add(E);
                    res.add(D);
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
    public static List subCanonicalForms(final DelaneySymbol ds) {
        if (!ds.isConnected()) {
            throw new UnsupportedOperationException("symbol must be connected");
        }
        if (!ds.hasStandardIndexSet()) {
            throw new UnsupportedOperationException("symbol must have indices 0..dim");
        }
        
        final List res = new LinkedList();
        
        final int size = ds.size();
        final int dim = ds.dim();
        final List idcs = new IndexList(ds);
        
        final List reps = firstRepresentatives(ds);
        for (final Iterator iter = reps.iterator(); iter.hasNext();) {
            final Object seed = iter.next();
            final Traversal trav = new Traversal(ds, idcs, seed, true);
            
            // --- elements will be numbered in the order they appear
            final HashMap old2new = new HashMap();

            // --- follow the traversal and assign the new numbers
            int nextE = 1;
            while (trav.hasNext()) {
                // --- retrieve the next edge
                final Edge e = (Edge) trav.next();
                final Object D = e.getElement();
                
                // --- determine a running number E for the target element D
                final Integer tmp = (Integer) old2new.get(D);
                if (tmp == null) {
                    // --- element D is encountered for the first time
                    old2new.put(D, new Integer(nextE));
                    ++nextE;
                }
            }
            
            // --- construct the new symbol
            int op[][] = new int[dim + 1][size + 1];
            int v[][] = new int[dim][size + 1];

            for (final Iterator elms = ds.elements(); elms.hasNext();) {
                final Object E = elms.next();
                final int D = ((Integer) old2new.get(E)).intValue();
                for (int i = 0; i <= dim; ++i) {
                    op[i][D] = ((Integer) old2new.get(ds.op(i, E))).intValue();
                    if (i < dim) {
                        v[i][D] = ds.v(i, i+1, E);
                    }
                }
            }
            
            // --- add it to the list
            res.add(new DSymbol(op, v));
        }
        
        // --- finis
        return res;
    }
}
