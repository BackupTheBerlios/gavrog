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
 * @version $Id: Kelvin.java,v 1.6 2008/04/12 09:40:45 odf Exp $
 */
public class Kelvin {
	final static private List iTiles = new IndexList(0, 1, 2);
	final static private List iFaces2d = new IndexList(0, 1);
	
	public static class Generator extends FrankKasperExtended {
		final Writer output;
		final boolean countOnly;
		final int start;
		final int stop;
		int count = 0;
		final Stopwatch timer = new Stopwatch();
		final int interval;
		
		public Generator(final int k,
				         final boolean verbose,
				         final boolean testParts,
				         final boolean countOnly,
				         final int start, final int stop,
				         final int checkpointInterval,
				         final Writer output) {
			super(k, verbose, testParts);
			this.countOnly = countOnly;
			this.start = start;
			this.stop = stop;
			this.interval = checkpointInterval * 1000;
			this.output = output;
			this.timer.start();
		}
		
		protected boolean partsListOkay(final List parts) {
			final boolean ok = super.partsListOkay(parts);
			if (ok) {
				++count;
			}
			if (countOnly) {
				return false;
			}
			return ok && (start <= count) && (stop <= 0 || stop > count);
		}
		
		public int getCount() {
			return count;
		}
		
		protected void handleCheckpoint() {
			this.timer.stop();
			if (interval > 0 && this.timer.elapsed() > interval) {
				this.timer.reset();
				writeCheckpoint();
			}
			this.timer.start();
		}
		
		public void writeCheckpoint() {
			try {
				output.write(String.format("#@ CHECKPOINT %s\n",
						getCheckpoint()));
			} catch (Throwable ex) {
			}
		}
	}
	
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
	
	private static String info(final DSymbol ds) {
		final StringBuffer buf = new StringBuffer(50);
		final DSymbol cover = Covers.pseudoToroidalCover3D(ds);
		final int count[] = new int[5];
		int nt = 0;
		int sum_nf = 0;
		int sum_nfsqr = 0;
		for (final Iterator reps = cover.orbitReps(iTiles); reps.hasNext();) {
			final Object D = reps.next();
			final DSymbol tile = new DSymbol(new Subsymbol(cover, iTiles, D));
			final int k = tile.numberOfOrbits(iFaces2d);
			if (k < 12) {
				return "found tile with less than 12 faces";
			}
			if (k > 16) {
				return "found tile with more than 16 faces";
			}
			++count[k-12];
			sum_nf += k;
			sum_nfsqr += k * k;
			++nt;
		}
		buf.append("composition: [");
		for (int i = 0; i < 5; ++i) {
			buf.append(' ');
			buf.append(count[i] > 0 ? String.valueOf(count[i]) : "-");
		}
		final double avg_nf = sum_nf / (double) nt;
		buf.append(" ]   <f> = ");
		buf.append(avg_nf);
		buf.append("   stdev = ");
		buf.append(Math.sqrt(sum_nfsqr / (double) nt - avg_nf * avg_nf));
		return buf.toString();
	}
	
	public static void main(final String[] args) {
		try {
			boolean verbose = false;
			boolean testParts = true;
			boolean check = true;
			int start = 0;
			int stop = 0;
			int section = 0;
			int nrOfSections = 0;
			int checkpointInterval = 3600;
			
			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equals("-v")) {
					verbose = !verbose;
				} else if (args[i].equals("-p")) {
					testParts = !testParts;
				} else if (args[i].equals("-e")) {
					check = !check;
				} else if (args[i].equals("-s")) {
					final String tmp[] = args[++i].split("/");
					section = Integer.parseInt(tmp[0]);
					nrOfSections = Integer.parseInt(tmp[1]);
					if (nrOfSections < 1) {
						String msg = "Number of sections must be positive.";
						System.err.println(msg);
						System.exit(1);
					} else if (section < 1 || section > nrOfSections) {
						String msg = "Section # must be between 1 and "
								+ nrOfSections;
						System.err.println(msg);
						System.exit(1);
					}
				} else if (args[i].equals("-i")) {
					checkpointInterval = Integer.parseInt(args[++i]);
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
			final Stopwatch eTestTimer = new Stopwatch();
			int n = 0;
			timer.start();
			
			if (nrOfSections > 0) {
				final Generator tmp = new Generator(k, verbose, testParts,
						true, 0, 0, 0, null);
				while (tmp.hasNext()) {
					tmp.next();
				}
				n = tmp.getCount();
				final int m = nrOfSections;
				final int s = section;
				start = (int) Math.round(n / (double) m * (s-1)) + 1;
				stop  = (int) Math.round(n / (double) m * s) + 1;
			}
			
			output.write("# Program Kelvin with k = " + k + ".\n");
			output.write("# Options:\n");
			output.write("#     verbose mode:                    ");
			output.write((verbose ? "on" : "off") + "\n");
			output.write("#     vertex symmetries pre-filtering: ");
			output.write((testParts ? "on" : "off") + "\n");
			output.write("#     euclidicity test:                ");
			output.write((check ? "on" : "off") + "\n");
			if (nrOfSections > 0) {
				output.write("#    running section " + section + " of "
						+ nrOfSections + " (cases " + start + " to "
						+ (stop - 1) + " of " + n + ").\n");
			}
			if (checkpointInterval > 0) {
				output.write("#     checkpoint interval:             ");
				output.write(checkpointInterval + "sec\n");
			} else {
				output.write("#     checkpoints:                     off\n");
			}
			output.write("\n");
			
			final Generator iter = new Generator(k, verbose, testParts,
					false, start, stop, checkpointInterval, output);

			while (iter.hasNext()) {
				final DSymbol out = ((DSymbol) iter.next()).dual();
				if (allTileSizesBetween(out, 12, 16)) {
					++countTileSizeOk;
					if (check) {
						eTestTimer.start();
						EuclidicityTester tester = new EuclidicityTester(out);
						final boolean bad = tester.isBad();
						final boolean ambiguous = tester.isAmbiguous();
						eTestTimer.stop();
						if (!bad) {
							iter.writeCheckpoint();
							if (ambiguous) {
								output.write("#@ name euclidicity dubious\n");
								++countAmbiguous;
							}
							output.write("# " + info(out) + "\n");
							output.write(out + "\n");
							++countGood;
						}
					} else {
						output.write(out + "\n");
					}
					output.flush();
				}
			}
			timer.stop();

			output.write("\n");
			output.write("# Total execution time in user mode was "
					+ timer.format() + ".\n");
			if (check) {
				output.write("# Time for euclidicity tests was "
						+ eTestTimer.format() + ".\n");
			}
			output.write("\n");
			output.write("# " + iter.statistics() + "\n");
			output.write("# Of the latter, " + countTileSizeOk
					+ " had between 12 and 16 faces in each tile.\n");
			if (check) {
				output.write("# Of those, " + countGood
						+ " were found euclidean.\n");
				if (countAmbiguous > 0) {
					output.write("# For " + countAmbiguous
							+ " symbols, euclidicity remains undetermined.\n");
				}
			}
			output.flush();
		} catch (final Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
