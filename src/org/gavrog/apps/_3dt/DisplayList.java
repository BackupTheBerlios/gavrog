/**
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


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.simple.NamedConstant;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.tilings.Tiling.Tile;

import buoy.event.EventSource;

/**
 * @author Olaf Delgado
 * @version $Id: DisplayList.java,v 1.11 2008/03/13 11:25:56 odf Exp $
 */
public class DisplayList extends EventSource implements
		Iterable<DisplayList.Item> {
	
	// --- the types of events that are produced by this class
	private static class EventType extends NamedConstant {
		protected EventType(String name) { super(name); }
	}
	public static EventType BEGIN = new EventType("Begin");
	public static EventType END = new EventType("End");
	public static EventType ADD = new EventType("Add");
	public static EventType DELETE = new EventType("Delete");
	public static EventType RECOLOR = new EventType("Recolor");
	
	// --- helper classes
	public static class Item {
		final private Tile tile;
		final private Vector shift;
		
		private Item(final Tile tile, final Vector shift) {
			this.tile = tile;
			this.shift = shift;
		}

		public Tile getTile() {
			return this.tile;
		}

		public Vector getShift() {
			return this.shift;
		}
		
		public int hashCode() {
			return this.tile.hashCode() * 37 + this.shift.hashCode();
		}
		
		public boolean equals(final Object arg) {
			final Item other = (Item) arg;
			return other.tile.equals(this.tile)
					&& other.shift.equals(this.shift);
		}
		
		public String toString() {
			final StringBuffer buf = new StringBuffer(40);
			buf.append("T");
			buf.append(getTile().getIndex());
			buf.append(" + ");
			buf.append(getShift());
			return buf.toString();
		}
	}
	
	public static class Event {
		final private EventType eventType;
		final private Item instance;
		final private Color oldColor;
		final private Color newColor;
		
		protected Event(final EventType type, final Item instance,
				final Color oldColor, final Color newColor) {
			this.eventType = type;
			this.instance = instance;
			this.oldColor = oldColor;
			this.newColor = newColor;
		}

		public Color getOldColor() {
			return this.oldColor;
		}

		public Color getNewColor() {
			return this.newColor;
		}

		public Item getInstance() {
			return this.instance;
		}

		public EventType getEventType() {
			return this.eventType;
		}
		
		public String toString() {
			final StringBuffer buf = new StringBuffer(100);
			buf.append(getEventType());
			buf.append(" ");
			buf.append(getInstance());
			buf.append(", ");
			buf.append(getOldColor());
			buf.append(", ");
			buf.append(getNewColor());
			return buf.toString();
		}
	}
	
	// --- fields
    final private Map<Item, Color> map = new HashMap<Item, Color>();
	
    // --- constructors
	public DisplayList() {
	}
	
	// --- shortcuts for dispatching events
	private void dispatchEvent(final EventType type, final Item inst,
			final Color oldColor, final Color newColor) {
		dispatchEvent(new Event(type, inst, oldColor, newColor));
	}
	
	private void dispatchEvent(final EventType type, final Item inst,
			final Color oldColor) {
		dispatchEvent(type, inst, oldColor, null);
	}
	
	private void dispatchEvent(final EventType type, final Item inst) {
		dispatchEvent(type, inst, null, null);
	}
	
	private void dispatchEvent(final EventType type) {
		dispatchEvent(type, null, null, null);
	}
	
	// --- primitive list modifications
	public Item add(final Tile tile, final Vector shift) {
		final Item inst = new Item(tile, shift);
		if (!this.map.containsKey(inst)) {
			this.map.put(inst, null);
			dispatchEvent(ADD, inst);
			return inst;
		} else {
			return null;
		}
	}

	public boolean remove(final Item item) {
		if (this.map.containsKey(item)) {
			final Color oldColor = color(item);
			this.map.remove(item);
			dispatchEvent(DELETE, item, oldColor);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean remove(final Tile tile, final Vector shift) {
		return remove(new Item(tile, shift));
	}

	public boolean recolor(final Item item, final Color newColor) {
		if (this.map.containsKey(item)) {
			final Color oldColor = color(item);
			this.map.put(item, newColor);
			dispatchEvent(RECOLOR, item, oldColor, newColor);
			return true;
		} else {
			return false;
		}
	}

	public boolean recolor(final Tile tile, final Vector shift,
			final Color color) {
		return recolor(new Item(tile, shift), color);
	}
	
	// --- slightly less primitive list modifications
	public Item addNeighbor(final Item item, final int face) {
		return addNeighbor(item.getTile(), item.getShift(), face);
	}

	public Item addNeighbor(final Tile tile, final Vector shift,
			final int face) {
		final Vector newShift = (Vector) shift.plus(tile.neighborShift(face));
		return add(tile.neighbor(face), newShift);
	}

	public boolean removeKind(final Item item) {
		final int kind = item.getTile().getKind();
		final List<Item> toRemove = new LinkedList<Item>();
		for (Item i: this) {
			if (i.getTile().getKind() == kind) {
				toRemove.add(i);
			}
		}
		if (toRemove.isEmpty()) {
			return false;
		} else {
			dispatchEvent(BEGIN);
			for (Item i: toRemove) {
				remove(i);
			}
			dispatchEvent(END);
			return true;
		}
	}
	
	public boolean removeAll() {
		final List<Item> toRemove = new LinkedList<Item>();
		for (Item i: this) {
			toRemove.add(i);
		}
		if (toRemove.isEmpty()) {
			return false;
		} else {
			dispatchEvent(BEGIN);
			for (Item i: toRemove) {
				remove(i);
			}
			dispatchEvent(END);
			return true;
		}
	}
	
	// --- list enquiries
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Item> iterator() {
		return this.map.keySet().iterator();
	}
	
	public int size() {
		return this.map.size();
	}
	
	public Color color(final Item inst) {
		return this.map.get(inst);
	}
}
