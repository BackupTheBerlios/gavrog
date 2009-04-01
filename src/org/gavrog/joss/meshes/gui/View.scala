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

import de.jreality.geometry.{IndexedFaceSetFactory, IndexedLineSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{DirectionalLight, SceneGraphComponent}
import de.jreality.scene.tool.{AbstractTool, InputSlot, ToolContext}
import de.jreality.shader.CommonAttributes._
import de.jreality.tools.ClickWheelCameraZoomTool
import de.jreality.util.SceneGraphUtility

import scala.collection.mutable.HashMap
import scala.swing.{BorderPanel, FileChooser, MainFrame, Menu, MenuBar,
                    Orientation, Separator, SplitPane, TextArea}
import scala.swing.event.MouseEntered

import JRealitySupport._
import Sums._
import SwingSupport._
import Vectors._

object View {
  implicit def xdouble(d: Double) = new { def deg = d / 180.0 * Math.Pi }
  implicit def xint(i: Int) = new { def deg = i / 180.0 * Math.Pi }

  implicit def wrapIter[T](it: java.util.Iterator[T]) = new Iterator[T] {
    def hasNext = it.hasNext
    def next = it.next
  }
  implicit def wrap[T](it: java.lang.Iterable[T]) = wrapIter(it.iterator)
  
  def log(message: String) = statusLine.text = message
  
  val meshFaceAttributes = Map(
    EDGE_DRAW                                   -> true,
    TUBES_DRAW                                  -> false,
    VERTEX_DRAW                                 -> false,
    FACE_DRAW                                   -> true,
    POLYGON_SHADER + '.' + DIFFUSE_COLOR        -> Color.WHITE,
    POLYGON_SHADER + '.' + SPECULAR_COEFFICIENT -> 0.1,
    SMOOTH_SHADING                              -> false,
    DEPTH_FUDGE_FACTOR                          -> 0.9999,
    LINE_WIDTH                                  -> 1.0,
    LINE_SHADER + '.' + DIFFUSE_COLOR           -> new Color(0.1f, 0.1f, 0.1f),
    LINE_SHADER + '.' + SPECULAR_COEFFICIENT    -> 0.0,
    // the following make sure no imaginary tubes and sphere are picked
    TUBE_RADIUS                                 -> 0.00001,
    POINT_RADIUS                                -> 0.00001
  )
  
  val loadMeshChooser = new FileChooser
  val screenShotChooser = new FileChooser
  
  val statusLine = new TextArea(1, 80) { editable = false }
  val sceneViewer = new MeshViewer

  val uvMapViewer = new JRealityViewerComponent {
    addTool(new PanTool)
    addTool(new ClickWheelCameraZoomTool)
    
    perspective = false
    var front_to_back = List[SceneGraphComponent]()
    var selection = Set[SceneGraphComponent]()
    var hidden = List[Set[SceneGraphComponent]]()
    var hot: SceneGraphComponent = null
    
    background_color = Color.LIGHT_GRAY
    size = (600, 800)
    setLight("Main Light",
             new DirectionalLight { setIntensity(1.0) },
             MatrixBuilder.euclidean)
    scene.addChild(grid)
    super.encompass
    
    def update_z_order = modify {
      for ((node, z) <- front_to_back.zipWithIndex) node.setTransformation(
        MatrixBuilder.euclidean.translate(0, 0, -0.01 * z))
    }
    
    def setMesh(mesh: Mesh) = modify {
      SceneGraphUtility.removeChildren(scene)
      for (chart <- mesh.charts)
        scene.addChild(new UVsGeometry(chart, "uv-chart") {
          setAppearance(new RichAppearance(meshFaceAttributes))
          front_to_back = this :: front_to_back
        })
      update_z_order
      scene.addChild(grid)
      encompass
    }
    
    override def encompass {
      grid.setVisible(false)
      super.encompass
      grid.setVisible(true)
    }
    
    def encompassSelected {
      var hidden = Set[SceneGraphComponent](grid)
      grid.setVisible(false)
      if (!selection.isEmpty)
        for (sgc <- scene.getChildComponents
             if (!selection.contains(sgc) && sgc.isVisible)) {
          hidden += sgc
          sgc.setVisible(false)
        }
      super.encompass
      for (sgc <- hidden) sgc.setVisible(true)
    }
    
    override def rotateScene(axis: Vec3, angle: Double) =
      super.rotateScene(new Vec3(0, 0, 1), angle)
    override def viewFrom(eye: Vec3, up: Vec3) =
      super.viewFrom(new Vec3(0, 0, 1), up)
    
    def set_color(sgc: SceneGraphComponent, c: Color) =
      sgc.getAppearance.setAttribute(POLYGON_SHADER + '.' + DIFFUSE_COLOR, c)
    
    def select(sgc: SceneGraphComponent) {
      set_color(sgc, Color.RED)
      selection += sgc
      clear_hot
    }
    def deselect(sgc: SceneGraphComponent) {
      set_color(sgc, Color.WHITE)
      selection -= sgc
      clear_hot
    }
    def clear_hot = if (hot != null) {
      set_color(hot, if (selection.contains(hot)) Color.RED else Color.WHITE)
      hot = null
    }
    def make_hot(sgc: SceneGraphComponent) = if (hot != sgc) {
      clear_hot
      set_color(sgc, if (selection.contains(sgc)) Color.YELLOW else Color.GREEN)
      hot = sgc
    }
    def hide(sgc: SceneGraphComponent) {
      sgc.setVisible(false)
      deselect(sgc)
    }
    def show(sgc: SceneGraphComponent) {
      sgc.setVisible(true)
    }
    def push_to_back(sgc: SceneGraphComponent) {
      front_to_back = (front_to_back - sgc) ::: List(sgc)
      update_z_order
    }
    def pull_to_front(sgc: SceneGraphComponent) {
      front_to_back = sgc :: (front_to_back - sgc)
      update_z_order
    }
    
    addTool(new AbstractTool {
      addCurrentSlot(InputSlot.getDevice("PointerTransformation")) // mouse move
      
      override def perform(tc: ToolContext) = modify {
        val pr = tc.getCurrentPick
        if (pr == null) clear_hot
        else make_hot(pr.getPickPath.getLastComponent)
      }
    })
    
    addTool(new AbstractTool(InputSlot.getDevice("PrimaryAction")) { // mouse 1
      override def activate(tc: ToolContext) {
        val pr = tc.getCurrentPick
        if (pr == null) return
        val sgc = pr.getPickPath.getLastComponent
        modify { if (selection contains sgc) deselect(sgc) else select(sgc) }
      }
    })
    
    listenTo(keyClicks, Mouse.clicks, Mouse.moves)
    reactions += {
      case MouseEntered(src, _, _) if (src == this) => requestFocus
      case KeyPressed(src, modifiers, code, _) if (src == this) => {
        val key = java.awt.event.KeyEvent.getKeyText(code)
        val mod = java.awt.event.KeyEvent.getKeyModifiersText(modifiers)
    	val txt = mod + (if (mod.size > 0) "-" else "") + key
    	txt match {
    	  case "Ctrl-Left"  => modify { rotateScene(Vec3(0, 0, 1), 5 deg) }
    	  case "Ctrl-Right" => modify { rotateScene(Vec3(0, 0, 1), -5 deg) }
      	  case "Home"       => modify {
      	    viewFrom(Vec3(0, 0, 1), Vec3(0, 1, 0))
            encompass
      	  }
    	  case "0"     => modify { encompassSelected }
      	  case "Space" => modify { selection map deselect }
      	  case "B"     => modify { selection map push_to_back }
      	  case "F"     => modify { selection map pull_to_front }
      	  case "H"     => if (selection.size > 0) modify {
            hidden = (Set() ++ selection) :: hidden
      	  	selection map hide
      	  }
      	  case "U"     => hidden match {
      	    case last_batch :: rest => modify {
      	      last_batch map show
      	      hidden = rest
      	    }
      	    case Nil => ()
      	  }
      	  case _  => log(txt)
    	}
      }
    }
  }

  var active = List(sceneViewer, uvMapViewer)
  
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
  
  object grid extends SceneGraphComponent("Grid") {
    setGeometry(new IndexedLineSetFactory {
      setVertexCount(16)
      setLineCount(8)
      setVertexCoordinates(Array[Array[Double]](
        Array(-1, -1, 0), Array(-1,  0, 0), Array(-1,  1, 0), Array(-1,  2, 0),
        Array( 2, -1, 0), Array( 2,  0, 0), Array( 2,  1, 0), Array( 2,  2, 0),
        Array(-1, -1, 0), Array( 0, -1, 0), Array( 1, -1, 0), Array( 2, -1, 0),
        Array(-1,  2, 0), Array( 0,  2, 0), Array( 1,  2, 0), Array( 2,  2, 0)
      ))
      setEdgeIndices(Array(
        Array( 0,  4), Array( 1,  5), Array( 2,  6), Array( 3,  7),
        Array( 8, 12), Array( 9, 13), Array(10, 14), Array(11, 15)))
      update
    }.getIndexedLineSet)
    setPickable(false)
    setAppearance(new RichAppearance(
      FACE_DRAW                                -> false,
      EDGE_DRAW                                -> true,
      TUBES_DRAW                               -> false,
      VERTEX_DRAW                              -> false,
      LINE_WIDTH                               -> 1.0,
      LINE_SHADER + '.' + DIFFUSE_COLOR        -> Color.BLUE,
      LINE_SHADER + '.' + SPECULAR_COEFFICIENT -> 0.0,
      PICKABLE                                 -> false
    ))
  }
}
