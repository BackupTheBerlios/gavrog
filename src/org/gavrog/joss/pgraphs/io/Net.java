package org.gavrog.joss.pgraphs.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.box.collections.Pair;
import org.gavrog.joss.pgraphs.basic.IEdge;
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
	final private Map nodeInfo = new HashMap();
	
	public Net(final int dim, final String name, final String group) {
		super(dim);
		this.name = name;
		this.givenGroup = group;
	}

	public Net(final PeriodicGraph graph, final String name, final String group) {
        super(graph.getDimension());
        final Map old2new = new HashMap();
        for (final Iterator nodes = graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            old2new.put(v, newNode());
        }
        for (final Iterator edges = graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = (INode) old2new.get(e.source());
            final INode w = (INode) old2new.get(e.target());
            newEdge(v, w, graph.getShift(e));
        }
		this.name = name;
		this.givenGroup = group;
	}

	public String getGivenGroup() {
		return givenGroup;
	}

	public String getName() {
		return name;
	}
	
	public void setNodeInfo(final INode v, final Object key, final Object value) {
		assert(this.hasElement(v));
		this.nodeInfo.put(new Pair(v, key), value);
	}
	
	public Object getNodeInfo(final INode v, final Object key) {
		assert(this.hasElement(v));
		return this.nodeInfo.get(new Pair(v, key));
	}
	
	public String getNodeName(final INode v) {
		assert(this.hasElement(v));
		return (String) this.nodeToName.get(v);
	}
	
	public Map getNodeToNameMap() {
		final Map res = new HashMap();
		res.putAll(this.nodeToName);
		return res;
	}
	
	public INode newNode() {
		final INode v = super.newNode();
		this.nodeToName.put(v, "V" + v.id());
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