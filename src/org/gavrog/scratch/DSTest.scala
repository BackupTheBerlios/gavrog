package org.gavrog.scratch

import org.gavrog.joss.dsyms.basic._
import org.gavrog.joss.dsyms.derived._

object DSTest {
  implicit def toIList(x: (Int, Int)) = new IndexList(x._1, x._2)
  implicit def toIList(x: (Int, Int, Int)) = new IndexList(x._1, x._2, x._3)
  implicit def wrapIter[T](it: java.util.Iterator[T]) = new Iterator[T] {
    def hasNext = it.hasNext
    def next = it.next
  }
  implicit def wrapElm(x: Any) = x.asInstanceOf[java.lang.Integer]

  val ds = new DSymbol("1.1:1:1,1,1:5,3")

  def main(args : Array[String]) : Unit = {
    println("Scala DSymbol Test")
    println("==================")
    println
    println("Symbol:        " + ds)
    println("Size:          " + ds.size)
    println("Faces:         " + ds.numberOfOrbits(0, 1))
    println("Edges:         " + ds.numberOfOrbits(0, 2))
    println("Vertices:      " + ds.numberOfOrbits(1, 2))
    println("Curvature:     " + ds.curvature2D)
    println("Spherical:     " + (if (ds.isSpherical2D) "yes" else "no"))
    println("Oriented:      " + (if (ds.isOriented) "yes" else "no"))
    println("Mirrors:       " + (if (ds.isLoopless) "no" else "yes"))
    if (ds.isSpherical2D) {
      val cover = Covers.finiteUniversalCover(ds)
      println("Cover: ")
      println("  Size:        " + cover.size)
      println("  Faces:       " + cover.numberOfOrbits(0, 1))
      println("  Edges:       " + cover.numberOfOrbits(0, 2))
      println("  Vertices:    " + cover.numberOfOrbits(1, 2))
      
      val sizes = cover.orbitReps(0, 1).map(D => cover.m(0, 1, D))
      println("  Faces sizes: " + sizes.mkString(" "))
      println("  Faces:")
      for (D <- cover.orbitReps(0, 1)) {
        val elms =
          cover.orbit((0, 1), D).toList.sort((a, b) => (a compareTo b) < 0)
        println("    (" + elms.mkString(", ") + ")")
      }
    }
  }
}
