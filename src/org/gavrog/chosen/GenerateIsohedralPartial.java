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

package org.gavrog.chosen;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.generators.IsohedralPartial;


/**
 * Generates all isohedral tilings by a given tile, specified as a spherical 2d
 * Delaney symbol.
 * 
 * @author Olaf Delgado
 * @version $Id: GenerateIsohedralPartial.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */

public class GenerateIsohedralPartial {
    public static void main(final String[] args) {
        int verbosityLevel = 0;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].startsWith("-v")) {
                verbosityLevel = Integer.parseInt(args[i].substring(2));
            } else {
                System.err.println("Unknown option '" + args[i] + "'");
            }
            ++i;
        }
        
        final DSymbol ds = new DSymbol(args[i]);
        final IsohedralPartial iter = new IsohedralPartial(ds, verbosityLevel);

        try {
            while (iter.hasNext()) {
                final DSymbol out = (DSymbol) iter.next();
                System.out.println(out);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        
        System.err.println(iter.statistics());
        System.err.println("Options: "
                + (verbosityLevel <= 0 ? "quiet" : ("verbosity level " + verbosityLevel))
                + ".");
    }
}
