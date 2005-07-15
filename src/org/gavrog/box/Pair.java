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

package org.gavrog.box;

/**
 * A class for hashable pairs of objects.
 * @author delgado
 * @version $Id: Pair.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public class Pair {
	private Object first, second;
	
	public Pair(Object first, Object second) {
		this.first = first;
		this.second = second;
	}
	
	public Object getFirst() {
		return first;
	}
	
	public Object getSecond() {
		return second;
	}
	
	public boolean equals(Object other) {
		boolean good = false;
		if (other instanceof Pair) {
			Pair p = (Pair) other;
			if (first == null) {
				good = (p.first == null);
			} else {
				good = first.equals(p.first);
			}
			if (good) {
				if (second == null) {
					good = (p.second == null);
				} else {
					good = second.equals(p.second);
				}
			}
		}
		return good;
	}
	
	public int hashCode() {
	    int a = first == null ? 0 : first.hashCode();
	    int b = second == null ? 0 : second.hashCode();
	    return a * 37 + b;
	}
	
	public String toString() {
		return "("  + first + ", " + second + ")";
	}
}
