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

import java.awt.Point

import javax.swing.{KeyStroke, SwingUtilities}
import scala.swing.{Action, Component, MenuItem}
import scala.swing.event.InputEvent

class KeyEvent(val source: Component,
               val modifiers: Int,
               keyCode: Int, keyChar: Char)(val when: Long) extends InputEvent
case class KeyPressed(override val source: Component,
                      override val modifiers: Int, 
                      keyCode: Int, keyChar: Char)(when: Long) extends
  KeyEvent(source, modifiers, keyCode, keyChar)(when)
case class KeyReleased(override val source: Component,
                       override val modifiers: Int, 
                       keyCode: Int, keyChar: Char)(when: Long) extends
  KeyEvent(source, modifiers, keyCode, keyChar)(when)
case class KeyTyped(override val source: Component,
                    override val modifiers: Int, 
                    keyCode: Int, keyChar: Char)(when: Long) extends
  KeyEvent(source, modifiers, keyCode, keyChar)(when)

object SwingSupport {
  implicit def asRunnable(body: => Unit) = new Runnable() { def run { body } }

  def invokeAndWait(body: => Unit) : Unit =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeAndWait(body)

  def invokeLater(body: => Unit) : Unit =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeLater(body)

  def run(body: => Unit) = new Thread(body).start
  
  class ActionMenuItem(name: String, body: => Unit) extends MenuItem(name) {
    action = new Action(name) {
      def apply { body }
    }
    def accelerator = action.accelerator
    def accelerator_=(stroke: KeyStroke) {
      action.accelerator = Some(stroke)
    }
    def accelerator_=(spec: String) {
      accelerator = KeyStroke.getKeyStroke(spec)
    }
    def accelerator_=(keyCode: Int) {
      accelerator = KeyStroke.getKeyStroke(keyCode, 0)
    }
    def accelerator_=(spec: Pair[Int, Int]) {
      accelerator = KeyStroke.getKeyStroke(spec._1, spec._2)
    }
  }
}
