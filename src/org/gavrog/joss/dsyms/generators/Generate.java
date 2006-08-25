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

import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.joss.dsyms.generators.ExtendTo3d.Move;

/**
 * Abstract base class for generators, defining the basic branch-and-cut strategy.
 * 
 * @author Olaf Delgado
 * @version $Id: Generate.java,v 1.1 2006/08/25 02:59:55 odf Exp $
 */
public abstract class Generate extends IteratorAdapter {
	// --- set to true to enable logging
	final private static boolean LOGGING = false;
	
	// --- the current partial generation result
	final private Object current = null;
	
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
            final Move choice = undoLastChoice();
            if (choice == null) {
            	log("leaving findNext(): no more choices to undo");
                throw new NoSuchElementException();
            } else {
            	log("  last choice was " + choice);
            	log("  current state:\n" + this.current);
            }
			
            final Move move = nextMove(choice);
            if (move == null) {
                log("  no potential move");
                continue;
            } else {
            	log("  found potential move " + move);
            }
            
            if (performMove(move)) {
                log("  result of move:\n" + this.current);
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

	protected boolean performMove(Move move) {
		//TODO implement this
		return false;
	}

	protected Move undoLastChoice() {
		//TODO implement this
		return null;
	}
	
	abstract protected Object nextChoice();

	abstract protected Move nextMove(Move choice);

	abstract protected Object makeResult();

	abstract protected boolean isComplete();

	abstract protected boolean isCanonical();
}
