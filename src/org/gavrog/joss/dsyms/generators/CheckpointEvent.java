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


package org.gavrog.joss.dsyms.generators;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class CheckpointEvent {
	final private CheckpointSource source;
	final private boolean old;
	
	public CheckpointEvent(final CheckpointSource source, final boolean old) {
		this.source = source;
		this.old = old;
	}

	public CheckpointSource getSource() {
		return source;
	}

	public String getCheckpoint() {
		return getSource().getCheckpoint();
	}
	
	public boolean isOld() {
		return old;
	}
}
