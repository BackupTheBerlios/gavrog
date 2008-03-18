#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
include Gavrog

min  = ARGV[0].to_i
max  = ARGV[1].to_i
fin  = ARGV[2]
fout = ARGV[3]

test = Filter.new { |ds| ds.faces.all? { |f| (min..max).include? f.degree } }
msg = "filter_by_face_sizes: face size range #{min}-#{max}"
run_filter(test, fin, fout, msg)
