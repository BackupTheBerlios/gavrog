#!/bin/env jruby

require File.join(File.dirname(__FILE__), 'gavrog.rb')
import org.gavrog.joss.dsyms.derived.Signature

run_filter(ARGV[0], ARGV[1], "print_signatures") { |ds| Signature.ofTiling(ds) }
