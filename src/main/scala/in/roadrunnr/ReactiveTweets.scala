//package in.roadrunnr
//
//import akka.stream.scaladsl.GraphDSL.Builder
//import akka.{Done, NotUsed}
//import akka.actor.ActorSystem
//import akka.stream.{UniformFanOutShape, ClosedShape, ActorMaterializer}
//import akka.stream.scaladsl._
//import akka.stream.scaladsl.GraphDSL.Implicits._
//
//import scala.concurrent.Future
//
//object ReactiveTweets extends App{
//
//  implicit val system = ActorSystem("reactive-tweets")
//  implicit val materializer = ActorMaterializer()
//  import GraphDSL.Implicits._
//
//  final case class Author(handle: String)
//
//  final case class Hashtag(name: String)
//
//  final case class Tweet(author: Author, timestamp: Long, body: String) {
//    def hashtags: Set[Hashtag] =
//      body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
//  }
//
//  val akka = Hashtag("#akka")
//
//  //Streams always start flowing from a Source[Out,M1] then can continue through Flow[In,Out,M2] elements or more advanced graph elements to
//  // finally be consumed by a Sink[In,M3]
//  val tweets: Source[Tweet, NotUsed] = ???
//
//  val authors: Source[Author, NotUsed] =
//    tweets
//      .filter((tweet: Tweet) => tweet.hashtags.contains(akka))
//      .map(_.author)
//
//  val result: Future[Done] = authors.runWith(Sink.foreach(println))
//
//  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags.toList)
//
//  val writeAuthors: Sink[Author, Unit] = ???
//  val writeHashtags: Sink[Hashtag, Unit] = ???
//  val g = RunnableGraph.fromGraph(GraphDSL.create() { (b: Builder[NotUsed]) =>
//
//    val bcast: UniformFanOutShape[Tweet, Tweet] = b.add(Broadcast[Tweet](2))
//    tweets ~> bcast.in
//    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
//    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
//    ClosedShape
//  })
//
//  val resGraph: NotUsed = g.run()
//
//
//
//
//
//
//}
