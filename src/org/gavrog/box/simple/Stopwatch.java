/*
   Copyright 2006 Olaf Delgado-Friedrichs

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

/**
 * @author Olaf Delgado
 * @version $Id: Stopwatch.java,v 1.4 2007/05/12 06:23:55 odf Exp $
 */
public class Stopwatch {
    private long accumulated = 0;
    private long start = 0;
    private boolean isRunning;
    private static boolean java5 = System.getProperty("java.version")
			.startsWith("1.5");
    
    public void start() {
        if (this.isRunning) {
            throw new RuntimeException("already running");
        }
        this.start = time();
        this.isRunning = true;
    }
    
    private static long time() {
    	if (java5) {
    		return System.nanoTime();
    	} else {
    		return System.currentTimeMillis();
    	}
    }
    
    public void stop() {
        final long end = time();
        this.accumulated += end - this.start;
        this.isRunning = false;
    }
    
    public void reset() {
        if (this.isRunning) {
            throw new RuntimeException("cannot reset while running");
        }
        this.accumulated = 0;
    }
    
    public long elapsed() {
        if (this.isRunning) {
            throw new RuntimeException("cannot read while running");
        }
        if (java5) {
        	return this.accumulated / (int) 1e6;
        } else {
        	return this.accumulated;
        }
    }
    
    public String format() {
        return elapsed() / 10 / 100.0 + " seconds";
    }
}
