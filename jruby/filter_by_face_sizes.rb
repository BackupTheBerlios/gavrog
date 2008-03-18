#!/bin/env jruby

include Java
import org.gavrog.joss.dsyms.basic.DSymbol
import org.gavrog.joss.dsyms.generators.InputIterator

DSFile = InputIterator

def int(arg)
  if arg.respond_to? :each
    arg.map { |x| int(x) }
  else
    java.lang.Integer.new(arg)
  end
end

class Iterator
  include Enumerable

  def initialize(base, &block)
    @base = base
    @block = block
  end
  
  def each
    @base.each { |x| yield @block.call(x) }
  end
end

class Face
  def initialize(ds, elm)
    @ds = ds
    @elm = int(elm)
  end
  
  def degree
    @ds.m(0, 1, @elm)
  end
end

class DSymbol
  def reps(*args)
    orbit_reps(int(args))
  end
  
  def faces
    Iterator.new self.reps(0, 1, 3) do |elm|
        Face.new(self, elm)
    end
  end
end

def filter(min, max, input, output)
  range = min..max
  
  File.open(output, "w") do |file|
    n_in = n_out = 0
    DSFile.new(input).each do |ds|
      n_in += 1
      if ds.faces.all? { |f| range.include? f.degree }
        file.puts ds
        n_out += 1
      end
    end
    file.puts "# filter_by_face_sizes read #{n_in} and wrote #{n_out} symbols"
  end
end

filter ARGV[0].to_i, ARGV[1].to_i, ARGV[2], ARGV[3]
