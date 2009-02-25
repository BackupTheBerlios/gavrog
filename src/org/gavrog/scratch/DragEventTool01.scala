package org.gavrog.scratch

import java.awt.Color

import scala.collection.mutable.ArrayBuffer

import de.jreality.geometry.Primitives
import de.jreality.scene.{Appearance, SceneGraphComponent}
import de.jreality.scene.data.{Attribute, StorageModel}
import de.jreality.shader.{DefaultLineShader, DefaultPointShader, ShaderUtility}
import de.jreality.tools.{DragEventTool, PointDragEvent, PointDragListener}
import de.jreality.ui.viewerapp.ViewerApp

object DragEventTool01 {
  def main(args: Array[String]) {
    ViewerApp.display(new SceneGraphComponent {
      setGeometry(Primitives.icosahedron)
      
      setAppearance(new Appearance {
        val dgs = ShaderUtility.createDefaultGeometryShader(this, true);
        val dls =
          dgs.createLineShader("default").asInstanceOf[DefaultLineShader]
        dls.setDiffuseColor(Color.yellow)
        dls.setTubeRadius(.03)
        val dpts =
          dgs.createPointShader("default").asInstanceOf[DefaultPointShader]
        dpts.setDiffuseColor(Color.red)
        dpts.setPointRadius(.05)
      })

      addTool(new DragEventTool {
        addPointDragListener(new PointDragListener {
          def pointDragStart(e: PointDragEvent) =
            println("drag start of vertex no " + e.getIndex)		
          
          def pointDragged(e: PointDragEvent) {
            val pointSet = e.getPointSet
            val points = new Array[Array[Double]](pointSet.getNumPoints)
            pointSet.getVertexAttributes(
              Attribute.COORDINATES).toDoubleArrayArray(points)
            points(e.getIndex) = e.getPosition
            pointSet.setVertexAttributes(
              Attribute.COORDINATES,
              StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(points))
          }	
	
          def pointDragEnd(e: PointDragEvent) {}
        })
      })
    })
  }
}
