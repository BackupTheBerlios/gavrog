#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')


covers = Covers.allCovers(DSymbol.new("1:1,1,1:3,3"))
covers.each do |ds|
  puts "#{ds.orbifoldSymbol2D}\t#{ds.to_s}"
end

tmp = []
File.open(ARGV[0]) do |f|
  f.each do |line|
    unless line.strip[0,1] == '#'
      fields = line.split('/')
      stabs = fields[1, fields[0].to_i]
      interesting = stabs.select { |x| %w{*332 2*2 332 222 2x}.include? x }
      unless interesting.empty?
        tmp << interesting
      end
    end
  end
end
tmp.sort!.uniq!

allowed = []
tmp.each do |list|
  n = list.size
  (0...2**n).each do |k|
    sub = []
    (0...n).each do |i|
      if k & 2**i != 0
        sub << list[i]
      end
    end
    allowed << sub
  end
end

puts
allowed.sort.uniq.each { |row| puts row.inspect }
