package org.gavrog.scratch

import java.io.{DataInputStream, EOFException, InputStream}
import java.lang.ProcessBuilder

import scala.collection.mutable.HashMap

object Cyclo {
  type Graph = Map[Int, List[Int]]
  
  var total_count = 0

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
    var old2new  : Map[Int, Int]    = Map().withDefaultValue(0)
    var new_edges: List[(Int, Int)] = Nil
    
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
      for (n <- 0 to 2 * (k - 1);
           s <- degree_sequences(n, n + k - 1);
           l <- 0 to k)
      {
        var count = 0
        val sequence = 0 :: l :: s
        for (g  <- generate(sequence) map simplified) {
          println(edges(g) map (_.toString) reduceLeft(_ + _))
          count += 1
        }
        println("# Generated %d graphs for degree sequence %s."
                format (count, sequence))
        total_count += count
      }

      println("# " + "=" * 72)
      println("# Generated a total of %d graphs." format total_count)
    } catch {
      case ex: Throwable => println(ex)
    }
  }
}
