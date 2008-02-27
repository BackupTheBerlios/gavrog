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

package org.gavrog.joss.algorithms;

import org.gavrog.box.simple.NamedConstant;

/**
 * Base class for move objects in generators.
 * 
 * @author Olaf Delgado
 * @version $Id: Move.java,v 1.2 2008/02/27 08:14:27 odf Exp $
 */
public class Move {
	public static class Type extends NamedConstant {
		// --- no actual move, just indicates a choice to be made
		final public static Type CHOICE = new Type("Choice");
		
		// --- a decision made upon a choice
		final public static Type DECISION = new Type("Decision");
		
		// --- a consequence of previous decisions
		final public static Type DEDUCTION = new Type("Deduction");
		
		private Type(final String name) {
			super(name);
		}
	}
	
	// --- the type of move
	final private Type type;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param type the type of this move
	 */
	public Move(final Type type) {
		this.type = type;
	}

	/**
	 * @return true if this is no actual move but indicates a choice to be made.
	 */
	public boolean isChoice() {
		return this.type == Type.CHOICE;
	}
	
	/**
	 * @return true if this move is a decision made upon a choice.
	 */
	public boolean isDecision() {
		return this.type == Type.DECISION;
	}
	
	/**
	 * @return true if this move is a consequence of previous decisions.
	 */
	public boolean isDeduction() {
		return this.type == Type.DEDUCTION;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "<" + this.type + ">";
	}
}
