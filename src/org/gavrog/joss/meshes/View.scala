package org.gavrog.joss.meshes

import java.awt.{Color, Dimension}
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{JMenu, JMenuBar}

import de.jreality.geometry.{Primitives, IndexedFaceSetFactory}
import de.jreality.math.MatrixBuilder
import de.jreality.scene.{Appearance,
                          DirectionalLight,
                          SceneGraphComponent,
                          Transformation}
import de.jreality.shader.CommonAttributes

import org.gavrog.apps._3dt.ViewerFrame

object View {
  def log(message: String) = System.err.println(message)
  
  class XDouble(d: Double) { def deg = d / 180.0 * Math.Pi }
  implicit def xdouble(d: Double) = new XDouble(d)
  implicit def xint(i: Int) = new XDouble(i)

  def main(args : Array[String]) : Unit = {
    val content = new SceneGraphComponent
    val frame = new ViewerFrame(content)
    
    val a = new Appearance
    a.setAttribute(CommonAttributes.EDGE_DRAW, false)
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
    frame.setLight("Main Light", l1, t1)

    val l2 = new DirectionalLight()
    l2.setIntensity(0.2)
    val t2 = new Transformation()
    MatrixBuilder.euclidean().rotateX(10 deg).rotateY(20 deg).assignTo(t2)
    frame.setLight("Fill Light", l2, t2)

    frame.validate
    frame.setVisible(true)
    frame.startRendering

    frame.setViewerSize(new Dimension(800, 600))
    
    frame.pauseRendering
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
    frame.encompass
    frame.startRendering
    log("Done!")
  }
}
