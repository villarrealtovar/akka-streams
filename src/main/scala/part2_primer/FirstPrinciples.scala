package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {

  /**
   * 1. Basics
   */
  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer() // materalizer in principle, it allows the running
                                        // of Akka streams components.

  // sources
  val source = Source(1 to 10)

  // sinks
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
  // graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

  // The following expressions do all the same thing

//    sourceWithFlow.to(sink).run()
//    source.to(flowWithSink).run()
//     source.via(flow).to(sink).run()

  /*
    Note: sources can emit any kind of objects as long as they're immutable and serializable
    much like actor messages because Source, Flow and Sink are based on actors, but `nulls`
    are NOT allowed per the reactive streams specification.
   */
  // val illegalSource = Source.single[String](null)
  // illegalSource.to(Sink.foreach(println)).run() // NullPointerException: Element must not be null, rule 2.13

  // use Options instead

  /**
   * 2. Various kinds of sources
   */
  // finites sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))

  // empty sources that never emits any elements
  val emptySource = Source.empty[Int]

  // infinite sources
  val infiniteSource = Source(Stream.from(1)) // Stream.from(1) is a infinite colletion of integers
                                              // DO NOT confuse an Akka stream with a "collection" Steam
  // creating source from futures
  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  /**
   * 3. Sinks
   */
  // consume everything and does nothing
  val theMostBoringSink = Sink.ignore

  // sinks that does something with values that it receives
  val foreachSink = Sink.foreach[String](println)

  // sinks that retrieve and may return a value
  val headSink = Sink.head[Int] // retrives head and then closes the stream

  // sinks that it can actually compute values out the element that they receive
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)

  /**
   * 4. Flows - usually mapped to collection operators
   */
  val mapFlow = Flow[Int].map(x => 2 * x)
  val takeFlow = Flow[Int].take(5)
  // Akka streams have other flows like drop, filter, and so on
  // Akka streams DON'T have flatMap


  /**
    A formal way to construct a stream is:

    source -> flow -> flow -> ... -> sink
   */
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  // doubleFlowGraph.run()

  // Akka stream is also very rich in syntactic sugar
  val mapSource = Source(1 to 10).map(x => x * 2) // it's equivalent to say Source(1 to 10).vía(Flow[Int].map(x => x * 2))

  // akka streams also has a very nice API to run streams directly.
  // mapSource.runForeach(println) // it's equivalent to mapSource.to(Sink.foreach[Int](println)).run()

  // OPERATORS = components

  /**
   * Exercise: create a stream that takes the names of persons, then
   * you will keep the first 2 names with length > 5 characters.
   */
  val names = List("Jose", "Carolina", "Lina", "Thiago", "Andres", "Lucia", "Monica")
  val nameSource = Source(names)
  val longNameFlow = Flow[String].filter(name => name.length > 5)
  val limitFlow = Flow[String].take(2)
  val nameSink = Sink.foreach[String](println)

  nameSource.via(longNameFlow).via(limitFlow).to(nameSink).run()
  // it's equivalent to the following expression
  nameSource.filter(_.length > 5).take(2).runForeach(println)

}
