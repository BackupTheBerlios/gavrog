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


package org.gavrog.box.simple;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author Olaf Delgado
 * @version $Id: Stopwatch.java,v 1.6 2008/04/07 06:32:31 odf Exp $
 */
public class Stopwatch {
    private long accumulated = 0;
    private long start = 0;
    private boolean isRunning;

    private static ThreadMXBean tb;
    static {
    	try {
    		tb = ManagementFactory.getThreadMXBean();
    		tb.getCurrentThreadUserTime();
    	} catch (Throwable ex) {
    		tb = null;
    	}
    }
    
    private static long time() {
    	if (tb != null) {
    		final long t = tb.getCurrentThreadUserTime();
    		if (t < 0) {
    			return System.nanoTime();
    		} else {
    			return t;
    		}
    	} else {
    		return System.currentTimeMillis();
    	}
    }
    
    public void start() {
        if (!isRunning) {
        	start = time();
            isRunning = true;
        }
    }
    
    public void stop() {
		if (isRunning) {
			accumulated += time() - start;
			isRunning = false;
		}
	}
    
    public void reset() {
    	accumulated = 0;
    	if (isRunning) {
    		start = time();
    	}
    }
    
    /**
     * Reports the elapsed time on this timer in milliseconds.
     * @return the elapsed time in milliseconds.
     */
    public long elapsed() {
    	return (accumulated + (isRunning ? time() - start : 0))
				/ (tb != null ? (int) 1e6 : 0);
    }
    
    public String format() {
        return elapsed() / 10 / 100.0 + " seconds";
    }
}
