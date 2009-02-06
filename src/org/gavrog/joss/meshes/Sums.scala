package org.gavrog.joss.meshes

object Sums {
  trait SemiGroup[A] {
    def add(x: A, y: A) : A
  }
  trait Monoid[A] extends SemiGroup[A] {
    def unit : A
  }
    
  implicit object IntMonoid extends Monoid[Int] {
    def add(x: Int, y: Int) = x + y
    def unit = 0
  }
    
  implicit object DoubleMonoid extends Monoid[Double] {
    def add(x: Double, y: Double) = x + y
    def unit = 0.0
  }

  trait Summable[T] {
    def foreach(f: T => Unit): Unit
  
    def sum[A](f: T => A)(implicit m : Monoid[A]) : A = {
      var s : A = m.unit;
      for (x <- this) s = m.add(s, f(x));
      s
    }
    def count(f: T => Boolean) = sum(x => if (f(x)) 1 else 0)
  }
    
  implicit def wrapIterable[T](iter : Iterable[T]) = new Summable[T] {
    def foreach(f : T => Unit) = iter.foreach(f)
  }
  implicit def wrapIterator[T](iter : Iterator[T]) = new Summable[T] {
    def foreach(f : T => Unit) = iter.foreach(f)
  }
}
