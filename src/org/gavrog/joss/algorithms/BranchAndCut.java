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

package org.gavrog.joss.algorithms;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.simple.NamedConstant;

/**
 * Abstract base class for generators, defining the basic branch-and-cut
 * strategy.
 * 
 * @author Olaf Delgado
 * @version $Id: BranchAndCut.java,v 1.5 2006/09/26 22:25:56 odf Exp $
 */
public abstract class BranchAndCut extends IteratorAdapter {
	// --- set to true to enable logging
	final private static boolean LOGGING = false;

	public static class Status extends NamedConstant {
		// --- move was performed okay
		final public static Status OK = new Status("ok");

		// --- void move; changes nothing
		final public static Status VOID = new Status("void");

		// --- move contradicts current state
		final public static Status ILLEGAL = new Status("illegal");

		private Status(final String name) {
			super(name);
		}
	}

	// --- flag set when generation is done
	private boolean done = false;

	// --- the generation history
	final private LinkedList stack = new LinkedList();

	/**
	 * If logging is enabled, print a message to the standard error stream.
	 * 
	 * @param text the message to print.
	 */
	protected void log(final String text) {
		if (LOGGING) {
			System.err.println(text);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
	 */
	protected Object findNext() throws NoSuchElementException {
		if (done) {
			throw new NoSuchElementException();
		}

		log("\nentering findNext(): stack size = " + this.stack.size());

		if (stack.size() == 0) {
			final Move choice = nextChoice(null);
			log("  adding initial choice " + choice);
			this.stack.addLast(choice);
		}

		while (true) {
			final Move decision = undoLastDecision();
			if (decision == null) {
				log("leaving findNext(): no more decisions to undo");
				this.done = true;
				throw new NoSuchElementException();
			} else {
				log("  last decision was " + decision);
			}

			final Move move = nextDecision(decision);
			if (move == null) {
				log("  no potential move");
				continue;
			} else {
				log("  found potential move " + move);
			}

			if (performMoveAndDeductions(move)) {
				if (isValid()) {
					final Object result = makeResult();
					if (result != null) {
						log("leaving findNext() with result " + result);
						return result;
					} else {
						log("  result is incomplete");
					}
					final Move choice = nextChoice(move);
					log("  adding choice " + choice);
					this.stack.addLast(choice);
				} else {
					log("  result or move is not valid");
				}
			} else {
				stack.addLast(move);
				log("  move was rejected");
			}
		}
	}

	private boolean performMoveAndDeductions(final Move initial) {
		// --- we maintain a queue of deductions, starting with the initial move
		final LinkedList queue = new LinkedList();
		queue.addLast(initial);

		while (queue.size() > 0) {
			// --- get the next move from the queue
			final Move move = (Move) queue.removeFirst();

			// --- see if the move can be performed
			final Status status = checkMove(move);

			// --- a void move has no consequences
			if (status == Status.VOID) {
				continue;
			}

			// --- if the move was illegal, return immediately
			if (status == Status.ILLEGAL) {
				log("    move " + move + " is impossible; backtracking");
				return false;
			}

			// --- perform and record the move
			performMove(move);
			this.stack.addLast(move);

			// --- finally, find and enqueue deductions
			final List deductions = deductions(move);
			if (deductions != null) {
				for (final Iterator iter = deductions.iterator(); iter.hasNext();) {
					queue.addLast(iter.next());
				}
			}
		}

		return true;
	}

	private Move undoLastDecision() {
		while (stack.size() > 0) {
			final Move last = (Move) this.stack.removeLast();
			if (last == null) {
				continue;
			}

			log("  undoing " + last);
			undoMove(last);

			if (!last.isDeduction()) {
				return last;
			}
		}
		return null;
	}

	// --- The following methods have to implemented by every derived class:

	/**
	 * Constructs a {@link Move} object which describes the next choice to make
	 * given the current state.
	 * 
	 * @param previous the last decision made (<code>null</code> at start).
	 * @return the next choice to make.
	 */
	abstract protected Move nextChoice(final Move previous);

	/**
	 * Constructs a {@link Move} object which describes the next possible way to
	 * decide upon the given choice.
	 * 
	 * @param previous the current choice or the last decision regarding that choice.
	 * @return the next decision for the given choice.
	 */
	abstract protected Move nextDecision(final Move previous);

	/**
	 * Returns the status of the given move: OK if it can be performed as
	 * requested, VOID if it would not change the current state, and ILLEGAL if
	 * it conflicts with the current state.
	 * 
	 * @return the status of the move (OK, VOID or ILLEGAL).
	 */
	abstract protected Status checkMove(final Move move);

	/**
	 * Performs the given move which must have been established as legal and
	 * non-void by calling {@link #checkMove(Move)}.
	 * 
	 * @param move the move to perform.
	 */
	abstract protected void performMove(final Move move);

	/**
	 * Undo the given move under the assumption that it was the last move
	 * performed on the path to the current state.
	 * 
	 * @param move
	 *            the move to undo.
	 */
	abstract protected void undoMove(final Move move);

	/**
	 * Determine forced moves (deductions) based on the current state and the
	 * last move performed to reach it.
	 * 
	 * @param move the last move performed.
	 * @return the list of forced moves.
	 */
	abstract protected List deductions(final Move move);

	/**
	 * Implements a final test of the current state after a decision move and a
	 * possible series of deductions. This might, for example, check if the
	 * current state describes a partial result in canonical form, or implement
	 * any other tests that would be too costly to be performed after every
	 * elementary move.
	 * 
	 * @return true if the current state is well-formed.
	 */
	abstract protected boolean isValid();

	/**
	 * Constructs an output object based on the current state or <code>null</code> if
	 * the current state does not describe a complete result.
	 * 
	 * @return the result of the current state.
	 */
	abstract protected Object makeResult();
}
