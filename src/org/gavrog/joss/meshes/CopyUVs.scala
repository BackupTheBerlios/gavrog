package org.gavrog.joss.meshes

import scala.collection.mutable._
import Mesh._

object CopyUVs {
  def bail(message: String) {
    System.err.println(message)
    exit(1)
  }
  
  def main(args: Array[String]) : Unit = {
    var i = 0

    if (args.size < i + 2) bail("need two .obj files as arguments")
    val mesh = Mesh.read(args(i), true)(0)
    val donor  = Mesh.read(args(i + 1), true)(0)

    val originals = mesh.charts
    
    for (chart <- donor.charts) {
      for (c <- originals) {
        val map = Mesh.bestMatch(chart, c)
        if (map != null) {
          System.err.println(
            "Transferring data for chart with %d vertices and %d faces."
            format (chart.vertices.size, chart.faces.size))
          for ((c, d) <- map) d.tVertex.moveTo(c.tVertex)
        }
      }
    }
    Mesh.write(System.out, List(mesh), "materials")
  }
}
