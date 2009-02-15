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
    val originals = Mesh.read(args(i))
    if (originals.size != 1) bail("original must have 1 body")
    
    val modified  = Mesh.read(args(i + 1))
    if (modified.size != 1) bail("modified must have 1 body")
    
    val mesh = originals(0)
    val old = mesh.charts
    val donor = modified(0)
    val mod = donor.charts
    
    for (chart <- mod) {
      for (c <- old) {
        val map = Mesh.bestMatch(chart, c)
        if (map != null) {
          System.err.println(
            "Transferring data for chart with %d vertices and %d faces."
            format (chart.vertices.size, chart.faces.size))
        }
      }
    }
  }
}
