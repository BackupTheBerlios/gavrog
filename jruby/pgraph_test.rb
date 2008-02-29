include Java
import org.gavrog.joss.pgraphs.io.Net

net = Net.iterator(ARGV[0]).next

puts net.connectingEdges(1, 2).map { |e| e.to_s }
puts net.directedEdges(1, 2).map { |e| e.to_s }
