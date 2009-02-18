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

import scala.swing.{Action, BorderPanel, MainFrame, Menu, MenuBar, MenuItem}

object View {
  def log(message: String) = System.err.println(message)
  
  class XDouble(d: Double) { def deg = d / 180.0 * Math.Pi }
  implicit def xdouble(d: Double) = new XDouble(d)
  implicit def xint(i: Int) = new XDouble(i)
  
  class SimpleAction(name: String,
                     keySpec: Option[String],
                     body: =>Unit) extends Action(name) {
    def apply { body }
    accelerator = keySpec match {
      case Some(s) => Some(KeyStroke.getKeyStroke(s))
      case None    => None
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
    
    val top = new MainFrame {
      title = "Scala Mesh Viewer"
      contents = new BorderPanel {
        add(new scala.swing.Component { override lazy val peer = viewer },
            BorderPanel.Position.Center)
      }
      menuBar = new MenuBar {
        contents += new Menu("File") {
          contents += new MenuItem(
            new SimpleAction("Dummy", Some("control D"),
                             System.err.println("Dummy was selected"))
          )
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

    log("Reading...")
    val meshes =
      if (args.length > 0) Mesh.read(args(0)) else Mesh.read(System.in)

    log("Converting...")
    for (mesh <- meshes) scene.addChild(new SceneGraphComponent(mesh.name) {
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
    })

    viewer.encompass
    viewer.startRendering
    for (i <- 1 to 24) {
      viewer.rotateScene(List(0, 1, 0), 15 deg)
      Thread.sleep(40)
    }
    log("Taking screenshot ...")
    viewer.screenshot(viewer.viewerSize, 4, "x.png")
    log("Done!")
  }
}
