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


package org.gavrog.joss.dsyms.generators;

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

/**
 * @author Olaf Delgado
 * @version $Id: Kelvin.java,v 1.2 2008/04/02 12:16:25 odf Exp $
 */
public class Kelvin {
	final static private List iTiles = new IndexList(0, 1, 2);
	final static private List iFaces2d = new IndexList(0, 1);
	
	private static boolean allTileSizesBetween(final DSymbol ds, final int min,
			final int max) {
		for (final Iterator reps = ds.orbitReps(iTiles); reps.hasNext();) {
			final Object D = reps.next();
			final DSymbol tile = new DSymbol(new Subsymbol(ds, iTiles, D));
			final DSymbol cover = Covers.finiteUniversalCover(tile);
			final int n = cover.numberOfOrbits(iFaces2d);
			if (n < min || n > max) {
				return false;
			}
		}
		return true;
	}
	
	public static void main(final String[] args) {
		try {
			boolean verbose = false;
			boolean check = true;
			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equals("-v")) {
					verbose = !verbose;
				} else if (args[i].equals("-e")) {
					check = !check;
				} else {
					System.err.println("Unknown option '" + args[i] + "'");
				}
				++i;
			}

			final int k = Integer.parseInt(args[i]);
			final Writer output;
			if (args.length > i + 1) {
				output = new FileWriter(args[i + 1]);
			} else {
				output = new OutputStreamWriter(System.out);
			}

			int countTileSizeOk = 0;
			int countGood = 0;
			int countAmbiguous = 0;

			final Stopwatch timer = new Stopwatch();
			timer.start();
			final TileKTransitive iter = new FrankKasperExtended(k, verbose, true);

			while (iter.hasNext()) {
				final DSymbol out = ((DSymbol) iter.next()).dual();
				if (allTileSizesBetween(out, 12, 16)) {
					++countTileSizeOk;
					if (check) {
						final EuclidicityTester tester = new EuclidicityTester(
								out);
						if (tester.isAmbiguous()) {
							output.write("#@ name euclidicity dubious\n");
							++countAmbiguous;
						}
						if (!tester.isBad()) {
							output.write(out + "\n");
							++countGood;
						}
					} else {
						output.write(out + "\n");
					}
				}
			}
			output.flush();
			timer.stop();

			output.write("\n# Program Kelvin with k = " + k + "\n");
			output.write("# Options: " + (check ? "" : "no")
					+ " euclidicity check, " + (verbose ? "verbose" : "quiet")
					+ ".\n");
			output.write("# Running time was " + timer.format() + "\n\n");
			output.write("# " + iter.statistics() + "\n");
			output.write("# Of the latter, " + countTileSizeOk
					+ " had between 12 and 16 faces in each tile.\n");
			if (check) {
				output.write("# Of those, " + countGood
						+ " were found euclidean.\n");
				if (countAmbiguous > 0) {
					output.write("# For " + countAmbiguous
						+ " symbols, euclidicity could not yet be decided.\n");
				}
			}
			output.flush();
		} catch (final Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
