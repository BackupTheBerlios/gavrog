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
import scala.io.Source

object Transfer {
  def bail(message: String) {
    System.err.println(message)
    exit(1)
  }
  
  class Options(p: Boolean, t: Boolean, g: Boolean, m: Boolean, s: Boolean) {
    def this() = this(false, false, false, false, false)
    
    var positions = p
    var texverts  = t
    var groups    = g
    var materials = m
    var smoothing = s
    
    def any = positions || texverts || groups || materials || smoothing
  }
  
  def main(args: Array[String]) : Unit = {
    var i = 0
    var options: Options = new Options
    while (args(i).startsWith("-")) {
      for (c <- args(i))  c match {
        case '-' => {}
        case 'p' => options.positions = true
        case 't' => options.texverts  = true
        case 'g' => options.groups    = true
        case 'm' => options.materials = true
        case 's' => options.smoothing = true
        case _   => System.err.println("WARNING: unknown option %s" format c)
      }
      i += 1
    }
    if (!options.any) options = new Options(false, false, true, true, true)
    
    if (args.size < i + 2) bail("need two .obj files as arguments")
    val mesh  = new Mesh(Source fromFile args(i))
    val donor = new Mesh(Source fromFile args(i + 1))
    
    val old = mesh.components
    val mod = donor.components
    
    val result = new Mesh(mesh.name)
    
    var count = 0
    for (part <- mod) {
      var dist = Double.MaxValue
      var map: Map[Mesh.Chamber, Mesh.Chamber] = null
      var image: Mesh.Component = null
      for (c <- old) {
        val candidate = Mesh.bestMatch(part, c)
        if (candidate != null) {
          val d = Mesh.distance(candidate)
          if (d < dist) {
            dist = d
            map = candidate
            image = c
          }
        }
      }
      if (map != null) {
        System.err.println(
          "Transferring data for component with %d vertices and %d faces."
          format (part.vertices.size, part.faces.size))
        count += 1
      } else {
        System.err.println(
          "Copying unmatched component with %d vertices and %d faces."
          format (part.vertices.size, part.faces.size))
      }
      
      val vMap = new LazyMap((p: Pair[Mesh, int]) =>
        result.addVertex(p._1.vertex(p._2).pos))
      val tMap = new LazyMap((p: Pair[Mesh, int]) =>
        result.addTextureVertex(p._1.textureVertex(p._2).pos))

      if (image != null) {
        val inv = new HashMap[Mesh.Chamber, Mesh.Chamber]
        for ((c, d) <- map) inv(d) = c 
        for (v <- mesh.vertices if image.vertices.contains(v))
          vMap(if (options.positions) (donor, inv(v.chamber).vertexNr)
               else (mesh, v.nr))
      }
      def newVertex(c: Mesh.Chamber) = {
        val p = if (map == null || options.positions) (donor, c.vertexNr)
                else (mesh, map(c).vertexNr)
        if (p._2 == 0) 0 else vMap(p).nr
      }
      def newTexVert(c: Mesh.Chamber) = {
        val p = if (map == null || options.texverts) (donor, c.tVertexNr)
                else (mesh, map(c).tVertexNr)
        if (p._2 == 0) 0 else tMap(p).nr
      }
      def newGroup(f: Mesh.Face) = {
        val g = if (map == null || options.groups) f.group
                else map(f.chamber).face.group
        result.group(if (g == null) "_default" else g.name)
      }
      def newMaterial(f: Mesh.Face) = {
        val m = if (map == null || options.materials) f.material
                else map(f.chamber).face.material
        result.material(if (m == null) "_default" else m.name)
      }
      def newSmoothing(f: Mesh.Face) =
        if (map == null || options.smoothing) f.smoothingGroup
        else map(f.chamber).face.smoothingGroup
        
      for (f <- part.faces) {
        val cs = f.vertexChambers.toSeq
        val vs = cs.map(newVertex)
        val vt = cs.map(newTexVert)
        val vn = cs.map(c => 0)
        val g = result.addFace(vs, vt, vn)
        g.group          = newGroup(f)
        g.material       = newMaterial(f)
        g.smoothingGroup = newSmoothing(f)
      }
    }
    result.fixHoles
    result.computeNormals
    result.mtllib ++ mesh.mtllib
    result.mtllib ++ donor.mtllib
    System.err.println("Made %d transfers out of %d components."
                       format (count, mod.size))
    result.write(System.out, "materials")
  }
}
