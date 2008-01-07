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


package org.gavrog.joss.geometry;

import java.util.HashSet;
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Whole;

/**
 * @author Olaf Delgado
 * @version $Id: CellCorrection.java,v 1.2 2008/01/07 06:30:50 odf Exp $
 */
public class CellCorrection {
	final private CoordinateChange coordinateChange;
	final private String groupName;
    
    public CellCorrection(final SpaceGroupFinder finder, final Matrix gram) {
		// --- extract some basic info
		final int dim = gram.numberOfRows();
		String name = finder.getGroupName();
		final CrystalSystem system = finder.getCrystalSystem();

		// --- check dimension
		if (dim != 3) {
			throw new UnsupportedOperationException("dimension must be 3");
		}

		// --- deal with trivial case quickly
		if (system != CrystalSystem.MONOCLINIC
				&& system != CrystalSystem.TRICLINIC) {
			this.coordinateChange = new CoordinateChange(Operator.identity(dim));
		} else {

			// --- the cell vectors in the original coordinate system
			final CoordinateChange fromStd = (CoordinateChange) finder
					.getToStd().inverse();
			final Vector a = (Vector) Vector.unit(3, 0).times(fromStd);
			final Vector b = (Vector) Vector.unit(3, 1).times(fromStd);
			final Vector c = (Vector) Vector.unit(3, 2).times(fromStd);

			// --- old and new cell vectors
			final Vector from[] = new Vector[] { a, b, c };
			final Vector to[];

			if (system == CrystalSystem.TRICLINIC) {
				to = Lattices.reducedLatticeBasis(from, gram);
			} else { // Monoclinic case

				// --- little helper class
				final class NameSet extends HashSet {
					public NameSet(final String names[]) {
						super();
						for (int i = 0; i < names.length; ++i) {
							this.add(names[i]);
						}
					}
				}

				// --- no centering, no glide
				final Set type1 = new NameSet(new String[] {
						"P121", "P1211", "P1m1", "P12/m1", "P121/m1" });
				// --- glides, no centering
				final Set type2 = new NameSet(new String[] {
						"P1c1", "P12/c1", "P121/c1" });
				// --- centering, no glide
				final Set type3 = new NameSet(new String[] {
						"C121", "C1m1", "C12/m1" });
				// --- both glide and centering
				final Set type4 = new NameSet(new String[] { "C1c1", "C12/c1" });

				// --- find the smallest vectors orthogonal to b
				final Vector old[] = new Vector[] { a, c };
				final Vector nu[] = Lattices.reducedLatticeBasis(old, gram);
				to = new Vector[] { nu[0], b, nu[1] };
				if (Vector.dot(to[0], to[2], gram).isPositive()) {
					to[2] = (Vector) to[2].negative();
				}
				
				// --- figure out if the group setting changed
				final CoordinateChange F = new CoordinateChange(Vector
						.toMatrix(from));
				final CoordinateChange T = new CoordinateChange(Vector
						.toMatrix(to));
				final CoordinateChange C = (CoordinateChange) F.inverse()
						.times(T);

				final Vector g = ((Vector) new Vector(0, 0, 1).dividedBy(
						new Whole(2)).times(C)).modZ();
				final boolean glides_x = !g.get(0).isZero();
				final boolean glides_z = !g.get(2).isZero();
				final Vector cen = ((Vector) new Vector(1, 0, 0).dividedBy(
						new Whole(2)).times(C)).modZ();
				final boolean centers_x = !cen.get(0).isZero();
				final boolean centers_z = !cen.get(2).isZero();

				if (type1.contains(name)) {
					// --- nothing to do
				} else if (type2.contains(name)) {
					if (glides_x) {
						if (glides_z) {
							name = name.replace('c', 'n');
						} else {
							final Vector tmp = to[0];
							to[0] = to[2];
							to[2] = tmp;
							to[1] = (Vector) to[1].negative();
						}
					}
				} else if (type3.contains(name)) {
					if (centers_z) {
						if (centers_x) {
							name = name.replace('C', 'I');
						} else {
							final Vector tmp = to[0];
							to[0] = to[2];
							to[2] = tmp;
							to[1] = (Vector) to[1].negative();
						}
					}
				} else if (type4.contains(name)) {
					boolean swap = false;
					if (centers_z) {
						if (centers_x) {
							name = name.replace('C', 'I').replace('c', 'a');
							swap = glides_z;
						} else {
							swap = true;
						}
					}
					if (swap) {
						final Vector tmp = to[0];
						to[0] = to[2];
						to[2] = tmp;
						to[1] = (Vector) to[1].negative();
					}
				} else {
					final String msg = "Cannot handle monoclinic space group "
							+ name + ".";
					throw new RuntimeException(msg);
				}
				if (Vector.volume3D(to[0], to[1], to[2]).isNegative()) {
					to[1] = (Vector) to[1].negative();
				}
			}

			final CoordinateChange F = new CoordinateChange(Vector
					.toMatrix(from));
			final CoordinateChange T = new CoordinateChange(Vector.toMatrix(to));
			this.coordinateChange = (CoordinateChange) F.inverse().times(T);
		}
		this.groupName = name;
	}

	public CoordinateChange getCoordinateChange() {
		return this.coordinateChange;
	}

	public String getGroupName() {
		return this.groupName;
	}
}
