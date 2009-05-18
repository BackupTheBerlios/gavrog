/*
   Copyright 2009 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


package org.gavrog.joss.meshes

import scala.collection.mutable._

object CopyUVs {
  def bail(message: String) {
    System.err.println(message)
    exit(1)
  }
  
  def main(args: Array[String]) : Unit = {
    var i = 0

    if (args.size < i + 2) bail("need two .obj files as arguments")
    val mesh  = Mesh.read(args(i))
    val donor = Mesh.read(args(i + 1))

    val originals = mesh.charts
    
    for (chart <- donor.charts; c <- originals) {
      val map = Mesh.bestMatch(chart, c)
      if (map != null) {
        System.err.println(
          "Transferring data for chart with %d vertices and %d faces."
          format (chart.vertices.size, chart.faces.size))
        for ((c, d) <- map) d.tVertex.pos = c.tVertex.pos
      }
    }
    Mesh.write(System.out, mesh, "materials")
  }
}
