package part2_primer

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlo2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // This runs on the SAME ACTOR
  // This is called operator/component FUSION
  // simpleSource.via(simpleFlow).via(simpleFlo2).to(simpleSink).run()

  // the previous akka stream is equivalent to create an instance of
  // this SimpleActor and execute all flows inside of the receive method.
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case x: Int =>
        // flow operations
        val x2 = x + 1
        val y = x2 * 10
        //sink operation
        println(y)
    }
  }

  // NOTE: By default, Akka Streams components are fused = run on the same actor

  val simpleActor = system.actorOf(Props[SimpleActor])
  // every single element that goes from the source is sent to SimpleActor.
  // (1 to 1000).foreach(simpleActor ! _)

  /*
    Now, the end result with both approaches is that a single CPU Core will be used
    for the complete processing of every single element throughout the entire flow
    much like a CPU Core will be used for complete processing of every single element
    in the message handler for the SimpleActor
   */

  // let's introduce some more complex operators.
  val complexFlow = Flow[Int].map { x =>
  // simulating a long computation
    Thread.sleep(1000)
    x+ 1
  }

  val complexFlow2 = Flow[Int].map { x =>
    // simulating a long computation
    Thread.sleep(1000)
    x * 10
  }

  // simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run();

  // So, when operators are expensive it's worth making them run separately
  // in parallel on different actors. For that, we need to introduce the
  // concept of an async boundary
/*  simpleSource.via(complexFlow).async // runs on one actor
    .via(complexFlow2).async // runs on another actor
    .to(simpleSink) // runs on a third actor
    .run()*/

  // ordering guarantees: with or without async boundaries

  // without async boundaries
 /* Source(1 to 3)
    .map(element => {
      println(s"Flow A: $element")
      element
    })
    .map(element => {
      println(s"Flow B: $element")
      element
    })
    .map(element => {
      println(s"Flow C: $element")
      element
    }).runWith(Sink.ignore)*/

  // with async boundaries
  Source(1 to 3)
    .map(element => {
      println(s"Flow A: $element")
      element
    }).async
    .map(element => {
      println(s"Flow B: $element")
      element
    }).async
    .map(element => {
      println(s"Flow C: $element")
      element
    }).async
    .runWith(Sink.ignore)

}
