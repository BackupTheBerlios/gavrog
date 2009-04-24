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

import java.awt.Color
import java.io.{File, FileWriter}

import scala.swing.{Action, BorderPanel, MainFrame, Menu, MenuBar,
                    MenuItem, Orientation, Separator, SplitPane, TextArea}

import SwingSupport._
import actions._

object View {
  val statusLine  = new TextArea(1, 80) { editable = false }
  val sceneViewer = new MeshViewer
  val uvMapViewer = new UVsViewer
  
  def log(message: String) = statusLine.text = message
  
  def main(args : Array[String]) {
    new MainFrame {
      title = "Scala Mesh Viewer"
      val main = new BorderPanel {
        add(new SplitPane(Orientation.Vertical, sceneViewer, uvMapViewer) {
          continuousLayout = true
        }, BorderPanel.Position.Center)
        add(statusLine, BorderPanel.Position.South)
      }
      
      val meshLoader = new MeshLoadAction("Load mesh...", main) {
        accelerator = "ctrl O"
      }
      import meshLoader._
      
      val meshSaver = new FileChoiceAction("Save mesh...", main) {
        accelerator = "ctrl S"
        openForWrite = true
        override def openFile(selected: File) {
          Mesh.write(new FileWriter(selected), Seq(mesh), null)
    	  log("Wrote mesh to %s" format selected)
    	}
      }
      
      val screenShotSaver =
        new ScreenShotAction("Take Screen Shot...", main, sceneViewer) {
          accelerator = "ctrl I"
        }

      var _mesh: Mesh = null
      def mesh = _mesh
      def mesh_=(new_mesh: Mesh) {
        _mesh = new_mesh
        sceneViewer.setMesh(_mesh)
        uvMapViewer.setMesh(_mesh)
      }
      
      listenTo(meshLoader, screenShotSaver)
      reactions += {
        case MessageSent(src, text) => log(text)
        case MeshLoaded(src, new_mesh) => mesh = new_mesh
        case ChoiceCancelled(src) => log("Cancelled!")
        case ChoiceError(src) => log("Error in file chooser.")
      }
  
      listenTo(sceneViewer, uvMapViewer)
      reactions += {
        case MessageSent(_, text) => log(text)
      }
      
      contents = main
      menuBar = new MenuBar {
        contents += new Menu("File") {
          contents ++ List(
            new MenuItem(meshLoader),
            new MenuItem(meshSaver),
            new Separator,
            new MenuItem(screenShotSaver),
            new Separator,
            new MenuItem(new Action("Exit") {
              def apply() { System.exit(0) }
              accelerator = "ctrl Q"
            })
          )
        }
        contents += new Menu("Mesh") {
          contents ++ List(
            new MenuItem(Action("Subdivide") { mesh = mesh.subdivision }),
            new MenuItem(Action("Coarsen") { mesh = mesh.coarsening })
          )
        }
      }
      pack
      visible = true
    }
  }
}
