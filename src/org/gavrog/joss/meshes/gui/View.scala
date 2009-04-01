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

import scala.swing.{BorderPanel, FileChooser, MainFrame, Menu, MenuBar,
                    Orientation, Separator, SplitPane, TextArea}

import SwingSupport._

object View {
  def log(message: String) = statusLine.text = message
  
  val loadMeshChooser = new FileChooser
  val screenShotChooser = new FileChooser
  
  val statusLine  = new TextArea(1, 80) { editable = false }
  val sceneViewer = new MeshViewer
  val uvMapViewer = new UVsViewer
  var active      = List(sceneViewer, uvMapViewer)
  
  def main(args : Array[String]) {
    new MainFrame {
      title    = "Scala Mesh Viewer"
      contents = new BorderPanel {
        add(new SplitPane(Orientation.Vertical, sceneViewer, uvMapViewer) {
          continuousLayout = true
        }, BorderPanel.Position.Center)
        add(statusLine, BorderPanel.Position.South)
      }
      menuBar = new MenuBar {
        contents += new Menu("File") {
          contents ++ List(
    	    new ActionMenuItem("Load Mesh ...", {
    		  val result = loadMeshChooser.showOpenDialog(this)
    		  result match {
    		    case FileChooser.Result.Approve => run {
    			  val file = loadMeshChooser.selectedFile
    			  log("Reading...")
    			  val mesh = Mesh.read(file.getAbsolutePath, true)(0)
    			  log("Converting...")
    			  sceneViewer.setMesh(mesh)
    			  uvMapViewer.setMesh(mesh)
    			  log("Done!")
    		    }
    		    case _ => {}
    		  }
    	    }) { accelerator = "control O" },
      
    	    new Separator,
    	    new ActionMenuItem("Take Screen Shot ...", {
    	      val result = screenShotChooser.showSaveDialog(this)
    	      result match {
    	        case FileChooser.Result.Approve => run {
    	          log("Taking screenshot ...")
    	          val file = screenShotChooser.selectedFile
    	          val d = sceneViewer.size
    	          sceneViewer.screenshot((d.width, d.height), 4, file)
    	          log("Wrote image to %s" format file.getName)
    	        }
    	        case _ => {}
    	      }
    	    }) { accelerator = "control I" },
      
    	    new Separator,
    	    new ActionMenuItem("Exit", System.exit(0))
          )
        }
      }
      pack
      visible = true
    }
  }
}
