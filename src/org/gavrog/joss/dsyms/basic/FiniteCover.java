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

package org.gavrog.joss.dsyms.basic;

import java.util.Iterator;

import org.gavrog.box.Iterators;


/**
 * A cover with finitely many layers, indexed by integers.
 * @author Olaf Delgado
 * @version $Id: FiniteCover.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public abstract class FiniteCover extends Cover {
	private int nLayers;
	
	public FiniteCover(DelaneySymbol base, int nLayers) {
		super(base);
		this.nLayers = nLayers;
	}
	
	public int numberOfLayers() {
		return nLayers;
	}
	
	public Iterator layers() {
		return Iterators.range(1, nLayers + 1);
	}
	
	public boolean hasLayer(Object layer) {
		if (layer instanceof Integer) {
			int i = ((Integer) layer).intValue();
			return i >= 1 && i <= nLayers;
		} else {
			return false;
		}
	}
}
