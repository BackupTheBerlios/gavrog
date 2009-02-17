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

import java.awt.{Color, Component, Dimension, Graphics2D, Image, RenderingHints}
import java.awt.image.BufferedImage
import java.io.File
import javax.media.opengl.GLException
import javax.swing.{JFrame, SwingUtilities}

import scala.collection.mutable.HashMap

import de.jreality.geometry.GeometryUtility
import de.jreality.math.{Matrix, MatrixBuilder}
import de.jreality.scene.{Appearance,
                          Camera,
                          Light,
                          SceneGraphComponent,
                          SceneGraphPath,
                          Transformation,
                          Viewer}
import de.jreality.shader.CommonAttributes
import de.jreality.softviewer.SoftViewer
import de.jreality.tools.{DraggingTool, RotateTool, ClickWheelCameraZoomTool}
import de.jreality.toolsystem.ToolSystem
import de.jreality.util.{CameraUtility, ImageUtility, RenderTrigger}

class JRealityViewerComponent(content: SceneGraphComponent) extends JFrame {
  implicit def asRunnable(body: => unit) = new Runnable() { def run { body } }
  def invokeAndWait(body: => unit) =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeAndWait(body)
  def invokeLater(body: => unit) =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeLater(body)
  
  private val rootNode     = new SceneGraphComponent
  private val cameraNode   = new SceneGraphComponent
  private val geometryNode = new SceneGraphComponent
  private val lightNode    = new SceneGraphComponent
  private val contentNode  = content
	
  private val lights = new HashMap[String, SceneGraphComponent]
	
  private val renderTrigger  = new RenderTrigger

  private var currentViewer: Viewer = null
  private var lastCenter: List[Double] = null

  rootNode     addChild geometryNode
  rootNode     addChild cameraNode
  rootNode     addChild lightNode
  geometryNode addChild contentNode

  contentNode  addTool  new RotateTool
  contentNode  addTool  new DraggingTool
  contentNode  addTool  new ClickWheelCameraZoomTool

  private val camera = new Camera
  cameraNode.setCamera(camera)
  MatrixBuilder.euclidean translate (0, 0, 3) assignTo cameraNode

  private val rootApp = new Appearance
  rootApp.setAttribute(CommonAttributes.BACKGROUND_COLOR, Color.DARK_GRAY)
  rootApp.setAttribute(CommonAttributes.DIFFUSE_COLOR, Color.RED)
  rootNode.setAppearance(rootApp)

  private val camPath = new SceneGraphPath(rootNode, cameraNode)
  camPath.push(camera)
  private val emptyPickPath =
    new SceneGraphPath(rootNode, geometryNode, contentNode)

  private val softwareViewer = new SoftViewer
  softwareViewer.setSceneRoot(rootNode)
  softwareViewer.setCameraPath(camPath)
  setupToolSystem(softwareViewer, emptyPickPath);

  viewer = try {
    val v = new de.jreality.jogl.Viewer()
    v.setSceneRoot(rootNode)
    v.setCameraPath(camPath)
    setupToolSystem(v, emptyPickPath)
    v
  } catch {
    case ex: Exception => {
      System.err.println("OpenGL viewer could not be initialized.")
      softwareViewer
    }
  }
  
  viewerSize = new Dimension(640, 400)
  pack
  
  if (viewer.isInstanceOf[de.jreality.jogl.Viewer]) invokeAndWait {
    try {
      viewer.asInstanceOf[de.jreality.jogl.Viewer].run
      System.err.println("OpenGL okay!")
    } catch {
      case ex: GLException => {
        System.err.println("OpenGL viewer could not render.");
        viewer = softwareViewer
      }
      case ex: Exception => ex.printStackTrace
    }
  }

  renderTrigger.addSceneGraphComponent(rootNode)

  private def setupToolSystem(viewer: Viewer, emptyPickPath: SceneGraphPath) {
    val ts = ToolSystem.toolSystemForViewer(viewer)
    ts.initializeSceneTools()
    ts.setEmptyPickPath(emptyPickPath)
  }
  
