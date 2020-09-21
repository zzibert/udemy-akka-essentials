package part1Recap

object AdvancedScalaRecap extends App {

  // partial functions
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val pf = (x: Int) => x match {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val modifiedList = List(1, 2, 3).map {
    case 1 => 42
    case 2 => 65
    case _ => 999
  }

  // lifting
  val lifted = partialFunction.lift
  println(lifted(2))
  println(lifted(345))

  // orElse
  val pfChain = partialFunction.orElse[Int, Int] {
    case 60 => 9000
  }
  println(pfChain(5))
  println(pfChain(60))

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def receive: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("confused...")
  }

  // implicits
  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => println("timeout"))

  // implicit conversion
  case class Person(name: String) {
    def greet = println(s"hi my name is $name")
  }

  implicit def fromStringToPerson(string: String): Person = Person(string)

  "Peter".greet

  // implicit class
  implicit class Dog(name: String) {
    def bark = println("bark!")
  }

  "Lassie".bark // automatically done by compiler

  // organize
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  println(List(1, 2, 3).sorted)



}
