package org.gavrog.scratch

object Drool {
  abstract class Droolean {
    def not: Droolean
    def and(other: Droolean) : Droolean
    def or(other: Droolean) : Droolean
  }

  case object yes extends Droolean {
    def not = no
    def and(other: Droolean) = other
    def or (other: Droolean) = yes
  }
  
  case object no extends Droolean {
    def not = yes
    def and(other: Droolean) = no
    def or (other: Droolean) = other
  }
  
  case object maybe extends Droolean {
    def not = maybe
    def and(other: Droolean) = if (other == no ) no  else maybe
    def or (other: Droolean) = if (other == yes) yes else maybe
  }
  
  case object not {
    def apply(x: Droolean) = x not
    def yes = Drool.no
    def no = Drool.yes
    def maybe = Drool.maybe
  }

  implicit def as_Droolean(b: Boolean) : Droolean = b match {
    case true => yes
    case false => no
  }
  
  def main(args: Array[String]) : Unit = {
    println(maybe or (maybe not))
  }
}
