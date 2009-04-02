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

import scala.swing.Reactor
import scala.swing.event.MouseEntered

trait KeyDispatcher extends Reactor {
  class Binding(description: String, code: => Unit) {
    def execute { code }
  }
  
  private var bindings = Map[String, List[Binding]]()
  private var sources = List[KeyPublisher]()
  
  def addKeySource(src: KeyPublisher) {
    sources = src :: sources
	listenTo(src.keyClicks, src.Mouse.moves)
  }
  
  def focusOnEnter(src: KeyPublisher) {
    reactions += { case MouseEntered(`src`, _, _) => src.requestFocus }
  }
  
  def addBinding(keyStroke: String, description: String, code: => Unit) {
    val old = bindings.getOrElse(keyStroke, Nil)
    bindings += keyStroke -> (new Binding(description, code) :: old)
  }
  
  def replaceBinding(keyStroke: String, description: String, code: => Unit) {
    bindings += keyStroke -> (new Binding(description, code) :: Nil)
  }
  
  def clearBindings(keyStroke: String) {
    bindings -= keyStroke
  }
  
  reactions += {
    case KeyPressed(src, modifiers, code, _) if (sources contains src) => {
      val key = java.awt.event.KeyEvent.getKeyText(code)
      val mod = java.awt.event.KeyEvent.getKeyModifiersText(modifiers)
      val txt = mod + (if (mod.size > 0) "-" else "") + key
      
      for (binding <- bindings.getOrElse(txt, Nil)) binding.execute
    }
  }
}
