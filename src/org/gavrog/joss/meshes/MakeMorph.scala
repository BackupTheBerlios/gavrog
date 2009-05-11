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
  
  case class IteratedFunction[A](f: A => A, n: Int) {
    def ^(m: Int) = IteratedFunction(f, n * m)
    def apply(x: A) = {
      def iter(x: A, m: Int): A = if (m <= 0) x else iter(f(x), m-1)
      iter(x, n)
    }
  }
  implicit def toRichFun[A](f: A => A) = new IteratedFunction(f, 1)
  
  def main(args: Array[String]) : Unit = {
    var i = 0
    if (args.size < i + 2) bail("need at least two .obj files as arguments")
    val original  = Mesh.read(args(i), true)(0)
    val morphed = Mesh.read(args(i + 1), true)(0)
    val subd = if (args.length > i + 2) args(i + 2).toInt else 0
    val base = if (args.length > i + 3)
                 Mesh.read(args(i + 3), true)(0) withMorphApplied(morphed)
               else
                 morphed
    
    val step: Mesh => Mesh = if (subd > 0) _.subdivision else _.coarsening
    val donor = (step^subd.abs)(base)
    
    Mesh.write(System.out, "materials", original.withMorphApplied(donor))
  }
}
