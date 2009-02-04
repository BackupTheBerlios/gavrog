package org.gavrog.joss.meshes

import java.io.FileWriter

object Split {
  def main(args : Array[String]) : Unit =
    for (m <- Mesh.read(args(0), true)(0).splitByGroup)
      Mesh.write(new FileWriter("%s.obj" format m.name), List(m), null)
}
