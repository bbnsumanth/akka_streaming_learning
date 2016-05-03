package in.roadrunnr

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.{Done, NotUsed}
import akka.stream.scaladsl._

import scala.collection.immutable.Range.Inclusive
import scala.concurrent.{ExecutionContextExecutor, Future}


object Runnr1 extends App {

  implicit val system: ActorSystem = ActorSystem("Runnr1")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val data: Inclusive = (1 to 10)

  val source: Source[Int, NotUsed] = Source(data)

  //every processing stage is immutable,so doing any transformation on source wont change the source,
  // instead it creates a new source which we assigned to source2
  val source2: Source[Int, NotUsed] = source.map(_*2)

  val sink1: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
  val sink2: Sink[Int, Future[Done]] = Sink.foreach[Int]((x) => println(x))
  val sink3: Sink[Int, Future[Int]] = Sink.reduce[Int](_ + _)

  // connect the Source to the Sink, obtaining a RunnableGraph
  val runnable1: RunnableGraph[Future[Int]] = source.toMat(sink1)(Keep.right)
  //any processing stage can be reused,as they are immutable objects and they only describe a computation.
  val runnable2: RunnableGraph[Future[Done]] = source.toMat(sink2)(Keep.right)
  //direct way to run a materialized flow
  val res3: Future[Int] = source2.runWith(sink3)

  // materialize the flow and get the value of the FoldSink
  val res1: Future[Int] = runnable1.run()
  val res2: Future[Done] = runnable2.run()

  //By default Akka Streams elements support exactly one downstream processing stage.
  // Making fan-out (supporting multiple downstream processing stages) an explicit opt-in feature allows default stream elements to be less complex
  // and more efficient. Also it allows for greater flexibility on how exactly to handle the multicast scenarios,by providing named fan-out elements
  // such as broadcast (signals all down-stream elements) or balance (signals one of available down-stream elements).


  for {
    r1 <- res1
    r2 <- res2
    r3 <- res3
  }yield println(s"$r1 ,$r2, $r3")

  //Since a stream can be materialized multiple times, the materialized value will also be calculated anew for each such materialization,
  // usually leading to different values being returned each time

  val sum1: Future[Int] = runnable1.run()
  val sum2: Future[Int] = runnable1.run()

  println(sum1 == sum2)

  for{
    r1 <- sum1
    r2 <- sum2
  }yield{
    println(r1,r2)
  }

  val s1: Source[Int, NotUsed] = Source(List(1, 2, 3))

  val s2: Source[String, NotUsed] = Source.fromFuture(Future.successful("Hello Streams!"))

  val s3: Source[String, NotUsed] = Source.single("only one element")

  val s4: Source[Nothing, NotUsed] = Source.empty

  // Sink that folds over the stream and returns a Future
  // of the final result as its materialized value
  val si1: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  // Sink that returns a Future as its materialized value,
  // containing the first element of the stream
  val si2: Sink[Nothing, Future[Nothing]] = Sink.head

  // A Sink that consumes a stream without doing anything with the elements
  val si3: Sink[Any, Future[Done]] = Sink.ignore

  // A Sink that executes a side-effecting call for every element of the stream
  val si4: Sink[String, Future[Done]] = Sink.foreach(println(_))


  // Explicitly creating and wiring up a Source, Sink and Flow
  val m: RunnableGraph[NotUsed] = s1.via(Flow[Int].map(_ * 2)).to(Sink.foreach(println(_)))
  //s1.via(Flow[Int].map(_ * 2)).to(si4)//this is not working ???

  // Starting from a Source,this is very similar to s1.via(Flow[Int].map(_ * 2))
  val sourcee: Source[Int, NotUsed] = s1.map(_ * 2)
  sourcee.to(Sink.foreach(println(_)))

  // Starting from a Sink
  val sink: Sink[Int, NotUsed] = Flow[Int].map(_ * 2).to(Sink.foreach(println(_)))
  s1.to(sink)

  // Broadcast to a sink inline
  val otherSink: Sink[Int, NotUsed] = Flow[Int]
    .alsoTo(Sink.foreach(println(_)))
    .to(Sink.ignore)

  s1.to(otherSink)



}
