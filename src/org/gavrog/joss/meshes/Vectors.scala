package org.gavrog.joss.meshes

import Sums._

object Vectors {
  case class Vec2(x: Double, y: Double) {
    def +(that: Vec2) = Vec2(this.x + that.x, this.y + that.y)
    def -(that: Vec2) = Vec2(this.x - that.x, this.y - that.y)
    def *(f: Double) = Vec2(x * f, y * f)
    def /(f: Double) = Vec2(x / f, y / f)
    
    def *(that: Vec2) = this.x * that.x + this.y * that.y
    def norm = Math.sqrt(this * this)
    def || = norm
    def unit = this / this.||
  }
  object zero2 extends Vec2(0, 0)
  implicit object Vec2Monoid extends Monoid[Vec2] {
    def add(x: Vec2, y: Vec2) = x + y
    def unit = Vec2(0, 0)
  }
  
  case class Vec3(x: Double, y: Double, z: Double) {
    def +(that: Vec3) = Vec3(this.x + that.x, this.y + that.y, this.z + that.z)
    def -(that: Vec3) = Vec3(this.x - that.x, this.y - that.y, this.z - that.z)
    def *(f: Double) = Vec3(x * f, y * f, z * f)
    def /(f: Double) = Vec3(x / f, y / f, z / f)
    
    def *(that: Vec3) = this.x * that.x + this.y * that.y + this.z * that.z
    def x(that: Vec3) : Vec3 = Vec3(this.y * that.z - this.z * that.y,
                                    this.z * that.x - this.x * that.z,
                                    this.x * that.y - this.y * that.x)
    def norm = Math.sqrt(this * this)
    def || = norm
    def unit = this / this.||
  }
  object zero3 extends Vec3(0, 0, 0)
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
