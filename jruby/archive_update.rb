#!/bin/env jruby

include Java

# ============================================================
#   Imports
# ============================================================

import org.gavrog.box.simple.DataFormatException
import org.gavrog.joss.pgraphs.io.Net
import org.gavrog.joss.pgraphs.io.NetParser
import org.gavrog.joss.pgraphs.io.Archive

import java.lang.ClassLoader
import java.io.InputStreamReader
import java.io.BufferedReader

# ============================================================
#   Prepare for lookup in old RCSR archive
# ============================================================

def archive_read(archive, path)
  # --- make sure this works from within .jar files and such
  stream = ClassLoader.getSystemResourceAsStream(path)
  reader = BufferedReader.new(InputStreamReader.new(stream))
  archive.add_all reader
end

# --- create an empty archive
archive = Archive.new "1.0"

# --- add entries from RCSR and zeolite archive files
archive_read archive, "org/gavrog/apps/systre/rcsr.arc"


# ============================================================
#   Main loop: read nets and print their symbols if found
# ============================================================

parser = NetParser.new(ARGV[0])

while not parser.at_end
  begin
    net = parser.parse_net
  rescue DataFormatException => ex
    if net.nil? || net.name.nil?
      puts "???:\t>>>#{ex}<<<"
    else
      puts "#{net.name}:\t>>>#{ex}<<<"
    end
  end
  if not net.connected?
    puts "#{net.name}:\t>>>not connected<<<"
  elsif not net.locally_stable?
    puts "#{net.name}:\t>>>unstable<<<"
  elsif net.ladder?
    puts "#{net.name}:\t>>>ladder<<<"
  elsif found = archive.get(net.minimal_image.systre_key)
    unless net.name == found.name
      puts "#{net.name}:\tfound as #{found.name}"
    end
  else
    if archive.getByName(net.name).nil?
      puts "#{net.name}:\tNEW!!!"
    else
      puts "#{net.name}:\t>>>name clash<<<"
    end
  end
end

# ============================================================
#   EOF
# ============================================================
