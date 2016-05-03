package in.roadrunnr

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import GraphDSL.Implicits._

import scala.concurrent.{ Future, ExecutionContextExecutor}

object Runnr2 extends App {

  implicit val system: ActorSystem = ActorSystem("Runnr1")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  //GRAPH.CREATE() TAKES A FUNCTION FROM builder: GraphDSL.Builder[NotUsed] => Shape as a param,it internally create a builder and call this function by passing it

  val graph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

    val in: Source[Int, NotUsed] = Source(1 to 10)
    val out: Sink[Any, Future[Done]] = Sink.foreach(println)

    //builder object is mutable,we can add junctions to it

    val broadcastJ: Broadcast[Int] = Broadcast[Int](2)
    val mergeJ: Merge[Int] = Merge[Int](2)


    val f1 = Flow[Int].map(_ + 10)
    val f2 = Flow[Int].filter(_ % 2 == 0)
    val f3 = Flow[Int].filter(_ % 2 != 0)
    val f4 = Flow[Int].map(_ + 100)

    val bcast: UniformFanOutShape[Int, Int] = builder.add(broadcastJ)
    val merge: UniformFanInShape[Int, Int] = builder.add(mergeJ)


    //~> is called edge or via or to operator ,this is available by importing GraphDSL.Implicits._
    //this operation gets builder obj implicitly and mutate it by adding edges
    //there is no need of adding in & out to builder explicitly,they are automatically added .. ???
    //after adding all the junctions to uilder,now use edge methods on them to connect flows which are edges.
    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge

