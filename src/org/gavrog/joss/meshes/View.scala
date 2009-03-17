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

import java.awt.{Color, Point}
import Color._

import de.jreality.geometry.{IndexedFaceSetFactory, IndexedLineSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{Appearance, DirectionalLight, SceneGraphComponent,
                          Transformation}
import de.jreality.scene.tool.{AbstractTool, InputSlot, ToolContext}
import de.jreality.shader.CommonAttributes._
import de.jreality.tools.{DraggingTool, RotateTool, ClickWheelCameraZoomTool}
import de.jreality.util.SceneGraphUtility

import scala.collection.mutable.HashMap
import scala.swing.{BorderPanel, FileChooser, MainFrame, Menu, MenuBar, Orientation,
                    Separator, SplitPane, TextArea}
import scala.swing.event._

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
  
  def log(message: String) = System.err.println(message)
  
  val meshFaceAttributes = Map(
    EDGE_DRAW                                   -> true,
    TUBES_DRAW                                  -> false,
    VERTEX_DRAW                                 -> false,
    FACE_DRAW                                   -> true,
    POLYGON_SHADER + '.' + DIFFUSE_COLOR        -> WHITE,
    POLYGON_SHADER + '.' + SPECULAR_COEFFICIENT -> 0.1,
    SMOOTH_SHADING                              -> false,
    DEPTH_FUDGE_FACTOR                          -> 0.9999,
    LINE_WIDTH                                  -> 1.0,
    LINE_SHADER + '.' + DIFFUSE_COLOR           -> new Color(0.1f, 0.1f, 0.1f),
    LINE_SHADER + '.' + SPECULAR_COEFFICIENT    -> 0.0
  )
  
  val loadMeshChooser = new FileChooser
  val screenShotChooser = new FileChooser
  
  val statusLine = new TextArea(1, 80)
  
  val sceneViewer = new JRealityViewerComponent {
    size = (600, 800)
    setLight("Main Light", new DirectionalLight { setIntensity(0.8) },
             MatrixBuilder.euclidean.rotateX(-30 deg).rotateY(-30 deg))
    setLight("Fill Light", new DirectionalLight { setIntensity(0.2) },
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

  val uvMapViewer = new JRealityViewerComponent(new DraggingTool,
                                                new ClickWheelCameraZoomTool) {
    var front_to_back: List[SceneGraphComponent] = Nil
    
    background_color = LIGHT_GRAY
    size = (600, 800)
    setLight("Main Light",
             new DirectionalLight { setIntensity(1.0) },
             MatrixBuilder.euclidean)
    fieldOfView = 0.01
    
    def setMesh(mesh: Mesh) = modify {
      SceneGraphUtility.removeChildren(scene)
      for (chart <- mesh.charts)
        scene.addChild(new UVsGeometry(chart, "uv-chart") {
          setAppearance(new RichAppearance(meshFaceAttributes))
          front_to_back = this :: front_to_back
        })
      update_z_order
      encompass
    }
    
    def update_z_order = modify {
      for ((node, z) <- front_to_back.zipWithIndex)
        node.setTransformation(MatrixBuilder.euclidean.translate(0, 0, -z))
    }
    
    addTool(new AbstractTool {
      val activationSlot = InputSlot.getDevice("PrimaryAction") // Mouse left
      addCurrentSlot(activationSlot)
        
      override def perform(tc: ToolContext) {
        if (tc.getAxisState(activationSlot).isReleased) return
        val pr = tc.getCurrentPick
        if (pr == null) return
        pr.getPickPath.iterator.find(_.getName.startsWith("uv-chart")) match {
          case None => ()
          case Some(node) => modify {
            val app = node.asInstanceOf[SceneGraphComponent].getAppearance
            val key = POLYGON_SHADER + '.' + DIFFUSE_COLOR
            if (app.getAttribute(key) == Color.RED)
              app.setAttribute(key, Color.WHITE)
            else {
              app.setAttribute(key, Color.RED)
              val sgc = node.asInstanceOf[SceneGraphComponent]
              front_to_back = sgc :: (front_to_back - sgc)
              update_z_order
            }
          }
        }
      }
    })
    
    addTool(new AbstractTool {
      val activationSlot = InputSlot.getDevice("JumpActivation") // Space
      addCurrentSlot(activationSlot)
      
      override def perform(tc: ToolContext) {
        if (!tc.getAxisState(activationSlot).isReleased) modify {
          val key = POLYGON_SHADER + '.' + DIFFUSE_COLOR
          for (sgc <- front_to_back) sgc.getAppearance.setAttribute(key, WHITE)
        }
      }
    })
  }

  def main(args : Array[String]) : Unit = {
    val top = new MainFrame {
      title    = "Scala Mesh Viewer"
      contents = new BorderPanel {
        add(new SplitPane(Orientation.Vertical, sceneViewer, uvMapViewer) {
          continuousLayout = true
        }, BorderPanel.Position.North)
        add(statusLine, BorderPanel.Position.South)
      }
      menuBar  = new MenuBar { contents ++ List(fileMenu, viewMenu) }
      
      def report(src: AnyRef, p: Point, action: String) {
        statusLine.text = "  mouse %s, position %4d,%4d in %s" format (
          action, p.x, p.y,
          src match {
            case `sceneViewer` => "model"
            case `uvMapViewer` => "uvs"
            case _             => "unknown"
          }
        )
      }

      listenTo(sceneViewer.mouse.clicks, sceneViewer.mouse.moves)
      listenTo(uvMapViewer.mouse.clicks, uvMapViewer.mouse.moves)
      reactions += {
        case MouseEntered (src, pt, _)       => report(src, pt, "entered")
        case MouseExited  (src, pt, _)       => report(src, pt, "exited")
        case MouseDragged (src, pt, _)       => report(src, pt, "dragged")
        case MousePressed (src, pt, _, _, _) => report(src, pt, "pressed")
        case MouseReleased(src, pt, _, _, _) => report(src, pt, "released")
        case MouseClicked (src, pt, _, _, _) => report(src, pt, "clicked")
      }
    }
    top.pack
    top.visible = true
  }
  
  def fileMenu = new Menu("File") {
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
      }) { accelerator = "control I" }
    )
  }
  
  def viewMenu = new Menu("View") {
    implicit def as_vec3(t: (Int, Int, Int)) = Vec3(t._1, t._2, t._3)
    def item(name: String, key: String, code: => unit) : ActionMenuItem =
      new ActionMenuItem(name, code) { accelerator = key }
    def item(name: String, key: String,
             eye: (Int, Int, Int), up: (Int, Int, Int)) : ActionMenuItem =
      item(name, key, sceneViewer.viewFrom(eye, up))
    def item(name: String, key: String,
             axis: (Int, Int, Int), angle: Double) : ActionMenuItem =
      item(name, key, sceneViewer.rotateScene(axis, angle))
    
    contents ++ List(
      item("Home", "H", { sceneViewer.viewFrom(Vec3(0, 0, 1), Vec3(0, 1, 0))
                          sceneViewer.fieldOfView = 25.0
                          sceneViewer.encompass }),
      item("Fit", "0", { sceneViewer.fieldOfView = 25.0
                         sceneViewer.encompass }),
      new Separator,
      item("View From +X", "X",       ( 1, 0, 0), ( 0, 1, 0)),
      item("View From +Y", "Y",       ( 0, 1, 0), ( 0, 0,-1)),
      item("View From +Z", "Z",       ( 0, 0, 1), ( 0, 1, 0)),
      item("View From -X", "shift X", (-1, 0, 0), ( 0, 1, 0)),
      item("View From -Y", "shift Y", ( 0,-1, 0), ( 0, 0, 1)),
      item("View From -Z", "shift Z", ( 0, 0,-1), ( 0, 1, 0)),
      new Separator,
      item("Rotate Left",             "alt LEFT",      (0, 1, 0), -5 deg),
      item("Rotate Right",            "alt RIGHT",     (0, 1, 0),  5 deg),
      item("Rotate Up",               "alt UP",        (1, 0, 0), -5 deg),
      item("Rotate Down",             "alt DOWN",      (1, 0, 0),  5 deg),
      item("Rotate Clockwise",        "control RIGHT", (0, 0, 1), -5 deg),
      item("Rotate Counterclockwise", "control LEFT",  (0, 0, 1),  5 deg)
    )
  }
  
  class MeshGeometry(mesh: Mesh)
  extends SceneGraphComponent(mesh.name) {
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
