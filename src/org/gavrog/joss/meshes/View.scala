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
import de.jreality.tools.{DraggingTool, RotateTool, ClickWheelCameraZoomTool}
import de.jreality.util.SceneGraphUtility

import scala.swing.{FileChooser, MainFrame, Menu, MenuBar, Orientation,
                    Separator, SplitPane}

import JRealitySupport._
import Mesh._
import Sums._
import SwingSupport._
import Vectors._

object View {
  class XDouble(d: Double) { def deg = d / 180.0 * Math.Pi }
  implicit def xdouble(d: Double) = new XDouble(d)
  implicit def xint(i: Int) = new XDouble(i)
  
  implicit def asArray[A](it: Iterator[A]) = it.toList.toArray

  def max[A <% Ordered[A]](xs: Iterator[A]) =
    xs.reduceLeft((x, y) => if (x > y) x else y)

  def log(message: String) = System.err.println(message)
  
  val meshFaceAttributes = Map(
    EDGE_DRAW                                   -> false,
    VERTEX_DRAW                                 -> false,
    FACE_DRAW                                   -> true,
    POLYGON_SHADER + '.' + DIFFUSE_COLOR        -> WHITE,
    POLYGON_SHADER + '.' + SPECULAR_COEFFICIENT -> 0.1,
    POLYGON_SHADER + '.' + DEPTH_FUDGE_FACTOR   -> 0.0,
    SMOOTH_SHADING                              -> false
  )
  
  val meshLineAttributes = Map(
    EDGE_DRAW                              -> true,
    TUBES_DRAW                             -> false,
    VERTEX_DRAW                            -> false,
    FACE_DRAW                              -> false,
    LINE_WIDTH                             -> 1.0,
    LINE_SHADER + '.' + DIFFUSE_COLOR      -> new Color(0.1f, 0.1f, 0.1f),
    LINE_SHADER + '.' + DEPTH_FUDGE_FACTOR -> 1.0
  )

  val uvsFaceAttributes = Map(
    EDGE_DRAW                                   -> false,
    VERTEX_DRAW                                 -> false,
    FACE_DRAW                                   -> true,
    POLYGON_SHADER + '.' + DIFFUSE_COLOR        -> WHITE,
    POLYGON_SHADER + '.' + SPECULAR_COEFFICIENT -> 0.0,
    POLYGON_SHADER + '.' + DEPTH_FUDGE_FACTOR   -> 0.0,
    SMOOTH_SHADING                              -> false
  )
  
  val loadMeshChooser = new FileChooser
  val screenShotChooser = new FileChooser
    
  val scene  = new SceneGraphComponent
  val layout = new SceneGraphComponent
  
  val sceneViewer = new JRealityViewerComponent(scene) {
    size = (600, 800)
    setLight("Main Light",
             new DirectionalLight { setIntensity(0.8) },
             MatrixBuilder.euclidean.rotateX(-30 deg).rotateY(-30 deg))
    setLight("Fill Light",
             new DirectionalLight { setIntensity(0.2) },
             MatrixBuilder.euclidean.rotateX(10 deg).rotateY(20 deg))
  }

  val layoutViewer =
    new JRealityViewerComponent(layout, List(new DraggingTool,
                                             new ClickWheelCameraZoomTool))
  {
      size = (600, 800)
      setLight("Main Light",
               new DirectionalLight { setIntensity(1.0) },
               MatrixBuilder.euclidean)
  }