    ClosedShape

  }
  //once the graph obj is created,it is immutable similar to flows,sources & sinks.So we can pass it around with peace

  val runnable: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(graph)

  runnable.run()

  //********************************************************************************************************************

  val topHeadSink: Sink[Any, Future[Done]] = Sink.foreach(println)
  val bottomHeadSink: Sink[Any, Future[Done]] = Sink.foreach(println)

  //val topHS: SinkShape[Any] = topHeadSink.shape
  //val bottomHS: SinkShape[Any] = bottomHeadSink.shape

  val sharedDoubler: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 2)

  val graph2: Graph[ClosedShape.type, (Future[Done], Future[Done])] = GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) { implicit builder =>
    (topHS, bottomHS) =>
      val bcast: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
      val source: Source[Int, NotUsed] = Source.single(1)

      source ~> bcast.in
      bcast.out(0) ~> sharedDoubler ~> topHS.in
      bcast.out(1) ~> sharedDoubler ~> bottomHS.in


      //how this is differen from above ???
      //      source ~> bcast ~> sharedDoubler ~> topHeadSink
      //      bcast ~> sharedDoubler ~> bottomHeadSink

      ClosedShape
  }

  val runnable2: RunnableGraph[(Future[Done], Future[Done])] = RunnableGraph.fromGraph(graph2)

  runnable2.run()

  //********************************************************************************************************************

  val resultSink = Sink.head[Int]


  //partial graph,not all the ports are connected.
  val graph3: Graph[UniformFanInShape[Int, Int], NotUsed] = GraphDSL.create() { implicit b =>

    val zip1: FanInShape2[Int, Int, Int] = b.add(ZipWith[Int, Int, Int](math.max _))

    val zip2: FanInShape2[Int, Int, Int] = b.add(ZipWith[Int, Int, Int](math.max _))

    val zip3: FanInShape2[Int, Int, Int] = b.add(ZipWith[Int, Int, Int]((x: Int, y: Int) => x + y))

    zip1.out ~> zip3.in0
    zip2.out ~> zip3.in1

    UniformFanInShape(zip3.out, zip1.in0, zip1.in1, zip2.in0, zip2.in1)

  }

  //create a graph using graph 3,graph 3 does not have all the ports connected,if we try to run it it will throw out exception.
  val graph4: Graph[ClosedShape.type, Future[Int]] = GraphDSL.create(resultSink) { implicit b =>
    (sink: Sink[Int, Future[Int]]#Shape) =>

      val pm3 = b.add(graph3)
      Source.single(1) ~> pm3.in(0)
      Source.single(2) ~> pm3.in(1)
      Source.single(3) ~> pm3.in(2)
      Source.single(4) ~> pm3.in(3)
      pm3.out ~> sink.in
      ClosedShape
  }

  val runnable3 = RunnableGraph.fromGraph(graph4)

  runnable3.run().map(x => println(x))


  //********************************************************************************************************************
  //actually source ,sink and flows are also partial graphs.
  //Source is a partial graph with exactly one output, that is it returns a SourceShape.
  //Sink is a partial graph with exactly one input, that is it returns a SinkShape.
  //Flow is a partial graph with exactly one input and exactly one output, that is it returns a FlowShape.

  //Being able to hide complex graphs inside of simple elements such as Sink / Source / Flow enables you to easily create one complex element
  // and from there on treat it as simple compound stage for linear computations.

  //creating a source graph

  val sourceGraph: Graph[SourceShape[(Int, Int)], NotUsed] = GraphDSL.create() { implicit b =>
    // prepare graph elements
    val zip: FanInShape2[Int, Int, (Int, Int)] = b.add(Zip[Int, Int]())

    val source: Source[Int, NotUsed] = Source(1 to 10)

    // connect the graph
    source.filter(_ % 2 != 0) ~> zip.in0
    source.filter(_ % 2 == 0) ~> zip.in1

    // expose port
    SourceShape(zip.out)
  }

  val source = Source.fromGraph(sourceGraph)

  val firstPair: Future[Done] = source.runWith(Sink.foreach(println))


  //********************************************************************************************************************

  val flowGraph: Graph[FlowShape[Int, (Int, String)], NotUsed] = GraphDSL.create() { implicit b =>

    val broadcast: UniformFanOutShape[Int, Int] = b.add(Broadcast[Int](2))
    val zip = b.add(Zip[Int, String]())

    // connect the graph
    broadcast.out(0).map(_ + 10) ~> zip.in0
    broadcast.out(1).map(_.toString) ~> zip.in1

    // expose ports
    FlowShape(broadcast.in, zip.out)
  }

  val flow: Flow[Int, (Int, String), NotUsed] = Flow.fromGraph(flowGraph)

  flow.runWith(Source(List(1, 2, 3, 4, 5)), Sink.foreach(println))

  //********************************************************************************************************************

  //combining sinks and sources can also be performed using a simple api,with out creating custom graphs

  val sourceOne = Source(List(1, 2, 3, 4))
  val sourceTwo = Source(List(5, 6, 7, 8))
  val mergedSource = Source.combine(sourceOne, sourceTwo)(Merge(_))

  val sumSink: Sink[Int, Future[Int]] = Sink.fold(0)((x, y) => {
    println(x, y)
    x + y
  })
  val printSink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

  val combinedSink: Sink[Int, NotUsed] = Sink.combine(sumSink, printSink)(Broadcast[Int](_))

  mergedSource.runWith(combinedSink)


  //********************************************************************************************************************

//  //Building reusable Graph components
//  //It is possible to build reusable, encapsulated components of arbitrary input and output ports using the graph DSL.
//
//  // a graph junction that represents a pool of workers, where a worker is expressed as a Flow[I,O,_], i.e. a simple transformation of jobs of type I to results of type O
//  // ( this flow can actually contain a complex graph inside).
//  // Our reusable worker pool junction will not preserve the order of the incoming jobs (they are assumed to have a proper ID field) and
//  // it will use a Balance junction to schedule jobs to available workers.
//  // On top of this, our junction will feature a "fastlane", a dedicated port where jobs of higher priority can be sent.
//
//  // A shape represents the input and output ports of a reusable
//  // processing module
//
//  // In general a custom Shape needs to
//  // be able to provide all its input and output ports,
//  // be able to copy itself, and also
//  // be able to create a new instance from given ports.
//  case class PriorityWorkerPoolShape[In, Out](jobsIn: Inlet[In], priorityJobsIn: Inlet[In], resultsOut: Outlet[Out]) extends Shape {
//
//    // It is important to provide the list of all input and output
//    // ports with a stable order. Duplicates are not allowed.
//    override val inlets: Seq[Inlet[_]] = jobsIn :: priorityJobsIn :: Nil
//    override val outlets: Seq[Outlet[_]] = resultsOut :: Nil
//
//    // A Shape must be able to create a copy of itself. Basically
//    // it means a new instance with copies of the ports
//    override def deepCopy() = PriorityWorkerPoolShape(
//      jobsIn.carbonCopy(),
//      priorityJobsIn.carbonCopy(),
//      resultsOut.carbonCopy())
//
//    // A Shape must also be able to create itself from existing ports
//    override def copyFromPorts(inlets: collection.immutable.Seq[Inlet[_]], outlets: collection.immutable.Seq[Outlet[_]]): Shape = {
//      assert(inlets.size == this.inlets.size)
//      assert(outlets.size == this.outlets.size)
//      // This is why order matters when overriding inlets and outlets.
//      PriorityWorkerPoolShape[In, Out](inlets(0).as[In], inlets(1).as[In], outlets(0).as[Out])
//    }
//  }

  //********************************************************************************************************************




}