#!/bin/env jruby

include Java
import org.gavrog.joss.dsyms.basic.DSymbol
import org.gavrog.joss.dsyms.basic.Subsymbol
import org.gavrog.joss.dsyms.derived.Covers
import org.gavrog.joss.dsyms.generators.InputIterator

def int(x)
  java.lang.Integer.new(x)
end

def filter(min, max, infile, outfile)
  ixt = [int(0), int(1), int(2)]
  range = min..max

  File.open(outfile, "w") do |f|
    n_in = n_out = 0
    InputIterator.new(infile).each do |ds|
      n_in += 1
      if ds.orbit_reps(ixt).all? { |elm| range.include? nr_faces(ds, elm) }
        f.puts ds
        n_out += 1
      end
    end
    f.puts "# filter_by_tile_sizes: tile size range #{min}-#{max}"
    f.puts "# read #{n_in} and wrote #{n_out} symbols"
  end
end

def nr_faces(ds, elm)
  ixt = [int(0), int(1), int(2)]
  ixf = [int(0), int(1)]
  sub = DSymbol.new(Subsymbol.new(ds, ixt, int(elm)))
  Covers.finiteUniversalCover(sub).orbit_reps(ixf).map.size
end

filter(ARGV[0].to_i, ARGV[1].to_i, ARGV[2], ARGV[3])
