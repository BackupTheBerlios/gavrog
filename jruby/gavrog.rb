module Gavrog
  include Java
  import org.gavrog.joss.dsyms.basic.DSymbol
  import org.gavrog.joss.dsyms.basic.Subsymbol
  import org.gavrog.joss.dsyms.derived.Covers
  import org.gavrog.joss.dsyms.generators.InputIterator
  
  DSFile = InputIterator
  
  class Face
    def initialize(ds, elm)
      @ds = ds
      @elm = int(elm)
    end
    
    def degree
      @ds.m(0, 1, @elm)
    end
  end
  
  class Tile
    def initialize(ds, elm)
      @ds = ds
      @elm = int(elm)
    end
    
    def cover
      sub = DSymbol.new(Subsymbol.new(@ds, int([0, 1, 2]), @elm))
      Covers.finiteUniversalCover(sub)
    end
  end
  
  class DSymbol
    def reps(*args)
      orbit_reps(int(args))
    end
    
    def faces
      case dim
      when 2:
        list = reps(0, 1)
      when 3:
        list = reps(0, 1, 3)
      else
        raise "dimension #{dim} not supported"
      end
      Mapper.new(list) { |elm| Face.new(self, elm) }
    end
    
    def tiles
      case dim
      when 2:
        list = reps(0, 1)
      when 3:
        list = reps(0, 1, 2)
      else
        raise "dimension #{dim} not supported"
      end
      Mapper.new(list) { |elm| Tile.new(self, elm) }
    end
  end
  
  def int(arg)
    if arg.respond_to? :each
      arg.map { |x| int(x) }
    else
      java.lang.Integer.new(arg)
    end
  end

  def run_filter(input, output, message = nil)
    File.open(output, "w") do |file|
      in_count = out_count = 0
      
      DSFile.new(input).each do |ds|
        in_count += 1
        out = yield(ds)
        case out
        when DSymbol:
          out_count += 1
          file.puts out
        when true:
          out_count += 1
          file.puts ds
        end
      end
      
      file.puts "# #{message}" if message
      file.puts "# read #{in_count} and wrote #{out_count} symbols"
    end
  end
end

module Enumerable
  def count
    inject(0) { |n, x| n += 1 }
  end
end

class Symbol
  def to_proc
    proc { |obj, *args| obj.send(self, *args) }
  end
end
