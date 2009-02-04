package org.gavrog.joss.meshes

import Sums._

object Info {
  def main(args : Array[String]) : Unit = {
    val meshes =
      if (args.length > 0) Mesh.read(args(0)) else Mesh.read(System.in)

    for (mesh <- meshes) {
      val parts = mesh.components
      println
      println("Mesh '%s':"             format mesh.name)
      println("  %5d components"       format parts.size)
      for (p <- parts) {
        println("       %5d vertices" format p.vertices.size)
        println("       %5d symmetries" format Mesh.allMatches(p, p).size)
        val c = p.coarseningClassifications
        println("       %5d coarsenings" format c.size)
        println("       %5d strict coarsenings" format c.count(_.isStrict))
        println
      }
      println("  %5d vertices"          format mesh.numberOfVertices)
      println("  %5d edges"             format mesh.numberOfEdges)
      println("  %5d faces"             format mesh.numberOfFaces)
      println("  %5d holes"             format mesh.numberOfHoles)
      println("  %5d border edges"      format mesh.holes.sum(_.degree))
      println("  %5d normals"           format mesh.numberOfNormals)
      println("  %5d texture vertices"  format mesh.numberOfTextureVertices)
      println("  %5d groups"            format mesh.numberOfGroups)
      println("  %5d materials"         format mesh.numberOfMaterials)
    
      println
      println("  First vertex:         " + mesh.vertices.next)
      println("  First texls ture vertex: " + mesh.textureVertices.next)
      println("  First normal:         " + mesh.normals.next)
      println("  First group:          " + mesh.groups.next)
      println("  First material:       " + mesh.materials.next)
      println("  First face:           " + mesh.faces.next)
      
      println
      println("  Poles:")
      val vds = mesh.vertices.map(_.degree).toList
      println("    %5d isolated vertices"            format vds.count(0 ==))
      println("    %5d vertices of degree 1"         format vds.count(1 ==))
      println("    %5d vertices of degree 2"         format vds.count(2 ==))
      println("    %5d vertices of degree 3"         format vds.count(3 ==))
      println("    %5d vertices of degree 5"         format vds.count(5 ==))
      println("    %5d vertices of degree 6 or more" format vds.count(5 <))
      println("    %5d faces of degree 3 or less" format
                    mesh.faces.count(_.degree < 3))
      println("    %5d faces of degree 5 or more" format
                    mesh.faces.count(_.degree > 5))
    }
  }
}
