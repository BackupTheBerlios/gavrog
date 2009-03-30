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

package org.gavrog.joss.meshes.gui

import de.jreality.scene.tool.{AbstractTool, InputSlot, ToolContext}
import de.jreality.math.{FactoredMatrix, Matrix, Pn}

import org.gavrog.joss.meshes.Vectors.Vec3

class RotateTool(viewer: JRealityViewerComponent)
extends AbstractTool() {
  val activationSlot = InputSlot.getDevice("RotateActivation")
  val restrictionSlot = InputSlot.getDevice("Meta")
  val evolutionSlot = InputSlot.getDevice("TrackballTransformation")
  val evolutionXSlot = InputSlot.getDevice("PointerNdcXevolution")
  val evolutionYSlot = InputSlot.getDevice("PointerNdcYevolution")
  addCurrentSlot(activationSlot)
  addCurrentSlot(restrictionSlot)
  addCurrentSlot(evolutionSlot)
  addCurrentSlot(evolutionXSlot)
  addCurrentSlot(evolutionYSlot)

  override def perform(tc: ToolContext) {
	if (tc.getAxisState(activationSlot).isReleased) return
    var angle = 0.0
	var axis = Array(0.0, 0.0, 0.0)
	if (tc.getAxisState(restrictionSlot).isPressed) {
	  val yrot = tc.getAxisState(evolutionXSlot).doubleValue
	  val xrot = tc.getAxisState(evolutionYSlot).doubleValue
	  if (xrot.abs > yrot.abs) {
	    axis = Array(1.0, 0.0, 0.0)
	    angle = -xrot
	  } else {
	    axis = Array(0.0, 1.0, 0.0)
	    angle = yrot
	  }
	} else {
	  val evolution = new Matrix(tc.getTransformationMatrix(evolutionSlot))
	  val e = new FactoredMatrix(evolution, Pn.EUCLIDEAN)
	  axis = e.getRotationAxis
	  angle = e.getRotationAngle
	}
	viewer.rotateScene(Vec3(axis(0), axis(1), axis(2)), angle)
  }
}
