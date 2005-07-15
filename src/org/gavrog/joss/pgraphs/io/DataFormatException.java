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

/**
 * Exception thrown if some parsing method encounters an ill-formatted input
 * string.
 * 
 * @author Olaf Delgado
 * @version $Id: DataFormatException.java,v 1.1.1.1 2005/07/15 21:58:39 odf Exp $
 */
public class DataFormatException extends IllegalArgumentException {
    public DataFormatException(final String msg) {
        super(msg);
    }
}
