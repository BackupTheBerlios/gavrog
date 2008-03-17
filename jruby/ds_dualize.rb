#!/bin/env jruby

include Java
import org.gavrog.joss.dsyms.generators.InputIterator

File.open(ARGV[1], "w") do |f|
  InputIterator.new(ARGV[0]).each do |ds|
    f.puts ds.dual()
  end
end
