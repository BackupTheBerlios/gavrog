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

object MakeMorph {
  def bail(message: String) {
    System.err.println(message)
    exit(1)
  }
  
  sealed def iterate[A](x: A, f: A => A, n: Int) : A =
    if (n <= 0) x else iterate(f(x), f, n-1)
  
  def main(args: Array[String]) : Unit = {
    var i = 0
    if (args.size < i + 2) bail("need two .obj files as arguments")
    val original  = Mesh.read(args(i), true)(0)
    val morphed = Mesh.read(args(i + 1), true)(0)
    val subd = if (args.length > i + 2) args(i + 2).toInt else 0
    val tmp = if (args.length > i + 3) Mesh.read(args(i + 3), true)(0) else null
    
    val step = if (subd > 0) (s: Mesh) => s.subdivision
               else          (s: Mesh) => s.coarsening
    val donor = if (subd == 0) morphed
                else iterate(if (tmp != null) tmp.withMorphApplied(morphed)
                             else morphed,
                             step, subd.abs)
    
    Mesh.write(System.out, "materials", original.withMorphApplied(donor))
  }
}
