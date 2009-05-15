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

import Vectors._

object Flip {
  def main(args: Array[String]) : Unit = {
    val original  = Mesh.read(System.in, true)(0)
    val donor = original.clone
    
    for (v <- donor.vertices) v.pos = (-v.x, v.y, v.z)
    
    Mesh.write(System.out, "materials", original.withMorphApplied(donor))
  }
}
