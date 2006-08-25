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

/**
 * Base class for move objects in generators.
 * 
 * @author Olaf Delgado
 * @version $Id: Move.java,v 1.1 2006/08/25 02:59:55 odf Exp $
 */
public class Move {
	final private boolean deduction;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param deduction true is this move is forced.
	 */
	public Move(final boolean deduction) {
		this.deduction = deduction;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (isDeduction()) {
			return "<deduction>";
		} else {
			return "<choice>";
		}
	}
	
	/**
	 * @return true if this move is forced by previous ones.
	 */
	public boolean isDeduction() {
		return this.deduction;
	}
}
