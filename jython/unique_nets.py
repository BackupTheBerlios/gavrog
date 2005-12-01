#!/bin/env jython

# ============================================================
#   Imports
# ============================================================

# --- Jython stuff
import sys

# --- Java stuff
from java.lang import ClassLoader
from java.io import InputStreamReader, BufferedReader

# --- Gavrog stuff
from org.gavrog.joss.pgraphs.io import NetParser
from org.gavrog.systre import Archive


# ============================================================
#   Prepare for RCSR lookup
# ============================================================

# --- get RCSR archive file (possibly from a .jar or the web)
rcsr_path = "org/gavrog/systre/rcsr.arc"
rcsr_stream = ClassLoader.getSystemResourceAsStream(rcsr_path)
reader = BufferedReader(InputStreamReader(rcsr_stream))

# --- create an archive object from it
archive = Archive("1.0")
archive.addAll(reader)


# ============================================================
#   Main data processing
# ============================================================

# --- create a parser that reads nets from the given file
parser = NetParser(sys.argv[1])

# --- dictionary of seen nets
seen = {}

# --- count the nets that we read
count = 0

# --- main loop
while 1:
    # --- read the next net from the file
    G0 = parser.parseNet()
    if G0 is None:
        break
    count += 1

    # --- retrieve the net's name or make one up
    name = parser.name
    if name is None:
        name = "unamed-%03d" % count
    print "Net %03d (%s): " % (count, name),

    # --- IMPORTANT: use the minimal ideal translation unit
    G = G0.minimalImage()

    # --- compute the Systre key and check if we've seen it
    key = G.systreKey
    old = seen.get(key)
    if old:
        # --- skip further output for duplicates
        print "\tduplicates %s" % old
        print
        continue
    else:
        print "\tnew entry"
    seen[key] = name

    # --- print coordination sequences for the unique nodes
    print "\tCS:",
    n = 0
    for orb in G.nodeOrbits():
        for v in orb:
            i = 0
            for x in G.coordinationSequence(v):
                if i == 0:
                    if n == 0:
                        print "\t%d" % x,
                    else:
                        print "\t\t%d" % x,
                else:
                    print x,
                i += 1
                if i > 16:
                    break
            break
        print
        n += 1

    # --- print the name of the space group
    print "\tGroup:",
    print "\t%s" % G.spaceGroup.name

    # --- look net up in RCSR and print symbol, if found
    print "\tRCSR:",
    found = archive.get(key)
    if found:
        print "\t%s" % found.name
    else:
        print "\tnot found"

    # --- print out the minimal graph in canonical form
    print "\tCode:",
    print "\t%s" % G.invariant()

    # --- print out the barycentric positions
    print "\tPositions:"
    canonical = G.canonical()
    pos = canonical.barycentricPlacement()
    for v in canonical.nodes():
        print "\t\t%s ->" % v.id(),
        p = pos[v]
        for i in range(p.dimension):
            print " %9.5f" % float(p[i]),
        print

    # --- don't crowd
    print

# ============================================================
#   EOF
# ============================================================
