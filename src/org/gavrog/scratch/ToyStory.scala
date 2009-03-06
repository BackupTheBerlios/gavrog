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
  implicit def wrapped_path(p: Path) = new {
    def cost = p.map(_.cost).reduceLeft(_ + _)
    def text = p.dropRight(1).mkString("", ", ", "") + ", and " + p.last
  }

  def pairs[A](s: List[A]) =
    for ((first, index) <- s.elements.zipWithIndex;
         second <- s.elements.drop(index + 1))
      yield List(first, second)
  
  def solve_right(on_left: List[Toy],
                  on_right: List[Toy], limit: Int) : Iterator[Path] =
    for { pair <- pairs(on_left)
          move = Move(Right, pair)
          remaining = limit - move.cost if remaining >= 0
          path <- solve_left(on_left -- pair, pair ::: on_right, remaining) }
      yield move :: path

  def solve_left(on_left: List[Toy],
                 on_right: List[Toy], limit: Int) : Iterator[Path] =
    if (on_left == Nil) Iterator.single(Nil)
    else for { toy <- on_right.elements
               move = Move(Left, List(toy))
               remaining = limit - move.cost if remaining >= 0
               path <- solve_right(toy :: on_left, on_right - toy, remaining) }
      yield move :: path
    
  def solutions(toys: List[Toy], limit: Int) = solve_right(toys, Nil, limit)

  val toys =
    List(Toy("Buzz", 5), Toy("Woody", 10), Toy("Rex", 20), Toy("Hamm", 25))

  def main(args : Array[String]) : Unit =
    for (path <- solutions(toys, 60))
      println("In %d minutes, %s." format (path.cost, path.text))
}
