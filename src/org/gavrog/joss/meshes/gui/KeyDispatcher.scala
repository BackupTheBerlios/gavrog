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
import scala.util.Sorting

trait KeyDispatcher extends Reactor {
  class Binding(description: String, code: => Unit) {
    def execute { code }
    override def toString = description
  }
  
  private var bindings = Map[String, Binding]()
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
  }
  
  def unbind(keyStroke: String) {
    bindings -= keyStroke
  }
  
  reactions += {
    case KeyPressed(src, modifiers, code, char) if (sources contains src) => {
      val key =java.awt.event.KeyEvent.getKeyText(code)
      val mod = java.awt.event.KeyEvent.getKeyModifiersText(modifiers)
      val txt = if (char > 32 && char < 128)       "" + char
                else if (mod.size > 0 && mod != key) mod + "-" + key
                else                                 key
      //print(txt + "\n")
      bindings.get(txt) match {
        case None => ()
        case Some(binding) => binding.execute
      }
    }
  }
  
  def helpText = Sorting.stableSort(bindings.keys.toList)
                   .map { k => "%-20s%s" format (k, bindings(k)) }
                   .mkString("\n") + "\n"
}
