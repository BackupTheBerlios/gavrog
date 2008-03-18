module Gavrog
  include Java
  import org.gavrog.joss.dsyms.basic.DSymbol
  import org.gavrog.joss.dsyms.generators.InputIterator
  
  DSFile = InputIterator
  
  class Iterator
    include Enumerable
  
    def initialize(base = nil, &block)
      @base = base
      @block = block
    end
    
    def apply(base)
      self.class.send(:new, base, &@block)
    end
    
    def in_count
      @in_count || 0
    end
    
    def out_count
      @out_count || 0
    end
  end

  class Mapper < Iterator
    def each
      @base.each do |x|
        @in_count = in_count + 1
        @out_count = out_count + 1
        yield @block.call(x)
      end
    end
  end
  
  class Filter < Iterator
    def each
      @base.each do |x|
        @in_count = in_count + 1
        if @block.call(x)
          @out_count = out_count + 1
          yield x
        end
      end
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
      Mapper.new(self.reps(0, 1, 3)) { |elm| Face.new(self, elm) }
    end
  end
  
  def int(arg)
    if arg.respond_to? :each
      arg.map { |x| int(x) }
    else
      java.lang.Integer.new(arg)
    end
  end

  def run_filter(filter, input, output, message = nil)
    File.open(output, "w") do |file|
      good = filter.apply(DSFile.new(input))
      good.each { |ds| file.puts ds }
      
      file.puts "# #{message}" if message
      file.puts "# read #{good.in_count} and wrote #{good.out_count} symbols"
    end
  end
end
