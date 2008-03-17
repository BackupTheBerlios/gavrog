#!/bin/env jruby

include Java
import org.gavrog.joss.pgraphs.io.NetParser
import org.gavrog.joss.tilings.FaceList

def convert(input, output)
  File.open(output, "w") do |f|
    parser = NetParser.new(input)
    while !parser.at_end
      data = parser.parse_data_block
      if data.type.match(/TILING/i)
        name = data.get_entries_as_string "name"
        f.puts "#\@ name #{name}" if name
        f.puts FaceList.new(data).symbol
        f.flush
      end
    end
  end
end

convert ARGV[0], ARGV[1]
