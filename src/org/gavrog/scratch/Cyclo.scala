package org.gavrog.scratch

import java.io.{BufferedInputStream, DataInputStream, EOFException, InputStream}
import java.lang.ProcessBuilder

import scala.util.Sorting._

trait SimpleIterator[A] extends Iterator[A] {
  var cache : Option[A] = None
  
  def advance : Option[A]
  
  private def next_with_cacheing = {
    if (cache == None) cache = advance
    cache
  }
  
  def hasNext = (next_with_cacheing != None)
      
  def next = next_with_cacheing match {
    case None => throw new NoSuchElementException("at end")
    case Some(x) => {
      cache = None
      x
    }
  }
}

object Cyclo {
  type Graph = Map[Int, List[Int]]
  type Edge = (Int, Int)
  
  def degree_sequences(n: Int, m: Int) = {
    def seq(n: Int, m: Int, min_d: Int) : Iterator[List[Int]] =
      if (n * min_d > m)
        Iterator.empty
      else if (n == 0)
        if (m == 0) Iterator.single(Nil) else Iterator.empty
      else for { i <- Iterator.range(0, m / min_d + 1) if i <= n
                 s <- seq(n - i, m - i * min_d, min_d + 1) }
        yield i :: s
    
    seq(n, 2 * m, 3)
  }
  
  def read_graphs(is: InputStream) = {
    val src = new DataInputStream(new BufferedInputStream(is))
    
    new SimpleIterator[Graph] {
      def advance = {
        val first_byte = try {
          Some(src.readByte)
        } catch {
          case ex: EOFException => {
            is.close
            None
          }
        }
    
        first_byte match {
          case None => None
          case Some(x) => {
            def next: Int = if (x == 0) src.readUnsignedShort else src.readByte
            val size: Int = if (x == 0) next else x
            var adj = Map[Int, List[Int]]()
            var i = 1
            while (i < size) {
              val j = next
              if (j == 0) i += 1
              else {
                adj += (i -> (j :: adj.getOrElse(i, Nil)))
                adj += (j -> (i :: adj.getOrElse(j, Nil)))
              }
            }
            Some(adj)
          }
        }
      }
    }
  }
  
  def simplified(gr: Graph) = {
    var old2new   = Map[Int, Int]().withDefaultValue(0)
    var new_edges = Nil: List[Edge]
    
    for ((i, neighbors) <- gr)
      if (neighbors.size == 2)
        new_edges = (neighbors(0), neighbors(1)) :: new_edges
      else old2new += (i -> (old2new.size + 1))
    
    var output = Map[Int, List[Int]]()
    
    for ((i, neighbors) <- gr if old2new(i) != 0)
      output += (old2new(i) -> (neighbors map old2new filter (0 !=)))
    
    for ((i, j) <- new_edges) {
      val a = old2new(i)
      val b = old2new(j)
      output += (a -> (b :: output(a)))
      if (a != b) output += (b -> (a :: output(b)))
    }
    
    output
  }
  
  def edges(gr: Graph) = for ((i, nb) <- gr; j <- nb if i <= j) yield (i, j)
  
  def sorted_edges(gr: Graph) = stableSort(edges(gr).toSeq)
  
  def bridges(gr: Graph) = {
    var time   = 0
    var dfsnum = Map[Int, Int]().withDefaultValue(0)
    var low    = Map[Int, Int]().withDefaultValue(0)
    var result = Nil: List[Edge]
    var stack  = Nil: List[Edge]
    
    def art(v: Int, u: Int) {
      time += 1
      low += (v -> time)
      dfsnum += (v -> time)
      
      for (w <- gr(v) if w != u) {
        if (dfsnum(w) < dfsnum(v)) stack = (v, w) :: stack
        if (dfsnum(w) == 0) {
          art(w, v)
          low += (v -> low(v).min(low(w)))
          if (low(w) >= dfsnum(v)) {
            if (stack.head == (v, w) && gr(v).filter(w==).size == 1)
              result = (v, w) :: result
            while (stack.head != (v, w)) stack = stack.tail
            stack = stack.tail
          }
        } else
          low += (v -> low(v).min(dfsnum(w)))
      }
    }
    
    art(gr.keys.next, 0)
    result
  }
  
  def generate(seq: List[Int]) : Iterator[Graph] = {
    val args = "mgraph_p" :: seq.map(_.toString) ::: List("o", "p")
    read_graphs(new ProcessBuilder(args.toArray: _*).start.getInputStream)
  }
  
  def main(args : Array[String]) : Unit = {
    implicit def as_pluralizer(n: Int) = new {
      def of(s: String) = "%d %s%s" format (n, s, if (n == 1) "" else "s")
    }

    try {
      var i = 0
      var loopless = false
      if (args(0) == "-l") {
        i += 1
        loopless = true
      }
      
      val k = args(i).toInt
      var n_total      = 0
      var n_bridgeless = 0

      for { n <- 0 to 2 * (k - 1)
            s <- degree_sequences(n, n + k - 1)
            l <- 0 to (if (loopless) 0 else k) }
      {
        val kind = "degree sequence %s and %s".format(s.mkString("(", ",", ")"),
                                                      l of "loop")
        println("# Graphs with %s:" format kind)

        var count = 0
        for (gr <- generate(0 :: l :: s)) {
          val b = bridges(gr).size
          val e = sorted_edges(simplified(gr))
          println("%s # (%s)" format (e.mkString, b of "bridge"))
          count += 1
          n_total += 1
          if (b == 0) n_bridgeless += 1
        }
        println("#   => %s with %s." format (count of "graph", kind))
      }

      println
      println("# " + "=" * 72)
      println("# A total of %s with cyclomatic number %d were generated."
              format (n_total of (if (loopless) "loopless " else "") + "graph",
                      k))
      println("# %d of those were bridgeless." format n_bridgeless)
    } catch {
      case ex: Throwable => println(ex)
    }
  }
}
