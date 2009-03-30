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


package org.gavrog.joss.meshes.gui;

import de.jreality.math.Matrix
import de.jreality.scene.{SceneGraphComponent, Transformation}
import de.jreality.scene.tool.{AbstractTool, InputSlot, ToolContext}

class PanTool extends AbstractTool(InputSlot.getDevice("DragActivation")) {
  val evolutionSlot = InputSlot.getDevice("PointerEvolution")
  addCurrentSlot(evolutionSlot)
      
  var comp: SceneGraphComponent = null
      
  override def activate(tc: ToolContext) {
	comp = tc.getRootToToolComponent.getLastComponent
	if (comp.getTransformation == null)
	  comp.setTransformation(new Transformation)
  }
	
  override def perform(tc: ToolContext) {
	val evolution = new Matrix(tc.getTransformationMatrix(evolutionSlot))
	evolution.conjugateBy(
	  new Matrix(tc.getRootToToolComponent.getInverseMatrix(null)))
	comp.getTransformation.multiplyOnRight(evolution.getArray)
  }
}
  
