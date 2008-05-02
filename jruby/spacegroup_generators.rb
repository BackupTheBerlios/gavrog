require File.join(File.dirname(__FILE__), 'gavrog.rb')

import java.util.HashSet

import org.gavrog.jane.compounds.Matrix
import org.gavrog.joss.geometry.Operator
import org.gavrog.joss.geometry.SpaceGroup
import org.gavrog.joss.geometry.SpaceGroupCatalogue
import org.gavrog.joss.geometry.Vector

def log(str)
  # puts("# #{str}")
end

def sorted_operators(group)
  ops = group.primitive_operators_by_type
  types = ops.key_set.sort_by do |x|
    [ -x.order, x.orientation_preserving? ? 0 : 1, x.clockwise? ? 0 : 1 ]
  end
  res = []
  types.each { |t| res += ops.get(t).map }
  res
end

def products(dim, generators, translation_mode = false)
  zero = Vector.zero(dim)
  one = Operator.identity(dim)
  
  translations = HashSet.new
  products = HashSet.new
  products.add(one)
  queue = [ one ]
  
  queue.each do |a|
    generators.each do |b|
      ab = a.times(b)
      if ab.linearPart == one
        t = ab.translationalPart
        if t.modZ == zero
          unless t == zero
            translations.add(t)
          end
        else
          translations.add(t.modZ)
        end
      else
        ab = ab.modZ
        if not products.contains(ab)
          products.add(ab)
          queue << ab
        end
      end
    end  
  end
  translation_mode ? translations : products
end

def rank(vecs)
  Vector.to_matrix(vecs.to_java(Vector)).rank
end

def volume(vecs)
  m = Vector.to_matrix(vecs.to_java(Vector)).mutable_clone
  Matrix.triangulate(m, nil, true, true)
  d = m.rank
  m.get_sub_matrix(0, 0, d, d).determinant
end

def conjugates(vectors, ops)
  res = HashSet.new
  vectors.each do |v|
    ops.each do |a|
      res.add(v.times(a.linear_part))
    end
  end
  res
end

def improves(v, t, ops)
  if t.length == 0
    true
  else
    t0 = conjugates(t, ops).map
    t1 = conjugates(t + [v], ops).map

    r = rank(t0)
    d = v.dimension
    r < d ? rank(t1) > r : volume(t1) < volume(t0)
  end
end

def generators(group)
  dim = group.dimension
  ops = group.primitive_operators

  log "initial guess ..."
  gens = []
  sorted_operators(group).map.each do |op|
    gens << op unless products(dim, gens).contains op
  end

  log "removing redundancies ..."
  done = false
  while not done
    done = true
    gens.each do |op|
      if products(dim, gens - [op]).contains op
        gens.delete(op)
        done = false
        break
      end
    end
  end
  
  log "adding missing translations ..."
  t0 = products(dim, gens, true).map
  t = t0.clone

  Vector.from_matrix(group.primitive_cell).each do |v|
    t << v if improves(v, t, ops)
  end
  
  log "removing redundant translations ..."
  while not done
    done = true
    t.each do |v|
      unless improves(t, v - [t], ops)
        t.delete(op)
        done = false
        break
      end
    end
  end

  log "checking ..."
  p = products(dim, gens)
  ops.each do |op|
    raise "Missing operator #{op}" unless p.contains op
  end
  t1 = conjugates(t, ops).map
  if rank(t1) < dim or volume(t1) > group.primitive_cell.determinant
    raise "Missing translation"
  end

  gens + t - t0
end

gens = generators(SpaceGroup.new(3, ARGV[0]))

puts gens.map(&:to_s)
