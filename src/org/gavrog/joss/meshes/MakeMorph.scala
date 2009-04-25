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

object MakeMorph {
  def bail(message: String) {
    System.err.println(message)
    exit(1)
  }
  
  def main(args: Array[String]) : Unit = {
    var i = 0

    if (args.size < i + 2) bail("need two .obj files as arguments")
    val mesh  = Mesh.read(args(i), true)(0)
    val donor = Mesh.read(args(i + 1), true)(0)

    val originals = mesh.components
    
    for (comp <- donor.components) {
      var dist = Double.MaxValue
      var map: Map[Mesh.Chamber, Mesh.Chamber] = null
      var image: Mesh.Component = null
      System.err.println( "Matching component with %d vertices and %d faces."
                          format (comp.vertices.size, comp.faces.size))
      try {
        for (c <- originals) {
          val candidate = Mesh.bestMatch(comp, c)
          if (candidate != null) {
            val d = Mesh.distance(candidate)
            if (d < dist) {
              dist = d
              map = candidate
              image = c
            }
          }
        }
      } catch {
        case _ => System.err.println("Error during matching!")
      }
      if (map != null) {
        System.err.println("Match found. Copying positions.")
        for ((c, d) <- map) d.vertex.moveTo(c.vertex.pos)
      }
    }
    Mesh.write(System.out, List(mesh), "materials")
  }
}
