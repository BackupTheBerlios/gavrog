package org.gavrog.joss.pgraphs.io;

import java.util.HashMap;
import java.util.Map;

import org.gavrog.joss.pgraphs.basic.IGraphElement;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * Encapsulates a net with extra information picked up by the parser.
 */
public class Net extends PeriodicGraph {
	final private String name;
	final private String givenGroup;
	final private Map nodeToName = new HashMap();
	
	public Net(final int dim, final String name, final String group) {
		super(dim);
		this.name = name;
		this.givenGroup = group;
	}

	public Net(final PeriodicGraph graph, final String name, final String group) {
		super(graph);
		this.name = name;
		this.givenGroup = group;
	}

	public String getGivenGroup() {
		return givenGroup;
	}

	public String getName() {
		return name;
	}
	
	public String getNodeName(final INode v) {
		assert(this.hasElement(v));
		return (String) this.nodeToName.get(v);
	}
	
	public INode newNode() {
		final INode v = super.newNode();
		this.nodeToName.put(v, "#" + v.id());
		return v;
	}
	
	public INode newNode(final String name) {
		final INode v = super.newNode();
		this.nodeToName.put(v, name);
		return v;
	}
	
	public void delete(final IGraphElement x) {
		this.nodeToName.remove(x);
		super.delete(x);
	}
}