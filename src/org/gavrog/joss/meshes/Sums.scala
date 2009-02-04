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
  
  case class Vec2(x: Double, y: Double) {
    def +(that: Vec2) = Vec2(this.x + that.x, this.y + that.y)
    def -(that: Vec2) = Vec2(this.x - that.x, this.y - that.y)
    def *(f: Double) = Vec2(x * f, y * f)
    def /(f: Double) = Vec2(x / f, y / f)
  }
  implicit object Vec2Monoid extends Monoid[Vec2] {
    def add(x: Vec2, y: Vec2) = x + y
    def unit = Vec2(0, 0)
  }
  
  case class Vec3(x: Double, y: Double, z: Double) {
    def +(that: Vec3) = Vec3(this.x + that.x, this.y + that.y, this.z + that.z)
    def -(that: Vec3) = Vec3(this.x - that.x, this.y - that.y, this.z - that.z)
    def *(f: Double) = Vec3(x * f, y * f, z * f)
    def /(f: Double) = Vec3(x / f, y / f, z / f)
  }
  implicit object Vec3Monoid extends Monoid[Vec3] {
    def add(x: Vec3, y: Vec3) = x + y
    def unit = Vec3(0, 0, 0)
  }
  
  case class Scalar(x: Double) {
    def *(that: Vec3) = that * x
  }
  implicit def dbl2scalar(x: Double) = Scalar(x)
  implicit def int2scalar(x: Int) = Scalar(x)
}
