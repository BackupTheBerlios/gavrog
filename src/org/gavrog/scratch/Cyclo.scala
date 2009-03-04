package org.gavrog.scratch

import java.io.{DataInputStream, EOFException, InputStream}
import java.lang.ProcessBuilder

import scala.collection.mutable.HashMap

object Cyclo {
  class Pluralizer(n: Int) {
    def apply(s: String) = "%d %s%s" format (n, s, if (n == 1) "" else "s")
  }
  implicit def i2pl(n: Int) = new Pluralizer(n)

  type Graph = Map[Int, List[Int]]
  
  def degree_sequences(n: Int, m: Int) = {
    def seq(n: Int, m: Int, min_d: Int) : Seq[List[Int]] =
      if (n * min_d > m)
        Nil
      else if (n == 0)
        if (m == 0) List(Nil) else Nil
      else for (i <- 0 to m / min_d if i <= n;
                s <- seq(n - i, m - i * min_d, min_d + 1))
        yield i :: s
    
    seq(n, 2 * m, 3)
  }
  
  def read_graphs(is: InputStream) : Stream[Graph] = {
    val src = new DataInputStream(is)
    
    val first_byte = try {
      Some(src.readByte)
    } catch {
      case ex: EOFException => None
    }
    
    first_byte match {
      case None => Stream.empty
      case Some(x) => {
        def next: Int = if (x == 0) src.readUnsignedShort else src.readByte
        val size: Int = if (x == 0) next else x
        val adj = new HashMap[Int, List[Int]]
        var i = 1
        while (i < size) {
          val j = next
          if (j == 0) i += 1
          else {
            adj(i) = j :: adj.getOrElse(i, Nil)
            adj(j) = i :: adj.getOrElse(j, Nil)
          }
        }
        Stream.cons(Map(adj.toSeq: _*), read_graphs(is))
      }
    }
  }
  
  def simplified(gr: Graph) = {
    var old2new   = Map[Int, Int]().withDefaultValue(0)
    var new_edges = Nil: List[(Int, Int)]
    
    for ((i, neighbors) <- gr)
      if (neighbors.size == 2)
        new_edges = (neighbors(0), neighbors(1)) :: new_edges
      else old2new += (i -> (old2new.size + 1))
    
    val output = new HashMap[Int, List[Int]]
    
    for ((i, neighbors) <- gr if old2new(i) != 0)
      output(old2new(i)) = neighbors map old2new filter (0 !=)
    
    for ((i, j) <- new_edges) {
      val a = old2new(i)
      val b = old2new(j)
      output(a) = b :: output(a)
      if (a != b) output(b) = a :: output(b)
    }
    
    Map(output.toSeq: _*)
  }
  
  def edges(gr: Graph) =
    for ((i, neighbors) <- gr; j <- neighbors if i <= j) yield (i, j)
  
  def bridges(gr: Graph) = {
    var time   = 0
    var dfsnum = Map[Int, Int]().withDefaultValue(0)
    var low    = Map[Int, Int]().withDefaultValue(0)
    var is_art = Set[Int]()
    var result = Nil: List[(Int, Int)]
    
    def art(v: Int, u: Int) {
      time += 1
      low += (v -> time)
      dfsnum += (v -> time)
      
      for (w <- gr(v) if w != u)
        if (dfsnum(w) == 0) {
          art(w, v)
          if (low(w) < low(v)) low += (v -> low(w))
          if ((dfsnum(v) == 1 &&
                 (dfsnum(w) != 2 || !gr(v).forall(dfsnum(_) > 0)))
              || (dfsnum(v) != 1 && low(w) >= dfsnum(v))
          ) {
            is_art += v
            if (is_art(w) && gr(v).filter(w ==).size == 1)
              result = (v, w) :: result
          }
        } else
          if (dfsnum(w) < low(v)) low += (v -> dfsnum(w))
    }
    
    art(gr.keys.next, 0)
    result
  }
  
  def generate(seq: List[Int]) : Seq[Graph] = {
    val args   = "mgraph_p" :: seq.map(_.toString) ::: List("o", "p")
    val proc   = new ProcessBuilder(args.toArray: _*).start
    val result = read_graphs(proc.getInputStream)
    proc.waitFor
    result
  }
  
  def main(args : Array[String]) : Unit = {
    try {
      val k = args(0).toInt
      var n_total      = 0
      var n_bridgeless = 0

      for (n <- 0 to 2 * (k - 1);
           s <- degree_sequences(n, n + k - 1);
           l <- 0 to k)
      {
        val graphs = generate(0 :: l :: s)
        val n = graphs.size
        println("# Found %s with degree sequence (%s) and %s%s"
                format (n("graph"), s map (_.toString) reduceLeft(_ + ", " + _),
                        l("loop"), if (n > 0) ":" else "."))
        for (gr <- graphs) {
          val b = bridges(gr).size
          if (b == 0) n_bridgeless += 1
          println("%s # (%s)" format
                    (edges(simplified(gr)).map(_.toString).reduceLeft(_ + _),
                     b("bridge")))
        }
        n_total += n
      }

      println
      println("# " + "=" * 72)
      println("# A total of %d graphs with cyclomatic number %d were generated."
              format (n_total, k))
      println("# %d of those were bridgeless." format n_bridgeless)
    } catch {
      case ex: Throwable => println(ex)
    }
  }
}
