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
import org.gavrog.jane.numbers.IArithmetic;

/**
 * @author Olaf Delgado
 * @version $Id: CellCorrection.java,v 1.1 2008/01/07 02:02:29 odf Exp $
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

				// --- no centering, no glide, both a and c are free
				final Set type1 = new NameSet(new String[] { "P121", "P1211",
						"P1m1", "P12/m1", "P121/m1" });
				// --- no centering, a is free
				final Set type2 = new NameSet(new String[] { "P1c1", "P12/c1",
						"P121/c1" });
				// --- no glide, c is free
				final Set type3 = new NameSet(new String[] { "C121", "C1m1",
						"C12/m1" });
				// --- both glide and centering, only signs are free
				final Set type4 = new NameSet(new String[] { "C1c1", "C12/c1" });

				if (type1.contains(name)) {
					// --- find a reduced basis for the lattice spanned by a and c
					final Vector old[] = new Vector[] { a, c };
					final Vector nu[] = Lattices.reducedLatticeBasis(old, gram);
					to = new Vector[] { nu[0], b, nu[1] };
				} else if (type2.contains(name)) {
					// --- keep c and use shortest sum of a and multiples of c for a
					final IArithmetic t = Vector.dot(a, c, gram).dividedBy(
							Vector.dot(c, c, gram)).round();
					final Vector new_a = (Vector) a.minus(t.times(c));
					if (Vector.dot(new_a, c, gram).isPositive()) {
						to = new Vector[] { (Vector) new_a.negative(), b, c };
					} else {
						to = new Vector[] { new_a, b, c };
					}
				} else if (type3.contains(name)) {
					// --- keep a and use shortest sum of c and multiples of a for c
					final IArithmetic t = Vector.dot(a, c, gram).dividedBy(
							Vector.dot(a, a, gram)).round();
					final Vector new_c = (Vector) c.minus(t.times(a));
					if (Vector.dot(a, new_c, gram).isPositive()) {
						to = new Vector[] { a, b, (Vector) new_c.negative() };
					} else {
						to = new Vector[] { a, b, new_c };
					}
				} else if (type4.contains(name)) {
					// --- must keep all old vectors
					to = new Vector[] { a, b, c };
				} else {
					final String msg = "Cannot handle monoclinic space group "
							+ name + ".";
					throw new RuntimeException(msg);
				}

				if (Vector.dot(to[0], to[2], gram).isPositive()) {
					to[2] = (Vector) to[2].negative();
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
