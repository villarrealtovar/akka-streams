package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1) // assuming hard computations
  val multiplier = Flow[Int].map(x => x * 10) // assuming hard computations

  /*
    I would like to execute both of these flows in parallel somehow and merge
    back the results in a tuple or a pair
   */
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator: it has only one single input
                                                    // and two outputs
      val zip = builder.add(Zip[Int, Int]) // fan-in operator: it has two inputs and one output

      // step 3 - tying up the components
      input ~> broadcast // it reads: input feeds `~>` into broadcast

      // first output
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape object
      ClosedShape // FREEZE the builder's shape
    } // this expression must be a Graph

  ) // runnable graph

  // graph.run() // run the graph and materialize it

  /*
    Exercise 1: Feed a single source into 2 sinks at the same time (hint: use a broadcast)
   */

  val firstSink = Sink.foreach[Int](x => println(s"First sink: $x"))
  val secondSink = Sink.foreach[Int](x => println(s"Second sink: $x"))

  //step 1
  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder => //builder type inferred by Scala compiler
      import GraphDSL.Implicits._

      // step 2 - declaring the components
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3 - tying up the components
      input ~> broadcast
      broadcast.out(0) ~> firstSink
      broadcast.out(1) ~> secondSink

      /***
       * a shorter version of the code would be:

      input ~> broadcast ~> firstSink
               broadcast ~> secondSink

       This is called  `Implicit port numbering`: it just allocates the right ports in order.

       */

      // step 4
      ClosedShape
    }
  )

  /*
    Exercise 2: balance
   */

  import scala.concurrent.duration._

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 1 number of elements: $count")
    count + 1
  })
  val sink2 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 2 number of elements: $count")
    count + 1
  })

  // step 1
  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 -- declare component
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      // step 3 - tie them up
      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge;   balance ~> sink2 // the merge here doesn't have ~> because it was done previously


      // step 4
      ClosedShape
    }
  )

  balanceGraph.run()

}
