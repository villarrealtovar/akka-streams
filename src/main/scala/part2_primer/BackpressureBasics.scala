package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._

object BackpressureBasics extends App {
  implicit val system = ActorSystem("BackPressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
      // simulate a long processing
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  // fastSource.to(slowSink).run() // this fusing, not backpressure

  // fastSource.async.to(slowSink).run() // backpressure in place because two components (fastSource, slowSink)
                                      // operate on different actors.

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }

/*  fastSource.async
    .via(simpleFlow).async
    .to(slowSink).run()*/

  /*
    A component will react to back pressure in the following way (in order):
    - try to slow down if possible
    - buffer elements until there's more demand
    - drop down element from the buffer if it overflows <= we as programmers can only control what happens to a
                                                          component at this point if the component has buffered
                                                          enough elements and it's about to overflow.
    - tear down/kill the whole stream (failure)

   */
  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)

  /*fastSource.async
    .via(bufferedFlow).async
    .to(slowSink).run()*/


  /*
    number from
      - 1-16: nobody is brackpressured
      - 17-26: flow will buffer, flow will stat dropping at the next element
      - 26-1000: flow will always drop the oldest element
        => 991 - 1000 which convert to 992 - 1001 and sends to sink

  */

  /*
    overflow strategies:
    - drop head = oldest
    - drop tail = newest
    - drop new = exact element to be added = keeps the buffer
    - drop the entire buffer
    - backpressure signal
    - fail
   */

  // throttling
  import scala.concurrent.duration._
  fastSource.throttle(10, 1 second).runWith((Sink.foreach(println)))



}
