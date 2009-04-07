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


package org.gavrog.joss.meshes.gui.actions

import scala.swing.{Action, Component, FileChooser}
import scala.swing.event.Event

import SwingSupport._

class MeshLoadAction(name: String, parent: Component)
extends Action(name) with MessagePublisher
{
  abstract class MeshLoaderEvent(src: MeshLoadAction) extends Event
  case class MeshLoaded(src: MeshLoadAction,
                        mesh: Option[Mesh]) extends MeshLoaderEvent(src)
  case class ChoiceCancelled(src: MeshLoadAction) extends MeshLoaderEvent(this)
  case class ChoiceError(src: MeshLoadAction) extends MeshLoaderEvent(this)
  
  val chooser = new FileChooser
  
  def apply {
	chooser.showOpenDialog(parent) match {
	  case FileChooser.Result.Approve => run {
        val file = chooser.selectedFile
        send("Reading...")
        val mesh = Mesh.read(file.getAbsolutePath, true)(0)
        send("Processing...")
        publish(MeshLoaded(this, Some(mesh)))
        send("Done!")
	  }
	  case FileChooser.Result.Cancel => publish(ChoiceCancelled(this))
	  case FileChooser.Result.Error => publish(ChoiceError(this))
    }
  }
}
