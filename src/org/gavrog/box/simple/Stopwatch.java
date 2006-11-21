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
 * @version $Id: Stopwatch.java,v 1.1 2006/11/21 22:46:56 odf Exp $
 */
public class Stopwatch {
    private long accumulated = 0;
    private long start = 0;
    private boolean isRunning;
    
    public void start() {
        if (this.isRunning) {
            throw new RuntimeException("already running");
        }
        this.start = System.nanoTime();
        this.isRunning = true;
    }
    
    public void stop() {
        if (!this.isRunning) {
            throw new RuntimeException("not running");
        }
        final long end = System.nanoTime();
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
        return this.accumulated;
    }
    
    public String format() {
        return elapsed() / (int) 1e7 / 100.0 + " seconds";
    }
}
