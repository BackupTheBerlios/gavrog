package org.gavrog.apps._3dt;

import de.jreality.scene.Camera;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;

/**
 * This class uses the mouse wheel to implement a simple camera zoom tool.
 * Scrolling up zooms out, scrolling down zooms in.
 * 
 * @author Charles Gunn
 *
 */
public class ClickWheelCameraZoomTool extends AbstractTool  {

	public ClickWheelCameraZoomTool()	{
		super(
				InputSlot.getDevice("PrimaryUp"),
				InputSlot.getDevice("PrimaryDown"));
	}
	int wheel = 0;
	double speed = 1.05;
	public void activate(ToolContext tc) {
		if (tc.getSource()== InputSlot.getDevice("PrimaryUp")) {
			wheel = 1;
		}
		else if (tc.getSource()== InputSlot.getDevice("PrimaryDown")) {
			wheel = -1;
		}
		Camera cam = (Camera) tc.getViewer().getCameraPath().getLastElement();
		double fov = cam.getFieldOfView();
		cam.setFieldOfView(fov * ((wheel > 0) ? speed : 1.0/speed));
		tc.getViewer().renderAsync();
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
}
