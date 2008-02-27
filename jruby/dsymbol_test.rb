include Java
import org.gavrog.joss.dsyms.basic.DSymbol
import org.gavrog.joss.dsyms.basic.Traversal
import org.gavrog.joss.dsyms.basic.IndexList

ds = DSymbol.new "8 3:2 8 7 6,3 4 5 6 8,1 2 8 5 7,2 4 6 8:3 4,5 3,4 3"

puts Traversal.new(ds, [0, 1, 2, 3], 1).map { |x| [x.element, x.index] }.inspect
puts ds.orbit([0, 1, 2, 3], 1).map.inspect
