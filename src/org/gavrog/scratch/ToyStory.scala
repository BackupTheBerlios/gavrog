// Based on:
// http://jonhnny-weslley.blogspot.com/2008/08/escape-from-zurg.html

package org.gavrog.scratch

object ToyStory {
  case class Toy(name: String, time: Int) {
    override def toString = name
  }

  abstract class Direction
  case object Left extends Direction 
  case object Right extends Direction

  case class Move(direction: Direction, toys: List[Toy]) {
    def cost = Iterable.max(toys.map(_.time))

    override def toString = 
      toys.mkString("", " and ", "") +
      (if (toys.size == 1) " moves " else " move ") +
      direction.toString.toLowerCase
  }

  type Path = List[Move]

  def pairs[A](s: List[A]) =
    for ((first, index) <- s.toStream.zipWithIndex; second <- s.drop(index + 1))
      yield List(first, second)
  
  def solutions(toys: List[Toy], limit: Int) = {
    def solve_right(on_left: List[Toy],
                    on_right: List[Toy], limit: Int) : Stream[Path] =
      for (pair <- pairs(on_left);
           move = Move(Right, pair);
           remaining = limit - move.cost if remaining >= 0;
           path <- solve_left(on_left -- pair, pair ::: on_right, remaining))
        yield move :: path

    def solve_left(on_left: List[Toy],
                   on_right: List[Toy], limit: Int) : Stream[Path] =
      on_left match {
        case Nil => Stream(Nil)
        case _ =>
          for (toy  <- on_right.toStream;
               move = Move(Left, List(toy));
               remaining = limit - move.cost if remaining >= 0;
               path <- solve_right(toy :: on_left, on_right - toy, remaining))
            yield move :: path
      }
    
    solve_right(toys, Nil, limit)
  }

  val toys =
    List(Toy("Buzz", 5), Toy("Woody", 10), Toy("Rex", 20), Toy("Hamm", 25))

  def main(args : Array[String]) : Unit =
    for (path <- solutions(toys, 60))
      println("Solution: " + path.mkString("", ", ", "."))
}
