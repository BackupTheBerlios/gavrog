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

package org.gavrog.joss.pgraphs.io;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.pgraphs.io.GenericParser;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestGenericParser.java,v 1.1 2005/07/15 21:12:50 odf Exp $
 */
public class TestGenericParser extends TestCase {
    public void testParseBlock() {
        final StringReader empty = new StringReader(""
            + "VOID\n"
            + "END\n");
        final GenericParser emptyParser = new GenericParser(empty);
        assertEquals(0, emptyParser.parseBlock().length);
        assertEquals("void", emptyParser.lastBlockType());
        assertEquals(2, emptyParser.lastLineNumber());
        
        final StringReader test = new StringReader(""
            + "GARBAGE\n"
            + "  # this is a comment - test test test\n"
            + "  JUNK a \"1\" -1/2\n"
            + "\"JUNK\" asdf\n"
            + "TRASH 127 1.2e7 1.2i8\n"
            + "  VOID\n"
            + "  1/2.3\n"
            + "END\n");
        final Map synonyms = new HashMap();
        synonyms.put("trash", "junk");
        final GenericParser parser = new GenericParser(test);
        parser.setSynonyms(synonyms);
        final GenericParser.Entry block[] = parser.parseBlock();
        assertEquals(4, block.length);

        List row;

        assertEquals("junk", block[0].key);
        assertEquals(3, block[0].lineNumber);
        row = block[0].values;
        assertEquals(3, row.size());
        assertEquals("a", row.get(0));
        assertEquals("1", row.get(1));
        assertEquals(new Fraction(-1, 2), row.get(2));

        assertEquals("junk", block[1].key);
        assertEquals(4, block[1].lineNumber);
        row = block[1].values;
        assertEquals(2, row.size());
        assertEquals("JUNK", row.get(0));
        assertEquals("asdf", row.get(1));

        assertEquals("junk", block[2].key);
        assertEquals(5, block[2].lineNumber);
        row = block[2].values;
        assertEquals(3, row.size());
        assertEquals(new Whole(127), row.get(0));
        assertEquals(new FloatingPoint(1.2e7), row.get(1));
        assertEquals("1.2i8", row.get(2));

        assertEquals("void", block[3].key);
        assertEquals(7, block[3].lineNumber);
        row = block[3].values;
        assertEquals(1, row.size());
        assertEquals("1/2.3", row.get(0));
    }
}
