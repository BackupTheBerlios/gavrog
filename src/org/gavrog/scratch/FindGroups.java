/*
   Copyright 2009 Olaf Delgado-Friedrichs

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


package org.gavrog.scratch;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.tilings.Tiling;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class FindGroups {
	public static void main(final String args[]) {
		final DSymbol ds = new DSymbol("<1.1:3:1 2 3,2 3,1 3:8 4,3>");
		final String name =
			new SpaceGroupFinder(new Tiling(ds).getSpaceGroup()).getGroupName();
		System.out.println(
				String.format("Space group for symbol %s is %s.", ds, name));
	}
}
