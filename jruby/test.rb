#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
import org.gavrog.box.collections.Iterators
import org.gavrog.joss.dsyms.basic.DynamicDSymbol


covers = Covers.allCovers(DSymbol.new("1:1,1,1:3,3"))

i = 0
Iterators.selections(covers.map.to_java, 8).each do |list|
  ds = DynamicDSymbol.new(2)
  list.each { |part| ds.append(part) }
  if ds.subsymbols(int([0, 1, 2])).all?(&:isSpherical2D)
    puts(i += 1)
  else
    raise ds
  end
end