  def viewingComponent = viewer.getViewingComponent.asInstanceOf[Component]
 
  def viewer = currentViewer
  
  def viewer_=(newViewer: Viewer) {
    val d = viewerSize
    this.renderTrigger.removeViewer(viewer)
    this.renderTrigger.addViewer(newViewer)
    this.currentViewer = newViewer
    getContentPane().removeAll()
    getContentPane().add(viewingComponent)
    viewerSize = d
  }

  def viewerSize = if (currentViewer == null) new Dimension(0, 0)
                   else currentViewer.getViewingComponentSize
  
  def viewerSize_=(d: Dimension) {
    viewingComponent setPreferredSize d
    pack
  }

  def setLight(name: String, light: Light, t: Transformation) {
    val node = lights.get(name) match {
      case Some(node) => node
      case None => {
        val node = new SceneGraphComponent
        lights(name) = node
        lightNode.addChild(node)
        node
      }
    }
    node.setLight(light)
    node.setTransformation(t)
  }
  
  def removeLight(name: String) {
    lights.get(name) match {
      case Some(node) => {
        lightNode.removeChild(node)
        lights -= name
      }
      case None => {}
    }
  }
 
  def startRendering = renderTrigger.finishCollect
  def pauseRendering = renderTrigger.startCollect

  def encompass {
    // -- extract parameters from scene and viewer
    val ts = ToolSystem.toolSystemForViewer(viewer)
    val avatarPath = ts.getAvatarPath
    val scenePath = ts.getEmptyPickPath
    val cameraPath = viewer.getCameraPath
    val aspectRatio = CameraUtility.getAspectRatio(viewer)

    // -- compute scene-to-avatar transformation
    val toAvatar = new Matrix
    scenePath.getMatrix(toAvatar.getArray, 0, scenePath.getLength - 2)
    toAvatar.multiplyOnRight(avatarPath.getInverseMatrix(null))

    // -- compute bounding box of scene
    val bounds = GeometryUtility.calculateBoundingBox(
      toAvatar.getArray, scenePath.getLastComponent)
    if (bounds.isEmpty) return

    // -- compute best camera position based on bounding box and viewport
    val camera = cameraPath.getLastElement.asInstanceOf[Camera]
	val vp = CameraUtility.getViewport(camera, aspectRatio)
	val e = bounds.getExtent
	val radius = Math.sqrt(e(0) * e(0) + e(2) * e(2) + e(1) * e(1)) / 2.0
    val front = e(2) / 2.0

    val xscale = e(0) / vp.getWidth
	val yscale = e(1) / vp.getHeight
	var camdist = Math.max(xscale, yscale) * 1.1
	if (!camera.isPerspective) {
	  camdist *= camera.getFocus // adjust for viewport scaling
      camera.setFocus(camdist)
	}

	// -- compute new camera position and adjust near/far clipping planes
    val c = bounds.getCenter
	c(2) += front + camdist
	camera.setFar(camdist + front + 5 * radius)
	camera.setNear(0.1 * camdist)

	// -- make rotateScene() recompute the center
	lastCenter = null

	// -- adjust the avatar position to make scene fit
	val camMatrix = new Matrix
	cameraPath.getInverseMatrix(camMatrix.getArray, avatarPath.getLength)
	val avatar = avatarPath.getLastComponent
	val mb = MatrixBuilder.euclidean(new Matrix(avatar.getTransformation))
    mb.translate(c).translate(camMatrix.getColumn(3)).assignTo(avatar)
  }
  
  def screenshot(size: Dimension, antialias: Int, file: String) {
    val width = size.width
    val height = size.height
    val img =
      softwareViewer.renderOffscreen(width * antialias, height * antialias)
    val scaledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val gr = scaledImg.getGraphics.asInstanceOf[Graphics2D]
    gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    scaledImg.getGraphics.drawImage(
      img.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null)
    ImageUtility.writeBufferedImage(new File(file), scaledImg)
  }
}
