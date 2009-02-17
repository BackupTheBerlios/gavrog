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

import java.awt.{Color, Dimension}
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{JFrame, JMenu, JMenuBar}

import de.jreality.geometry.{Primitives, IndexedFaceSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{Appearance,
                          DirectionalLight,
                          SceneGraphComponent,
                          Transformation}
import de.jreality.shader.CommonAttributes

object View {
  def log(message: String) = System.err.println(message)
  
  class XDouble(d: Double) { def deg = d / 180.0 * Math.Pi }
  implicit def xdouble(d: Double) = new XDouble(d)
  implicit def xint(i: Int) = new XDouble(i)

  def main(args : Array[String]) : Unit = {
    val content = new SceneGraphComponent
    val frame = new JFrame
    val viewer = new JRealityViewerComponent(content)
    
    val a = new Appearance
    a.setAttribute(CommonAttributes.EDGE_DRAW, true)
    a.setAttribute(CommonAttributes.TUBES_DRAW, false)
    a.setAttribute(CommonAttributes.LINE_WIDTH, 1.0)
    a.setAttribute(CommonAttributes.LINE_SHADER + '.' +
                     CommonAttributes.DIFFUSE_COLOR, Color.GRAY)
    a.setAttribute(CommonAttributes.VERTEX_DRAW, false)
    a.setAttribute(CommonAttributes.POLYGON_SHADER + '.' +
                     CommonAttributes.DIFFUSE_COLOR, Color.WHITE)
    content.setAppearance(a)

    frame.addWindowListener(new WindowAdapter() {
	  override def windowClosing(arg0: WindowEvent) = System.exit(0)
    })
    frame.setJMenuBar(new JMenuBar)
    frame.getJMenuBar.add(new JMenu("File"))
    
    val l1 = new DirectionalLight
    l1.setIntensity(0.8)
    val t1 = new Transformation
    MatrixBuilder.euclidean.rotateX(-30 deg).rotateY(-30 deg).assignTo(t1)
    viewer.setLight("Main Light", l1, t1)

    val l2 = new DirectionalLight()
    l2.setIntensity(0.2)
    val t2 = new Transformation()
    MatrixBuilder.euclidean().rotateX(10 deg).rotateY(20 deg).assignTo(t2)
    viewer.setLight("Fill Light", l2, t2)

    viewer.viewerSize = new Dimension(800, 600)
    frame.getContentPane.add(viewer)
    frame.pack
    frame.setVisible(true)
    
    viewer.pauseRendering
    log("Reading...")
    val meshes =
      if (args.length > 0) Mesh.read(args(0)) else Mesh.read(System.in)

    log("Converting...")
    for (mesh <- meshes) {
      val ifsf = new IndexedFaceSetFactory
      ifsf setVertexCount mesh.numberOfVertices
      ifsf setFaceCount   mesh.numberOfFaces
      ifsf setVertexCoordinates
        mesh.vertices.map(v => Array(v.x, v.y, v.z)).toList.toArray
      ifsf setFaceIndices
        mesh.faces.map(f => f.vertexChambers.map(c => c.vertexNr-1).toArray).
        toList.toArray
      ifsf setGenerateEdgesFromFaces true
      ifsf setGenerateFaceNormals    true
      //ifsf setGenerateVertexNormals  true
      ifsf.update

      val obj = new SceneGraphComponent(mesh.name)
      obj.setGeometry(ifsf.getIndexedFaceSet())
      content.addChild(obj)
    }
    viewer.encompass
    viewer.startRendering
    log("Taking screenshot...")
    viewer.screenshot(viewer.viewerSize, 4, "x.png")
    log("Done!")
  }
}
