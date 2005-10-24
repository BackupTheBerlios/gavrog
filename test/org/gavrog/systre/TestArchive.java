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

package org.gavrog.systre;

import junit.framework.TestCase;

import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.systre.Archive.Entry;

/**
 * @author Olaf Delgado
 * @version $Id: TestArchive.java,v 1.1 2005/10/24 00:12:50 odf Exp $
 */
public class TestArchive extends TestCase {
    public void testEntryChecksum() {
        final String digest = "d01d26b1ad1122626f6c4c98415129f8";
        
        final String key = "3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 3 0 1 0 2 4 1 0 0 3 4 0 0 1";
        final Entry entry1 = new Entry(key, "1.0", "srs");
        assertEquals(digest, entry1.getDigestString());
        
        final PeriodicGraph srs = NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  1 2  0 0 0\n"
                + "  1 3  0 0 0\n"
                + "  1 4  0 0 0\n"
                + "  2 3  1 0 0\n"
                + "  2 4  0 1 0\n"
                + "  3 4  0 0 1\n"
                + "END\n");
        final Entry entry2 = new Entry(srs, "srs");
        assertEquals(digest, entry2.getDigestString());
    }
}
