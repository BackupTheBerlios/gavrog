/*
Copyright 2006 Olaf Delgado-Friedrichs

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

package org.gavrog.systre;

import java.awt.Insets;

import buoy.event.CommandEvent;
import buoy.event.WindowClosingEvent;
import buoy.widget.BButton;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BorderContainer;
import buoy.widget.LayoutInfo;

/**
 * A simple GUI for Gavrog Systre (in the making).
 * 
 * @author Olaf Delgado
 * @version $Id: GUI.java,v 1.1 2006/03/24 00:44:46 odf Exp $
 */
public class GUI extends BFrame {

    /**
     * Constructs an instance.
     */
    public GUI() {
        super("Systre");
        
        final BorderContainer bc = new BorderContainer();
        final LayoutInfo layoutInfo = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
                new Insets(5, 5, 5, 5), null);
        final BLabel label = new BLabel("Gavrog Systre - Version 1.0b0");
        bc.add(label, BorderContainer.NORTH, layoutInfo);
        final BButton okButton = new BButton("OK");
        bc.add(okButton, BorderContainer.SOUTH, layoutInfo);
        
        setContent(bc);
        
        okButton.addEventLink(CommandEvent.class, this, "doQuit");
        addEventLink(WindowClosingEvent.class, this, "doQuit");
        
        pack();
        setVisible(true);
    }
    
    protected void doQuit() {
        System.exit(0);
    }
    
    public static void main(final String args[]) {
        new GUI();
    }
}
