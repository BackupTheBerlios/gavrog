package org.gavrog.joss.meshes

import scala.collection.mutable.HashMap

class LazyMap[A, B](f: A => B) extends scala.collection.Map[A, B] with Proxy {
  private val cache = new HashMap[A, B]
    
  def self      = cache
  def size      = cache.size
  def elements  = cache.elements
  def get(x: A) = Some(cache.getOrElseUpdate(x, f(x)))
}
  
