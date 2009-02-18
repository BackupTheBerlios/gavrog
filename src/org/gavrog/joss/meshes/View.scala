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

import java.awt.Color
import javax.swing.KeyStroke

import de.jreality.geometry.{Primitives, IndexedFaceSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{Appearance, DirectionalLight,
                          SceneGraphComponent, Transformation}
import de.jreality.shader.CommonAttributes
import de.jreality.util.SceneGraphUtility

import scala.swing.{Action, BorderPanel, FileChooser,
                    MainFrame, Menu, MenuBar, MenuItem}

import SwingSupport._

object View {
  def log(message: String) = System.err.println(message)
  
  class XDouble(d: Double) { def deg = d / 180.0 * Math.Pi }
  implicit def xdouble(d: Double) = new XDouble(d)
  implicit def xint(i: Int) = new XDouble(i)
  
  class ActionMenuItem(name: String, body: => Unit) extends MenuItem(name) {
    action = new Action(name) {
      def apply { body }
    }
    def accelerator = action.accelerator
    def accelerator_=(spec: String) {
      action.accelerator = Some(KeyStroke.getKeyStroke(spec))
    }
  }
  
  def main(args : Array[String]) : Unit = {
    val scene = new SceneGraphComponent {
      setAppearance(new Appearance {
        setAttribute(CommonAttributes.EDGE_DRAW, true)
        setAttribute(CommonAttributes.TUBES_DRAW, false)
        setAttribute(CommonAttributes.LINE_WIDTH, 1.0)
        setAttribute(CommonAttributes.LINE_SHADER + '.' +
                       CommonAttributes.DIFFUSE_COLOR, Color.GRAY)
        setAttribute(CommonAttributes.VERTEX_DRAW, false)
        setAttribute(CommonAttributes.POLYGON_SHADER + '.' +
                       CommonAttributes.DIFFUSE_COLOR, Color.WHITE)
      })
    }

    val viewer = new JRealityViewerComponent(scene) {
      viewerSize = (800, 600)
    }
    
    val loadMeshChooser = new FileChooser
    val screenShotChooser = new FileChooser
    
    val top = new MainFrame {
      title = "Scala Mesh Viewer"
      contents = new BorderPanel {
        add(new scala.swing.Component { override lazy val peer = viewer },
            BorderPanel.Position.Center)
      }
      menuBar = new MenuBar {
        contents += new Menu("File") {
          contents += new ActionMenuItem("Load Mesh ...", {
            loadMeshChooser.showOpenDialog(menuBar) match {
              case FileChooser.Result.Approve => {
                val file = loadMeshChooser.selectedFile
                log("Reading...")
                val meshes = Mesh.read(file.getAbsolutePath)
                log("Converting...")
                invokeAndWait {
                  viewer.pauseRendering
                }
                SceneGraphUtility.removeChildren(scene)
                for (mesh <- meshes) scene.addChild(nodeFromMesh(mesh))
                invokeAndWait {
                  viewer.encompass
                  viewer.startRendering
                }
                log("Done!")
              }
            }
          }) {
            accelerator = "control O"
          }
          contents += new ActionMenuItem("Screen Shot Image ...", {
            screenShotChooser.showSaveDialog(menuBar) match {
              case FileChooser.Result.Approve => {
                log("Taking screenshot ...")
                val file = screenShotChooser.selectedFile
                viewer.screenshot(viewer.viewerSize, 4, file)
                log("Wrote image to %s" format file.getName)
              }
            }
          }) {
            accelerator = "control I"
          }
        }
      }
    }
    top.pack
    top.visible = true
    
    viewer.pauseRendering
    
    viewer.setLight("Main Light",
                    new DirectionalLight { setIntensity(0.8) },
                    new Transformation {
                      MatrixBuilder.euclidean.rotateX(-30 deg).rotateY(-30 deg)
                        .assignTo(this)
                    })
    viewer.setLight("Fill Light",
                    new DirectionalLight { setIntensity(0.2) },
                    new Transformation {
                      MatrixBuilder.euclidean().rotateX(10 deg).rotateY(20 deg)
                        .assignTo(this)
                    })

    viewer.startRendering
  }
  
  def nodeFromMesh(mesh: Mesh) = new SceneGraphComponent(mesh.name) {
    setGeometry(new IndexedFaceSetFactory {	
      setVertexCount(mesh.numberOfVertices)
      setFaceCount(mesh.numberOfFaces)
      setVertexCoordinates(mesh.vertices.map(v =>
        Array(v.x, v.y, v.z)).toList.toArray)
      setFaceIndices(mesh.faces.map(f =>
        f.vertexChambers.map(c => c.vertexNr-1).toArray).toList.toArray)
      setGenerateEdgesFromFaces(true)
      setGenerateFaceNormals(true)
      // setGenerateVertexNormals(true)
      update
    }.getIndexedFaceSet)
  }
}
