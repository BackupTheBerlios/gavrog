/*
   Copyright 2008 Olaf Delgado-Friedrichs

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


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.proxy.scene.Transformation;
import de.jreality.shader.CommonAttributes;
import de.jreality.tools.RotateTool;
import de.jreality.toolsystem.ToolSystem;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class ViewerFrame extends JFrame {
	final SceneGraphComponent lightNode;
	final Thread renderThread;
	
	Viewer viewer;
	Dimension viewerSize;
	boolean renderingEnabled = false;

	public static void main(String args[]) {
		final SceneGraphComponent content = new SceneGraphComponent();
		final IndexedFaceSet ifs = Primitives.icosahedron();
		content.setGeometry(ifs);
		
		final ViewerFrame frame = new ViewerFrame(content);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				System.exit(0);
			}
		});
		frame.setViewerSize(new Dimension(800, 600));
		frame.setJMenuBar(new JMenuBar());
		frame.getJMenuBar().add(new JMenu("File"));
		final Light l1 = new DirectionalLight();
		l1.setIntensity(0.8);
		final Transformation t1 = new Transformation();
		MatrixBuilder.euclidean().rotateX(degrees(-30)).rotateY(degrees(-30))
				.assignTo(t1);
		frame.addLight(l1, t1);
		final Light l2 = new DirectionalLight();
		l2.setIntensity(0.2);
		final Transformation t2 = new Transformation();
		MatrixBuilder.euclidean().rotateX(degrees(10)).rotateY(degrees(20))
				.assignTo(t2);
		frame.addLight(l2, t2);
		
		frame.validate();
		frame.setVisible(true);
		frame.startRendering();
	}

	public ViewerFrame(final SceneGraphComponent content) {
		SceneGraphComponent rootNode = new SceneGraphComponent();
		SceneGraphComponent cameraNode = new SceneGraphComponent();
		SceneGraphComponent geometryNode = new SceneGraphComponent();
		lightNode = new SceneGraphComponent();

		rootNode.addChild(geometryNode);
		rootNode.addChild(cameraNode);
		cameraNode.addChild(lightNode);

		Camera camera = new Camera();
		cameraNode.setCamera(camera);

		geometryNode.addChild(content);
		
		RotateTool rotateTool = new RotateTool();
		geometryNode.addTool(rotateTool);

		MatrixBuilder.euclidean().translate(0, 0, 3).assignTo(cameraNode);

		Appearance rootApp = new Appearance();
		rootApp.setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(0f,
				.1f, .1f));
		rootApp.setAttribute(CommonAttributes.DIFFUSE_COLOR, new Color(1f, 0f,
				0f));
		rootNode.setAppearance(rootApp);

		SceneGraphPath camPath = new SceneGraphPath();
		camPath.push(rootNode);
		camPath.push(cameraNode);
		camPath.push(camera);

		final Viewer viewer = new de.jreality.jogl.Viewer();
		viewer.setSceneRoot(rootNode);
		viewer.setCameraPath(camPath);
		ToolSystem toolSystem = ToolSystem.toolSystemForViewer(viewer);
		toolSystem.initializeSceneTools();
		
		setViewer(viewer);
		setViewerSize(new Dimension(640, 400));
		
		renderThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (renderingEnabled) {
						viewer.renderAsync();
					}
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		renderThread.start();
	}
	
	public static double degrees(final double d) {
		return d / 180.0 * Math.PI;
	}
	
	public void addLight(final Light light, final Transformation t) {
		final SceneGraphComponent node = new SceneGraphComponent();
		node.setLight(light);
		node.setTransformation(t);
		lightNode.addChild(node);
	}
	
	public void startRendering() {
		renderingEnabled = true;
	}
	
	public void pauseRendering() {
		renderingEnabled = false;
	}
	
	public Viewer getViewer() {
		return this.viewer;
	}

	public void setViewer(final Viewer viewer) {
		getContentPane().removeAll();
		getContentPane().add((Component) viewer.getViewingComponent());
		this.viewer = viewer;
	}
	
	public Dimension getViewerSize() {
		return this.viewerSize;
	}

	public void setViewerSize(final Dimension viewerSize) {
    	final Component c = (Component) viewer.getViewingComponent();
    	c.setPreferredSize(null); // force event triggering
    	c.setPreferredSize(viewerSize);
    	pack();
		this.viewerSize = c.getSize();
	}
}
