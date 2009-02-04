package org.gavrog.joss.meshes

object Scratch {
  def main(args : Array[String]) : Unit = {
    // -- unless (<cond>) <body>
    
    def unless(cond: => Boolean)(body: => Unit) = if (!cond) body
    
    // -- <body> when|unless (<cond>)
    
    class Conditional(body: => Unit) {
      def when(cond: => Boolean) = if (cond) body
      def unless(cond: => Boolean) = if (!cond) body
    }
    
    implicit def toConditional(u: => Unit) = new Conditional(u)
    
    // -- <cond> ? <true_val> :- <false_val>
    
    class IfThenElse(cond: => Boolean) {
      class Alternative(ifTrue: => Any) {
        def :-(ifFalse: => Any) = if (cond) ifTrue else ifFalse
      }
      def ?(ifTrue: => Any) = new Alternative(ifTrue)
    }
    
    implicit def toIfThenElse(cond: => Boolean) = new IfThenElse(cond)
    
    implicit def numToIfThenElse[T <% Double](x: => T) = new IfThenElse(x != 0)
    
    // -- mappings through anonymous functions
    
    val weekDay : Int => String = {
      case 1 => "Monday"
      case 2 => "Tuesday"
      case 3 => "Wednesday"
      case 4 => "Thursday"
      case 5 => "Friday"
      case 6 => "Saturday"
      case 7 => "Sunday"
      case _ => null
    }
    
    // -- the stack class from "Scala by Example"
    abstract class Stack[+A] {
      def push[B >: A](x: B): Stack[B] = new NonEmptyStack(x, this)
      def isEmpty: Boolean
      def top: A
      def pop: Stack[A]
    }
    object EmptyStack extends Stack[Nothing] {
      def isEmpty = true
      def top = error("EmptyStack.top")
      def pop = error("EmptyStack.pop")
    }
    class NonEmptyStack[+A](elem: A, rest: Stack[A]) extends Stack[A] {
      def isEmpty = false
      def top = elem
      def pop = rest
    }
    
    // -- recursive sorting with lists
    def isort[A](less: (A, A) => Boolean)(xs: List[A]) : List[A] = {
      def insert(x : A, xs : List[A]) : List[A] = xs match {
        case Nil    => List(x)
        case h :: t => if (less(x, h)) x :: xs else h :: insert(x, t)
      }
      xs match {
        case Nil    => Nil
        case h :: t => insert(h, isort(less)(t))
      }
    }
    
    def msort[A](less: (A, A) => Boolean)(xs: List[A]): List[A] = {
      def merge(xs: List[A], ys: List[A]) : List[A] = {
        if      (xs.isEmpty)             ys
        else if (ys.isEmpty)             xs
        else if (less(xs.head, ys.head)) xs.head :: merge(xs.tail, ys)
        else                             ys.head :: merge(xs, ys.tail)
      }
      val n = xs.length / 2
      if (n == 0) xs else merge(msort(less)(xs take n), msort(less)(xs drop n))
    }
    
    def qsort[A](less: (A, A) => Boolean)(xs: List[A]): List[A] = {
      if (xs.isEmpty) Nil else {
        val pivot = xs(xs.length / 2)
        List.concat(qsort(less)(xs.filter(less(_, pivot))),
                    xs.filter(_ == pivot),
                    qsort(less)(xs.filter(less(pivot, _))))
      }
    }
    
    // -- computing with streams
    def from(n: Int): Stream[Int] = Stream.cons(n, from(n + 1))

    def sieve(s: Stream[Int]): Stream[Int] =
      Stream.cons(s.head, sieve(s.tail filter { _ % s.head != 0 }))

    def primes = sieve(from(2))

    // -- implicit parameters
    abstract class SemiGroup[A] {
      def add(x: A, y: A) : A
    }
    
    abstract class Monoid[A] extends SemiGroup[A] {
      def unit : A
    }
    
    implicit object IntMonoid extends Monoid[Int] {
      def add(x: Int, y: Int) = x + y
      def unit = 0
    }
    
    implicit object StringMonoid extends Monoid[String] {
      def add(x: String, y : String) = x + y
      def unit = ""
    }
    
    def sum[A](xs : Seq[A])(implicit m : Monoid[A]) : A = {
      var s : A = m.unit
      for (x <- xs) s = m.add(s, x)
      s
    }
    
    // -- some test code
    
    println("--- unless (i % 2 == 0) { println(i) } ---")
    for (i <- 0 until 10)
      unless (i % 2 == 0) { println(i) }
    println
    
    println("--- { println(i) } when (i % 2 == 0) ---")
    for (i <- 0 until 10)
      { println(i) } when (i % 2 == 0)
    println
    
    println("--- { println(i) } unless (i % 2 == 0) ---")
    for (i <- 0 until 10)
      { println(i) } unless (i % 2 == 0)
    println
    
    println("--- println((i % 2 == 0) ? (i / 2) :- (i * 3 + 1)) ---")
    for (i <- 0 until 10)
      println((i % 2 == 0) ? (i / 2) :- (i * 3 + 1))
    println
    
    println("--- println((i % 2) ? (i * 3 + 1) :- (i / 2)) ---")
    for (i <- 0 until 10)
      println((i % 2) ? (i * 3 + 1) :- (i / 2))
    println
    
    println(weekDay(3) + " is the third day of the week.")
    println
    
    println("--- functional stack ---")
    var stack = EmptyStack.push(1).push(2).push(3).push(4)
    while (!stack.isEmpty) {
      println(stack.top)
      stack = stack.pop
    }
    println
    
    val list = List(1,9,2,7,3,8,6,4,5)
    val less = (x : Int, y : Int) => x < y
    
    println("--- recursive insert sort on lists ---")
    printf("isort(%s) = %s", list, isort(less)(list))
    println
    
    println("--- recursive merge sort on lists ---")
    printf("msort(%s) = %s", list, msort(less)(list))
    println
    
    println("--- recursive quicksort on lists ---")
    printf("qsort(%s) = %s", list, qsort(less)(list))
    println
    
    println("--- first 20 prime number via lazy sieve ---")
    for (p <- primes take 20)
      print("  " + p)
    println
    println("The 1000th prime number is " + primes(999))
    println
    
    println("--- generic sum function using implicit parameters ---")
    println("sum(1 until 10) = " + sum(1 until 10))
    println("sum(1 until 10 map (x => \" \" + x)) = \"" +
              sum(1 until 10 map (x => " " + x)) + "\"")
  }
}
