#!/bin/env jruby

include Java
import org.gavrog.joss.dsyms.generators.InputIterator

def int(x)
  java.lang.Integer.new(x)
end

idcs = [int(0), int(1), int(3)]
range = 4..6

File.open(ARGV[1], "w") do |f|
  n_in = n_out = 0
  InputIterator.new(ARGV[0]).each do |ds|
    n_in += 1
    if ds.orbit_reps(idcs).all? { |elm| range.include? ds.m(0, 1, int(elm)) }
      f.puts ds
      n_out += 1
    end
  end
  f.puts "# filter_by_face_sizes read #{n_in} and wrote #{n_out} symbols"
end
