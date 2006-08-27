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
import org.gavrog.box.simple.NamedConstant;

/**
 * Abstract base class for generators, defining the basic branch-and-cut strategy.
 * 
 * @author Olaf Delgado
 * @version $Id: Generate.java,v 1.3 2006/08/27 02:44:57 odf Exp $
 */
public abstract class Generate extends IteratorAdapter {
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
	
	/* (non-Javadoc)
	 * @see org.gavrog.box.collections.IteratorAdapter#findNext()
	 */
	protected Object findNext() throws NoSuchElementException {
        log("entering findNext(): stack size = " + this.stack.size());
        while (true) {
            final Move decision = undoLastDecision();
            if (decision == null) {
            	log("leaving findNext(): no more decisions to undo");
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
                if (isCanonical()) {
                    if (isComplete()) {
                    	log("leaving findNext(): result is complete");
                    	return makeResult();
                    } else {
                        log("  result is incomplete");
                        this.stack.addLast(nextChoice());
                    }
                } else {
                    log("  result is not canonical");
                }
            } else {
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

            // --- perform the move if possible
            final Status status = performMove(move);

            // --- a void move has no consequences
            if (status == Status.VOID) {
            	continue;
            }
            
            // --- if the move was illegal, return immediately
            if (status == Status.ILLEGAL) {
				log("move " + move + " is impossible; backtracking");
				return false;
			}
            
            // --- record the move
        	this.stack.addLast(move);
            
            // --- finally, find and enqueue deductions
            final List deductions = deductions(move);
            for (final Iterator iter = deductions.iterator(); iter.hasNext();) {
            	queue.addLast(iter.next());
            }
        }
        
		return true;
	}

	private Move undoLastDecision() {
        while (stack.size() > 0) {
            final Move last = (Move) this.stack.removeLast();
            
            log("Undoing " + last);
            undoMove(last);

            if (!last.isDeduction()) {
            	return last;
            }
        }
        return null;
	}
	
	abstract protected Move nextChoice();

	abstract protected Move nextDecision(final Move previous);

	abstract protected Status performMove(final Move move);

	abstract protected void undoMove(final Move move);

	abstract protected List deductions(final Move move);

	abstract protected boolean isComplete();

	abstract protected boolean isCanonical();

	abstract protected Object makeResult();
}
