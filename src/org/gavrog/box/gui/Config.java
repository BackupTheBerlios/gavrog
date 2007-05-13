/*
   Copyright 2007 Olaf Delgado-Friedrichs

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


package org.gavrog.box.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Delgado
 * @version $Id: Config.java,v 1.1 2007/05/13 07:55:29 odf Exp $
 */
public class Config {
	final private static Map mappedTypes = new HashMap();
	static {
		mappedTypes.put(int.class, Integer.class);
		mappedTypes.put(long.class, Long.class);
		mappedTypes.put(float.class, Float.class);
		mappedTypes.put(double.class, Double.class);
		mappedTypes.put(Color.class, ColorWrapper.class);
		mappedTypes.put(boolean.class, Boolean.class);
	}
	
	public static Class wrapperType(final Class cl) {
		if (mappedTypes.containsKey(cl)) {
			return (Class) mappedTypes.get(cl);
		} else {
			return cl;
		}
	}
}
