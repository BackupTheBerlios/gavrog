#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
include Gavrog

run_filter(Mapper.new { |ds| ds.dual }, ARGV[0], ARGV[1], "dualize")