  def main(args : Array[String]) : Unit = {
    val top = new MainFrame {
      title    = "Scala Mesh Viewer"
      contents = new SplitPane(Orientation.Vertical, sceneViewer, layoutViewer)
      menuBar  = new MenuBar { contents ++ List(fileMenu, viewMenu) }
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
              sceneViewer.pauseRendering
              layoutViewer.pauseRendering
            }
            SceneGraphUtility.removeChildren(scene)
            for (mesh <- meshes) {
              layout.addChild(new UVsGeometry(mesh) {
                setAppearance(new RichAppearance(uvsFaceAttributes))
              })
              layout.addChild(new UVsGeometry(mesh) {
                setAppearance(new RichAppearance(meshLineAttributes))
                setTransformation(
                  MatrixBuilder.euclidean.translate(0, 0, 1/1000))
              })
              scene.addChild(new MeshGeometry(mesh) {
                setAppearance(new RichAppearance(meshFaceAttributes))
              })
              scene.addChild(new MeshGeometry(mesh) {
                setAppearance(new RichAppearance(meshLineAttributes))
                setTransformation(
                  MatrixBuilder.euclidean.translate(0, 0, 1/1000))
              })
            }
            invokeAndWait {
              sceneViewer.encompass
              layoutViewer.encompass
              sceneViewer.startRendering
              layoutViewer.startRendering
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
            val d = sceneViewer.size
            sceneViewer.screenshot((d.width, d.height), 4, file)
            log("Wrote image to %s" format file.getName)
          }
        }
      }) { accelerator = "control I" }
    )
  }
  
  def viewMenu = new Menu("View") {
    contents ++ List(
      new ActionMenuItem("Home", {
        sceneViewer.viewFrom(Vec3(0, 0, 1), Vec3(0, 1, 0))
        sceneViewer.encompass
      }) { accelerator = "H" },
      new ActionMenuItem("Fit", sceneViewer.encompass) { accelerator = "0" },
      new Separator,
      new ActionMenuItem("View From +X",
                         sceneViewer.viewFrom(Vec3(1, 0, 0), Vec3(0, 1, 0))
      ) { accelerator = "X" },
      new ActionMenuItem("View From +Y",
                         sceneViewer.viewFrom(Vec3(0, 1, 0), Vec3(0, 0, -1))
      ) { accelerator = "Y" },
      new ActionMenuItem("View From +Z",
                         sceneViewer.viewFrom(Vec3(0, 0, 1), Vec3(0, 1, 0))
      ) { accelerator = "Z" },
      new ActionMenuItem("View From -X",
                         sceneViewer.viewFrom(Vec3(-1, 0, 0), Vec3(0, 1, 0))
      ) { accelerator = "shift X" },
      new ActionMenuItem("View From -Y",
                         sceneViewer.viewFrom(Vec3(0, -1, 0), Vec3(0, 0, 1))
      ) { accelerator = "shift Y" },
      new ActionMenuItem("View From -Z",
                         sceneViewer.viewFrom(Vec3(0, 0, -1), Vec3(0, 1, 0))
      ) { accelerator = "shift Z" },
      new Separator,
      new ActionMenuItem("Rotate Left",
                         sceneViewer.rotateScene(Vec3(0, 1, 0), -5 deg)
      ) { accelerator = "LEFT" },
      new ActionMenuItem("Rotate Right",
                         sceneViewer.rotateScene(Vec3(0, 1, 0),  5 deg)
      ) { accelerator = "RIGHT" },
      new ActionMenuItem("Rotate Up",
                         sceneViewer.rotateScene(Vec3(1, 0, 0), -5 deg)
      ) { accelerator = "UP" },
      new ActionMenuItem("Rotate Down",
                         sceneViewer.rotateScene(Vec3(1, 0, 0),  5 deg)
      ) { accelerator = "DOWN" },
      new ActionMenuItem("Rotate Clockwise",
                         sceneViewer.rotateScene(Vec3(0, 0, 1), -5 deg)
      ) { accelerator = "control RIGHT" },
      new ActionMenuItem("Rotate Counterclockwise",
                         sceneViewer.rotateScene(Vec3(0, 0, 1),  5 deg)
      ) { accelerator = "control LEFT" }
    )
  }
  
  class MeshGeometry(mesh: Mesh) extends SceneGraphComponent(mesh.name) {
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
  
  class UVsGeometry(mesh: Mesh) extends SceneGraphComponent(mesh.name) {
    setGeometry(new IndexedFaceSetFactory {	
      setVertexCount(mesh.numberOfTextureVertices)
      setFaceCount(mesh.numberOfFaces)
      setVertexCoordinates(mesh.textureVertices.map(v => Array(v.x, v.y, 0)))
      setFaceIndices(mesh.faces.map(_.textureVertices.map(_.nr-1).toArray))
      setGenerateEdgesFromFaces(true)
      setGenerateFaceNormals(true)
      setGenerateVertexNormals(true)
      update
    }.getIndexedFaceSet)
  }
}
