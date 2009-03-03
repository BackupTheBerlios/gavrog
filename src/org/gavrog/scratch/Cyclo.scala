package org.gavrog.scratch

import java.io._
import java.lang.ProcessBuilder

object Cyclo {
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
  
  def generate(seq: List[Int]) {
    val args = "mgraph_p" :: seq.map(_.toString) ::: List("o", "p")
    val proc = new ProcessBuilder(args.toArray: _*).start
    val src = new DataInputStream(proc.getInputStream)
    
    def getDummy = try {
      Some(src.readByte)
    } catch {
      case ex: EOFException => None
    }

    def read(n: Int): Int = {
      getDummy match {
        case None => n
        case Some(dummy) => {
	      def next: Int = {
	        if (dummy == 0) src.readUnsignedShort else src.readByte
	      }
	      val size: Int = if (dummy == 0) next else dummy
          println("PERIODIC_GRAPH")
	      var i = 1
	      while (i < size) {
	        val j = next
	        if (j == 0) i += 1
            else println("  %3d %3d" format (i, j))
	      }
          println("END")
          read(n + 1)
        }
      }
    }
    val count = read(0)
    proc.waitFor
    
    println("# Generated %d graphs for degree sequence %s." format (count, seq))
    total_count += count
  }
  
  def main(args : Array[String]) : Unit = {
    try {
      val k = args(0).toInt
      for (n <- 0 to 2 * (k - 1);
           s <- degree_sequences(n, n + k - 1);
           l <- 0 to k)
        generate(0 :: l :: s)

      println("# " + "=" * 72)
      println("# Generated altogeter %d graphs." format total_count)
    } catch {
      case ex: Throwable => println(ex)
    }
  }
}
