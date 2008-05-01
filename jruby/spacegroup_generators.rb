require File.join(File.dirname(__FILE__), 'gavrog.rb')

import java.util.HashSet

import org.gavrog.jane.compounds.Matrix
import org.gavrog.joss.geometry.Operator
import org.gavrog.joss.geometry.SpaceGroup
import org.gavrog.joss.geometry.SpaceGroupCatalogue
import org.gavrog.joss.geometry.Vector

def make_products(dim, generators, translation_mode = false)
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

def generators(dim, name)
  group = SpaceGroup.new(dim, name)
  ops = group.primitive_operators
  gens = []
  products = make_products(dim, gens)

  ops.each do |op|
    unless products.contains op.modZ
      gens << op.modZ
      products = make_products(dim, gens)
    end
  end

  t = make_products(dim, gens, true).map
  (0..dim-1).each do |i|
    e = Vector.unit(dim, i)
    if t.length == 0 or rank(t + [e]) > rank(t)
      t << e
      gens << e
    end
  end

  Vector.from_matrix(group.primitive_cell).each do |v|
    if volume(t + [v]) < volume(t)
      t << v
      gens << v
    end
  end

  gens
end

dim = 3
gens = generators(dim, ARGV[0])

puts gens.map(&:to_s)
