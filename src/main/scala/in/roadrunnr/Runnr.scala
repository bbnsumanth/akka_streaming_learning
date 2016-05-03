//package in.roadrunnr
//
//import java.io.File
//import akka.event.Logging
//import akka.{Done, NotUsed}
//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
//import akka.stream._
//import akka.stream.scaladsl._
//import akka.util.ByteString
//
//import scala.concurrent.Future
//
//
//object Runnr extends App {
//
//  implicit val system: ActorSystem = ActorSystem("QuickStart")
//  implicit val materializer = ActorMaterializer()
//  implicit val ec = system.dispatcher
//
//  //The Source type is parameterized with two types: the first one is the type of element that this source emits and the second one may signal
//  // that running the source produces some auxiliary value (e.g. a network source may provide information about the
//  // bound port or the peer’s address). Where no auxiliary information is produced, the type akka.NotUsed is used
//  val source: Source[Int, NotUsed] = Source(1 to 100)
//
//  //val res : Future[Done] = source.filter(i => i % 20 == 0).runForeach(i => println(i))(materializer)
//
//  val factorials: Source[BigInt, NotUsed] = source.scan(BigInt(1))((acc, next) => acc * next)
//
//  val res2 = factorials.runForeach(i => println(i))
//
//  val result: Future[IOResult] =
//    factorials
//      .map(num => ByteString(s"$num\n"))
//      .runWith(FileIO.toFile(new File("factorials.txt")))
//
//  res2.map(f => println(f))
//  result.map(f => println(f))
//
//  def lineSink(filename: String): Sink[String, Future[IOResult]] =
//    Flow[String]
//      .map(s => ByteString(s + "\n"))
//      .toMat(FileIO.toFile(new File(filename)))(Keep.right)
//
//  val res3: Future[IOResult] = factorials.map(_.toString).runWith(lineSink("factorial2.txt"))
//
//  res3.map(println)
//
//  val done: Future[Done] =
//    factorials
//      .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
//      .throttle(1, 1.second, 1, ThrottleMode.shaping)
//      .runForeach(println)
//
//  val loggedSource: Source[Int, NotUsed] = source.map { elem => println(elem); elem }
//
//
//  // customise log levels
//  source.log("before-map")
//    .withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel))
//    .map(analyse)
//
//  // or provide custom logging adapter
//  implicit val adapter = Logging(system, "customLogger")
//  source.log("custom")
//
//}
