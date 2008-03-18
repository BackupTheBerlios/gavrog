#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
include Gavrog

min  = ARGV[0].to_i
max  = ARGV[1].to_i
fin  = ARGV[2]
fout = ARGV[3]

def nr_faces(tile)
  tile.cover.faces.count
end

test = Filter.new { |ds| ds.tiles.all? { |t| (min..max).include? nr_faces(t) } }
msg = "filter_by_tile_sizes: tile size range #{min}-#{max}"
run_filter(test, fin, fout, msg)
