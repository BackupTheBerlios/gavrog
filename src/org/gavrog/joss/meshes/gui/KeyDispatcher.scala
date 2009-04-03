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

import scala.swing.{Alignment, GridPanel, Frame, Label, Reactor, UIElement}
import scala.swing.event.{MouseEntered, WindowClosed}
import scala.util.Sorting

trait KeyDispatcher extends Reactor {
  class Binding(description: String, code: => Unit) {
    def execute { code }
    override def toString = description
  }
  
  private var bindings = Map[String, Binding]()
  private var boundKeys = List[String]()
  private var sources = List[KeyPublisher]()
  
  def addKeySource(src: KeyPublisher) {
    sources = src :: sources
	listenTo(src.keyClicks, src.Mouse.moves)
  }
  
  def focusOnEnter(src: KeyPublisher) {
    reactions += { case MouseEntered(`src`, _, _) => src.requestFocus }
  }
  
  def bind(keyStroke: String, description: String, code: => Unit) {
    bindings += keyStroke -> new Binding(description, code)
    boundKeys = (boundKeys - keyStroke) ::: keyStroke :: Nil
  }
  
  def unbind(keyStroke: String) {
    bindings  -= keyStroke
    boundKeys -= keyStroke
  }
  
  reactions += {
    case KeyPressed(src, modifiers, code, char) if (sources contains src) => {
      val key =java.awt.event.KeyEvent.getKeyText(code)
      val mod = java.awt.event.KeyEvent.getKeyModifiersText(modifiers)
      val txt = if (char > 32 && char < 128)       "" + char
                else if (mod.size > 0 && mod != key) mod + "-" + key
                else                                 key
      //print(txt + "\n")
      if (boundKeys contains txt) bindings(txt).execute
    }
  }
  
  def bindingDescriptions = boundKeys.map(k => List(k, bindings(k).toString))
  
  def showKeyBindings(parent: UIElement) {
    new Frame {
      title = "Key Bindings"
      setLocationRelativeTo(parent)
      contents = new GridPanel(boundKeys.size, 2) {
        for (k <- boundKeys; entry <- List(k, bindings(k).toString))
          contents += new Label(entry, null, Alignment.Left)
      }
      pack
      listenTo(this)
      reactions += { case WindowClosed(w) if w == this => this.dispose }
      visible = true
    }
  }
}
