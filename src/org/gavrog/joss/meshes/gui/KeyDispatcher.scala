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

import java.awt.Point
import javax.swing.{JPopupMenu, KeyStroke}

import scala.swing.{Action, Alignment, GridPanel,
                    Frame, Label, Reactor, UIElement}
import scala.swing.event.{MouseEntered, WindowClosed}
import scala.util.Sorting

trait KeyDispatcher extends Reactor {
  class Binding(description: String, code: => Unit) {
    def execute { code }
    override def toString = description
  }
  
  private var bindings = Map[KeyStroke, Binding]()
  private var boundKeys = List[KeyStroke]()
  private var sources = List[KeyPublisher]()
  
  def addKeySource(src: KeyPublisher) {
    sources = src :: sources
    listenTo(src.keyClicks, src.Mouse.moves)
  }
  
  def focusOnEnter(src: KeyPublisher) {
    reactions += { case MouseEntered(`src`, _, _) => src.requestFocus }
  }
  
  def bind(ksText: String, description: String, code: => Unit) {
    KeyStroke.getKeyStroke(ksText) match {
      case null => throw new Error("Unknown key stroke '%s'" format ksText)
      case keyStroke => bind(keyStroke, description, code)
    }
  }
  
  def bind(keyStroke: KeyStroke, description: String, code: => Unit) {
    bindings += keyStroke -> new Binding(description, code)
    boundKeys = (boundKeys - keyStroke) ::: keyStroke :: Nil
  }
  
  def unbind(keyStroke: KeyStroke) {
    bindings  -= keyStroke
    boundKeys -= keyStroke
  }
  
  reactions += {
    case KeyPressed(src, modifiers, code, char) if (sources contains src) => {
      val ks = KeyStroke.getKeyStroke(code, modifiers)
      //print(ks + "\n")
      if (boundKeys contains ks) bindings(ks).execute
    }
  }
  
  def bindingDescriptions = boundKeys.map(k => List(k, bindings(k).toString))
  
  def showKeyBindings(parent: UIElement) {
    JPopupMenu.setDefaultLightWeightPopupEnabled(false)
    new JPopupMenu("Key Bindings") {
      setLightWeightPopupEnabled(false)
      for (ks <- boundKeys) add(new Action(bindings(ks).toString) {
        def apply = bindings(ks).execute
        accelerator = Some(ks)
      }.peer)
    }.show(parent.peer, 0, 0)
  }
}
