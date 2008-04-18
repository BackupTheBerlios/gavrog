include Java
import org.gavrog.jane.numbers.Fraction

half = Fraction.new(1, 2)
third = Fraction.new(1, 3)

(3..120).each do |a|
  b = Fraction.new(2 * a, a - 2).floor.plus(1).intValue
  while true
    as = Fraction.new(1, a).plus(Fraction.new(1, b)).minus(half).times(3)
    n2 = as.inverse.times(4).negative
    n3 = as.inverse.times(6).negative
    if n3.minus(b).negative?
      break
    end
    if n2.integer? or n3.integer?
      puts "%8s %8s %8s %8s" %
           [a, b, n2.integer? ? n2 : "-", n3.integer? ? n3 : "-"]
    end
    b += 1
  end
end
