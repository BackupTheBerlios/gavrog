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
import java.awt.event.{MouseEvent, MouseMotionListener}
import Color._

import de.jreality.geometry.{IndexedFaceSetFactory, IndexedLineSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{Appearance, DirectionalLight,
                          SceneGraphComponent, Transformation}
import de.jreality.shader.CommonAttributes._
import de.jreality.util.SceneGraphUtility

import scala.swing.{BorderPanel, FileChooser, MainFrame, Menu, MenuBar,
                    Separator}

import Mesh._
import Sums._
import SwingSupport._
import Vectors._

object View {
  class XDouble(d: Double) { def deg = d / 180.0 * Math.Pi }
  implicit def xdouble(d: Double) = new XDouble(d)
  implicit def xint(i: Int) = new XDouble(i)
  
  implicit def asArray[A](it: Iterator[A]) = it.toList.toArray
  def max(xs: Iterator[Double]) = (0.0 /: xs)((x, y) => if (x > y) x else y)
      
  def log(message: String) = System.err.println(message)
  
  implicit def asTransformation(mb: MatrixBuilder) = new Transformation {
    mb.assignTo(this)
  }
  
  class RichAppearance extends Appearance {
    def update(attr: (String, Any)*) {
      for ((k, v) <- attr) setAttribute(k, v)
    }
  }
  
  val loadMeshChooser = new FileChooser
  val screenShotChooser = new FileChooser
    
  val scene = new SceneGraphComponent
  
  val viewer = new JRealityViewerComponent(scene) {
    viewerSize = (800, 600)
    setLight("Main Light",
             new DirectionalLight { setIntensity(0.8) },
             MatrixBuilder.euclidean.rotateX(-30 deg).rotateY(-30 deg))
    setLight("Fill Light",
             new DirectionalLight { setIntensity(0.2) },
             MatrixBuilder.euclidean().rotateX(10 deg).rotateY(20 deg))
  }

  def main(args : Array[String]) : Unit = {
    val top = new MainFrame {
      title = "Scala Mesh Viewer"
      contents = new BorderPanel {
        add(new scala.swing.Component { override lazy val peer = viewer },
            BorderPanel.Position.Center)
      }
      menuBar = new MenuBar {
        contents ++ List(fileMenu, viewMenu)
      }
    }
    top.pack
    top.visible = true
    
//    viewer.viewingComponent.addMouseMotionListener(new MouseMotionListener {
//       def mouseMoved(e: MouseEvent) {
//        val p = e.getPoint
//        System.err.println("mouse moved to %4d,%4d" format (p.x, p.y))
//      }
//      def mouseDragged(e: MouseEvent) {
//        val p = e.getPoint
//        System.err.println("mouse dragged to %4d,%4d" format (p.x, p.y))
//      }
//    })
  }
  
  def fileMenu = new Menu("File") {
    contents ++ List(
      
      new ActionMenuItem("Load Mesh ...", {
        loadMeshChooser.showOpenDialog(this) match {
          case FileChooser.Result.Approve => {
            val file = loadMeshChooser.selectedFile
            log("Reading...")
            val meshes = Mesh.read(file.getAbsolutePath)
            log("Converting...")
            invokeAndWait {
              viewer.pauseRendering
            }
            SceneGraphUtility.removeChildren(scene)
            for (mesh <- meshes) {
              scene.addChild(faceSetFromMesh(mesh))
              scene.addChild(lineSetFromMesh(mesh))
            }
            invokeAndWait {
              viewer.encompass
              viewer.startRendering
            }
            log("Done!")
          }
        }
      }) { accelerator = "control O" },
      
      new ActionMenuItem("Take Screen Shot ...", {
        screenShotChooser.showSaveDialog(this) match {
          case FileChooser.Result.Approve => {
            log("Taking screenshot ...")
            val file = screenShotChooser.selectedFile
            viewer.screenshot(viewer.viewerSize, 4, file)
            log("Wrote image to %s" format file.getName)
          }
        }
      }) { accelerator = "control I" }
    )
  }
  
  def viewMenu = new Menu("View") {
    contents ++ List(
      new ActionMenuItem("Home", {
        viewer.viewFrom(Vec3(0, 0, 1), Vec3(0, 1, 0))
        viewer.encompass
      }) { accelerator = "H" },
      new ActionMenuItem("Fit", viewer.encompass) { accelerator = "0" },
      new Separator,
      new ActionMenuItem("View From +X",
                         viewer.viewFrom(Vec3(1, 0, 0), Vec3(0, 1, 0))
      ) { accelerator = "X" },
      new ActionMenuItem("View From +Y",
                         viewer.viewFrom(Vec3(0, 1, 0), Vec3(0, 0, -1))
      ) { accelerator = "Y" },
      new ActionMenuItem("View From +Z",
                         viewer.viewFrom(Vec3(0, 0, 1), Vec3(0, 1, 0))
      ) { accelerator = "Z" },
      new ActionMenuItem("View From -X",
                         viewer.viewFrom(Vec3(-1, 0, 0), Vec3(0, 1, 0))
      ) { accelerator = "shift X" },
      new ActionMenuItem("View From -Y",
                         viewer.viewFrom(Vec3(0, -1, 0), Vec3(0, 0, 1))
      ) { accelerator = "shift Y" },
      new ActionMenuItem("View From -Z",
                         viewer.viewFrom(Vec3(0, 0, -1), Vec3(0, 1, 0))
      ) { accelerator = "shift Z" },
      new Separator,
      new ActionMenuItem("Rotate Left",
                         viewer.rotateScene(Vec3(0, 1, 0), -5 deg)
      ) { accelerator = "LEFT" },
      new ActionMenuItem("Rotate Right",
                         viewer.rotateScene(Vec3(0, 1, 0),  5 deg)
      ) { accelerator = "RIGHT" },
      new ActionMenuItem("Rotate Up",
                         viewer.rotateScene(Vec3(1, 0, 0), -5 deg)
      ) { accelerator = "UP" },
      new ActionMenuItem("Rotate Down",
                         viewer.rotateScene(Vec3(1, 0, 0),  5 deg)
      ) { accelerator = "DOWN" },
      new ActionMenuItem("Rotate Clockwise",
                         viewer.rotateScene(Vec3(0, 0, 1), -5 deg)
      ) { accelerator = "control RIGHT" },
      new ActionMenuItem("Rotate Counterclockwise",
                         viewer.rotateScene(Vec3(0, 0, 1),  5 deg)
      ) { accelerator = "control LEFT" }
    )
  }
  
  def faceSetFromMesh(mesh: Mesh) = new SceneGraphComponent(mesh.name) {
    setAppearance(new RichAppearance {
      update(
        EDGE_DRAW                            -> false,
        VERTEX_DRAW                          -> false,
        POLYGON_SHADER + '.' + DIFFUSE_COLOR -> WHITE,
        SMOOTH_SHADING                       -> false
      )
    })
    setGeometry(new IndexedFaceSetFactory {	
      setVertexCount(mesh.numberOfVertices)
      setFaceCount(mesh.numberOfFaces)
      setVertexCoordinates(mesh.vertices.map(_.toArray))
      setFaceIndices(mesh.faces.map(_.vertices.map(_.nr-1).toArray))
      setGenerateEdgesFromFaces(true)
      setGenerateFaceNormals(true)
      setGenerateVertexNormals(true)
      update
    }.getIndexedFaceSet)
  }
  
  def lineSetFromMesh(mesh: Mesh) = new SceneGraphComponent(mesh.name) {
    setAppearance(new RichAppearance {
      update(
      	EDGE_DRAW                         -> true,
      	TUBES_DRAW                        -> false,
      	VERTEX_DRAW                       -> false,
      	LINE_WIDTH                        -> 1.0,
      	LINE_SHADER + '.' + DIFFUSE_COLOR -> new Color(0.1f, 0.1f, 0.1f)
      )
    })
    setGeometry(new IndexedLineSetFactory {
      val n = mesh.numberOfVertices
      val center = mesh.vertices.sum(_.pos) / n
      val radius = max(mesh.vertices.map(v => (v.pos - center).||))
      val f = radius / 1000
      setVertexCount(mesh.numberOfVertices)
      setLineCount(mesh.numberOfEdges)
      mesh.computeNormals
      setVertexCoordinates(
        mesh.vertices.map(v => (v + v.chamber.normal * f).toArray))
      setEdgeIndices(mesh.edges.map(e => Array(e.from.nr-1, e.to.nr-1)))
      update
    }.getIndexedLineSet)
  }
}
