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

import scala.swing.{Action, BorderPanel, MainFrame, Menu, MenuBar,
                    MenuItem, Orientation, Separator, SplitPane, TextArea}

import SwingSupport._
import actions.{MeshLoadAction, ScreenShotAction}

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
	    accelerator = "control O"
	  }
      val screenShotSaver =
        new ScreenShotAction("Take Screen Shot...", main, sceneViewer) {
          accelerator = "control I"
        }

	  listenTo(meshLoader, screenShotSaver)
	  reactions += {
	    case MessageEvent(text) => log(text)
	    case meshLoader.LoadEvent(result, Some(mesh)) => {
	      sceneViewer.setMesh(mesh)
	      uvMapViewer.setMesh(mesh)
	    }
	  }
  
	  contents = main
      menuBar = new MenuBar {
        contents += new Menu("File") {
          contents ++ List(
    	    new MenuItem(meshLoader),
    	    new Separator,
    	    new MenuItem(screenShotSaver),
    	    new Separator,
    	    new MenuItem(Action("Exit") { System.exit(0) })
          )
        }
      }
      pack
      visible = true
    }
  }
}
