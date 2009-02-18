package org.gavrog.joss.meshes

import javax.swing.SwingUtilities

object SwingSupport {
  implicit def asRunnable(body: => Unit) = new Runnable() { def run { body } }

  def invokeAndWait(body: => Unit) : Unit =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeAndWait(body)

  def invokeLater(body: => Unit) : Unit =
    if (SwingUtilities.isEventDispatchThread) body.run
    else SwingUtilities.invokeLater(body)
}
