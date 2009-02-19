package org.gavrog.joss.meshes

import javax.swing.{KeyStroke, SwingUtilities}
import scala.swing.{Action, MenuItem}

object SwingSupport {
  implicit def asRunnable(body: => Unit) = new Runnable() { def run { body } }

  def invokeAndWait(body: => Unit) : Unit =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeAndWait(body)

  def invokeLater(body: => Unit) : Unit =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeLater(body)

  class ActionMenuItem(name: String, body: => Unit) extends MenuItem(name) {
    action = new Action(name) {
      def apply { body }
    }
    def accelerator = action.accelerator
    def accelerator_=(spec: String) {
      action.accelerator = Some(KeyStroke.getKeyStroke(spec))
    }
  }
}
