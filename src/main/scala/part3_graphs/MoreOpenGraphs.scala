package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import java.util.Date

object MoreOpenGraphs extends App {
  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
    Example: Max3 operator
      - 3 input of type int
      - the maximun of the 3
   */
  // step 1
  val max3StaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    // step 2 - define aux SHAPES
    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    // step 3
    max1.out ~> max2.in0

    // step 4
    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is: $x"))
  // step 1
  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - declare Shapes
      val max3Shape = builder.add(max3StaticGraph)

      // step 3 - tie
      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      max3Shape.out ~> maxSink

      // step 4
      ClosedShape

    }
  )

  // max3RunnableGraph.run()

  // same for UniformFanOutShape

  /*
    Non-uniform fan out shape

    Assume that we are processing bank transactions.
    Transaction (Txn) is suspicious if amount > 10.000

    Stream components for txns
    - output1: let the transaction go throught
    - output2: suspicious txn ids
   */
  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("5273890572", "Thiago", "Andres", 100, new Date),
      Transaction("3572890254", "Caro", "Andres", 100000, new Date),
    Transaction("5489036033", "Andres", "Lucia", 7000, new Date)
  ))

  // external components
  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"Suspicious transaction ID: $txnId"))

  // step 1
  val suspiciousTxnStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    // step 2 - define SHAPES
    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))

    // step 3 - tie Shapes
    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

    // step 4
    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  // step 1
  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2
      val suspiciousTxnShape = builder.add(suspiciousTxnStaticGraph)

      // step 3
      transactionSource ~> suspiciousTxnShape.in
      suspiciousTxnShape.out0 ~> bankProcessor
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      // step 4
      ClosedShape
    }
  )

  suspiciousTxnRunnableGraph.run()

}
