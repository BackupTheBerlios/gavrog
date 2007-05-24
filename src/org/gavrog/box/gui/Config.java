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
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olaf Delgado
 * @version $Id: Config.java,v 1.4 2007/05/24 23:17:51 odf Exp $
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
	
	public static Class wrapperType(final Class type) {
		if (mappedTypes.containsKey(type)) {
			return (Class) mappedTypes.get(type);
		} else {
			return type;
		}
	}
	
	public static Object construct(final Class type, final String value)
			throws Exception {
		return wrapperType(type).getConstructor(new Class[] { String.class })
				.newInstance(new Object[] { value });
	}

	public static String asString(final Object value) throws Exception {
		final Class type = value.getClass();
		final Class wrapperType = wrapperType(type);
		if (wrapperType.equals(type)) {
			return String.valueOf(value);
		} else {
			return String.valueOf(wrapperType(type).getConstructor(
					new Class[] { type }).newInstance(new Object[] { value }));
		}
	}
	
	public static void setProperties(final Object target, final Properties input)
			throws Exception {
		final Class type = target.getClass();
		final String prefix = type.getCanonicalName() + ".";
		final BeanInfo info = Introspector.getBeanInfo(type);
		final PropertyDescriptor props[] = info.getPropertyDescriptors();
		for (int i = 0; i < props.length; ++i) {
			if (props[i].getWriteMethod() == null) {
				continue;
			}
			final String value = input.getProperty(prefix + props[i].getName());
			if (value == null) {
				continue;
			}
			final Method setter = props[i].getWriteMethod();
            final Class valueType = setter.getParameterTypes()[0];
			setter.invoke(target, new Object[] { construct(valueType, value) });
		}
	}

	public static Properties getProperties(final Object source)
			throws Exception {
		final Class type = source.getClass();
		final String prefix = type.getCanonicalName() + ".";
		final BeanInfo info = Introspector.getBeanInfo(type);
		final Properties result = new Properties();
		final PropertyDescriptor props[] = info.getPropertyDescriptors();
		for (int i = 0; i < props.length; ++i) {
            if (props[i].getWriteMethod() == null) {
                continue;
            }
            final Method getter = props[i].getReadMethod();
            final Object value = getter.invoke(source, new Object[] {});
            result.setProperty(prefix + props[i].getName(), asString(value));
        }
		return result;
	}
	
	public static PropertyDescriptor namedProperty(final Object source,
			final String name) throws Exception {
		final Class type = (source instanceof Class ? (Class) source : source
				.getClass());
		final BeanInfo info = Introspector.getBeanInfo(type);
		final PropertyDescriptor props[] = info.getPropertyDescriptors();
		PropertyDescriptor prop = null;
		for (int i = 0; i < props.length; ++i) {
			if (props[i].getName().equals(name)) {
				prop = props[i];
				break;
			}
		}
		return prop;
	}
}
