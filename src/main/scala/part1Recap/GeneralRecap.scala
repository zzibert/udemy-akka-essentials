package part1Recap

object GeneralRecap extends App {

  // for comprehension
  val pairs = for {
    num <- List(1, 2, 3, 4)
    char <- List('a', 'b', 'c', 'd')
  } yield num + "-" + char

  println(pairs)

  case class Person(x: Int, n: String)

  val bob = Person(22, "Bob")

  val greeting = bob match {
    case Person(_, name) => s"Hi, my name is $name"
  }

  println(greeting)
}
