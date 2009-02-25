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
import java.awt.event.{KeyEvent, MouseEvent, MouseMotionListener}
import Color._

import de.jreality.geometry.{IndexedFaceSetFactory, IndexedLineSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{Appearance, DirectionalLight,
                          SceneGraphComponent, Transformation}
import de.jreality.scene.tool.{AbstractTool, InputSlot, ToolContext}
import de.jreality.shader.CommonAttributes._
import de.jreality.tools.{DraggingTool, RotateTool, ClickWheelCameraZoomTool}
import de.jreality.util.SceneGraphUtility

import scala.collection.mutable.HashMap
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
  implicit def asArray[A](it: Iterable[A]) = it.toList.toArray

  implicit def wrapIter[T](it: java.util.Iterator[T]) = new Iterator[T] {
    def hasNext = it.hasNext
    def next = it.next
  }
  
  def max[A <% Ordered[A]](xs: Iterator[A]) =
    xs.reduceLeft((x, y) => if (x > y) x else y)

  def log(message: String) = System.err.println(message)
  
  val meshFaceAttributes = Map(
    EDGE_DRAW                                   -> true,
    TUBES_DRAW                                  -> false,
    VERTEX_DRAW                                 -> false,
    FACE_DRAW                                   -> true,
    POLYGON_SHADER + '.' + DIFFUSE_COLOR        -> WHITE,
    POLYGON_SHADER + '.' + SPECULAR_COEFFICIENT -> 0.1,
    SMOOTH_SHADING                              -> false,
    DEPTH_FUDGE_FACTOR                          -> 0.99998,
    LINE_WIDTH                                  -> 1.0,
    LINE_SHADER + '.' + DIFFUSE_COLOR           -> new Color(0.1f, 0.1f, 0.1f),
    LINE_SHADER + '.' + SPECULAR_COEFFICIENT    -> 0.0
  )
  
  val loadMeshChooser = new FileChooser
  val screenShotChooser = new FileChooser
    
  val scene = new SceneGraphComponent
  val uvMap = new SceneGraphComponent
  
  val sceneViewer = new JRealityViewerComponent(scene) {
    size = (600, 800)
    setLight("Main Light",
             new DirectionalLight { setIntensity(0.8) },
             MatrixBuilder.euclidean.rotateX(-30 deg).rotateY(-30 deg))
    setLight("Fill Light",
             new DirectionalLight { setIntensity(0.2) },
             MatrixBuilder.euclidean.rotateX(10 deg).rotateY(20 deg))
    fieldOfView = 25.0
    
    var center = Array(0.0, 0.0, 0.0, 1.0)
    
    override def computeCenter = center
    
    def setMesh(mesh: Mesh) = modify {
      SceneGraphUtility.removeChildren(scene)
      scene.addChild(new MeshGeometry(mesh) {
        setAppearance(new RichAppearance(meshFaceAttributes))
      })
      center = (mesh.vertices.sum(_.pos) / mesh.numberOfVertices).toArray
      encompass
    }
  }

  val uvMapViewer =
    new JRealityViewerComponent(uvMap, List(new DraggingTool,
                                            new ClickWheelCameraZoomTool))
  {
    size = (600, 800)
    setLight("Main Light",
             new DirectionalLight { setIntensity(1.0) },
             MatrixBuilder.euclidean)
    fieldOfView = 1.0
    
    def setMesh(mesh: Mesh) = modify {
      SceneGraphUtility.removeChildren(uvMap)
      for (chart <- mesh.charts) {
        uvMap.addChild(new UVsGeometry(chart, "uv-chart") {
          setAppearance(new RichAppearance(meshFaceAttributes))
        })
      }
      encompass
    }
    
    addTool(new AbstractTool {
        val activationSlot = InputSlot.getDevice("PrimarySelection");
        val addSlot = InputSlot.getDevice("SecondarySelection");
        val removeSlot = InputSlot.getDevice("Meta");
        addCurrentSlot(activationSlot)
        addCurrentSlot(removeSlot)
        addCurrentSlot(addSlot)
        
        override def perform(tc: ToolContext) {
          if (tc.getAxisState(activationSlot).isReleased) return
          val pr = tc.getCurrentPick
          if (pr == null) return
          val selection = pr.getPickPath
          for (node <- selection.iterator) {
            if (node.getName.startsWith("uv-chart")) modify {
              val app = node.asInstanceOf[SceneGraphComponent].getAppearance
              val key = POLYGON_SHADER + '.' + DIFFUSE_COLOR
              if (app.getAttribute(key) == Color.RED)
                app.setAttribute(key, Color.WHITE)
              else app.setAttribute(key, Color.RED)
            }
          }
        }
    })
  }

  def main(args : Array[String]) : Unit = {
    val top = new MainFrame {
      title    = "Scala Mesh Viewer"
      contents = new SplitPane(Orientation.Vertical, sceneViewer, uvMapViewer)
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
          case FileChooser.Result.Approve => new Thread {
            val file = loadMeshChooser.selectedFile
            log("Reading...")
            val mesh = Mesh.read(file.getAbsolutePath, true)(0)
            log("Converting...")
            sceneViewer.setMesh(mesh)
            uvMapViewer.setMesh(mesh)
            log("Done!")
          }.start
        }
      }) { accelerator = "control O" },
      
      new ActionMenuItem("Take Screen Shot ...", {
        screenShotChooser.showSaveDialog(this) match {
          case FileChooser.Result.Approve => new Thread {
            log("Taking screenshot ...")
            val file = screenShotChooser.selectedFile
            val d = sceneViewer.size
            sceneViewer.screenshot((d.width, d.height), 4, file)
            log("Wrote image to %s" format file.getName)
          }.start
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
      ) { accelerator = "alt LEFT" },
      new ActionMenuItem("Rotate Right",
                         sceneViewer.rotateScene(Vec3(0, 1, 0),  5 deg)
      ) { accelerator = "alt RIGHT" },
      new ActionMenuItem("Rotate Up",
                         sceneViewer.rotateScene(Vec3(1, 0, 0), -5 deg)
      ) { accelerator = "alt UP" },
      new ActionMenuItem("Rotate Down",
                         sceneViewer.rotateScene(Vec3(1, 0, 0),  5 deg)
      ) { accelerator = "alt DOWN" },
      new ActionMenuItem("Rotate Clockwise",
                         sceneViewer.rotateScene(Vec3(0, 0, 1), -5 deg)
      ) { accelerator = "control RIGHT" },
      new ActionMenuItem("Rotate Counterclockwise",
                         sceneViewer.rotateScene(Vec3(0, 0, 1),  5 deg)
      ) { accelerator = "control LEFT" }
    )
  }
  
  class MeshGeometry(mesh: Mesh)
  extends SceneGraphComponent(mesh.name) {
    setGeometry(new IndexedFaceSetFactory {
      mesh.computeNormals
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
  
  class UVsGeometry(chart: Mesh.Chart, name: String)
  extends SceneGraphComponent(name) {
    setGeometry(new IndexedFaceSetFactory {
      val vertices = chart.vertices.toArray
      val toNr = new HashMap[Mesh.TextureVertex, Int]
      for ((v, n) <- vertices.zipWithIndex) toNr(v) = n
      val faces = chart.faces.toArray
      setVertexCount(vertices.size)
      setFaceCount(faces.size)
      setVertexCoordinates(vertices.map(v => Array(v.x, v.y, 0)))
      setFaceIndices(faces.map(_.textureVertices.map(toNr).toArray))
      setGenerateEdgesFromFaces(true)
      setGenerateFaceNormals(true)
      setGenerateVertexNormals(true)
      update
    }.getIndexedFaceSet)
  }
}