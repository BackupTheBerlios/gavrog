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
