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


package org.gavrog.box.simple;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Delgado
 * @version $Id: TaskController.java,v 1.1 2007/03/26 23:49:22 odf Exp $
 */
public class TaskController {
    final static private Map controllers = new HashMap();
    
    final private Thread thread;
    private boolean cancelled = false;
    
    protected TaskController(final Thread thread) {
        this.thread = thread;
    }
    
    public synchronized static TaskController getInstance(final Thread thread) {
        if (!thread.isAlive()) {
            return null;
        }
        if (!controllers.containsKey(thread)) {
            controllers.put(thread, new TaskController(thread));
        }
        return (TaskController) controllers.get(thread);
    }
    
    public static TaskController getInstance() {
        return getInstance(Thread.currentThread());
    }
    
    public synchronized void cancel() {
        this.cancelled = true;
    }
    
    public synchronized void reset() {
        this.cancelled = false;
    }
    
    public synchronized void bailOutIfCancelled() {
        if (this.cancelled) {
            throw new TaskStoppedException(this.thread);
        }
    }
    
    public static boolean run(final Runnable task) {
        try {
            task.run();
            return true;
        } catch (final TaskStoppedException ex) {
            getInstance().reset();
            return false;
        } catch (final Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static void main(final String args[]) {
        for (int i = 0; i < 10; ++i) {
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    final boolean result = TaskController.run(new Runnable() {
                        long x = 0;
                        final TaskController cntrl = TaskController
                                .getInstance();

                        public void run() {
                            while (x < 1000000) {
                                try {
                                    cntrl.bailOutIfCancelled();
                                    ++x;
                                } catch (TaskStoppedException ex) {
                                    System.err.println("Stopped at x = " + x);
                                    throw ex;
                                }
                            }
                            System.err.println("Finished at x = " + x);
                        }
                    });
                    System.err.println("result = " + result);
                }
            });
            thread.start();
            final TaskController controller = TaskController
                    .getInstance(thread);
            try {
                Thread.sleep(0, 1);
                if (i < 9) {
                    controller.cancel();
                }
                thread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
