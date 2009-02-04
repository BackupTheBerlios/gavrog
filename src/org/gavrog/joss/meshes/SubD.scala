package org.gavrog.joss.meshes

import System.{in, out, err}

object SubD {
  def iterate[A](x: A, f: A => A, n: Int) : A =
    if (n > 0) iterate(f(x), f, n-1) else x
    
  def main(args : Array[String]) : Unit = {
    val n = if (args.length > 1) args(1).toInt else 1
    err.println("Reading...")
    val src = if (args.length > 0) Mesh.read(args(0)) else Mesh.read(in)
    err.println("Processing...")
    val dst = src.map(m =>
      if (n >= 0) iterate(m, (s : Mesh) => s.subdivision, n)
      else        iterate(m, (s : Mesh) => s.coarsening, -n))
    err.println("Writing...")
    Mesh.write(out, dst, "materials")
    err.println("Done.")
  }
}
