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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import de.jreality.geometry.Primitives;
import de.jreality.jogl.Viewer;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.shader.CommonAttributes;
import de.jreality.tools.RotateTool;
import de.jreality.toolsystem.ToolSystem;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class ViewerFrame extends JFrame {
	public ViewerFrame(final SceneGraphComponent content) {
		SceneGraphComponent rootNode = new SceneGraphComponent();
		SceneGraphComponent cameraNode = new SceneGraphComponent();
		SceneGraphComponent geometryNode = new SceneGraphComponent();
		SceneGraphComponent lightNode = new SceneGraphComponent();

		rootNode.addChild(geometryNode);
		rootNode.addChild(cameraNode);
		cameraNode.addChild(lightNode);

		Light dl = new DirectionalLight();
		lightNode.setLight(dl);

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

		Viewer viewer = new Viewer();
		viewer.setSceneRoot(rootNode);
		viewer.setCameraPath(camPath);
		ToolSystem toolSystem = ToolSystem.toolSystemForViewer(viewer);
		toolSystem.initializeSceneTools();
		
		setJMenuBar(new JMenuBar());
		getJMenuBar().add(new JMenu("File"));

		setVisible(true);
		setSize(640, 480);
		getContentPane().add((Component) viewer.getViewingComponent());
		validate();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				System.exit(0);
			}
		});

		while (true) {
			viewer.renderAsync();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String args[]) {
		final SceneGraphComponent content = new SceneGraphComponent();
		final IndexedFaceSet ifs = Primitives.icosahedron();
		content.setGeometry(ifs);
		new ViewerFrame(content);
	}
}
