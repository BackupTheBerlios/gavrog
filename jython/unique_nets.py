import sys

from java.io import FileReader

from org.gavrog.joss.pgraphs.io import NetParser

seen = {}
parser = NetParser(FileReader(sys.argv[1]))
count = 0

while 1:
    G = parser.parseNet()
    if G is None:
        break
    count += 1

    name = parser.name
    if name is None:
        name = "unamed-%03d" % count
    print "Net %03d (%s): " % (count, name),
    
    key = G.systreKey
    old = seen.get(key)
    if old:
        print "\t\tduplicates %s" % old
    else:
        print "\t\tnew entry"

    seen[key] = name
