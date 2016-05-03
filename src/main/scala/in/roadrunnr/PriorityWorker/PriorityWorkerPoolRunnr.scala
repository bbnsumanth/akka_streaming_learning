package in.roadrunnr.PriorityWorker

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape, Graph}

object PriorityWorkerPoolRunnr extends App {

  implicit val system: ActorSystem = ActorSystem("Runnr1")
  implicit val materializer = ActorMaterializer()

  val worker1 = Flow[String].map("step 1 " + _)
  val worker2 = Flow[String].map("step 2 " + _)

  val graph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit b =>

    //to create our custom junction PriorityWorkerPool,we need to create it as a custom graph(PriorityWorkerPool) as all components are graphs in akka streams
    //to construct a graph with out custom input and output ports,we need to create a custom shape(PriorityWorkerPoolShape)
    val priorityPool1 = b.add(PriorityWorkerPool(worker1, 4))
    val priorityPool2 = b.add(PriorityWorkerPool(worker2, 2))

    Source(1 to 10).map("job: " + _) ~> priorityPool1.jobsIn
    Source(1 to 10).map("priority job: " + _) ~> priorityPool1.priorityJobsIn

    priorityPool1.resultsOut ~> priorityPool2.jobsIn
    Source(1 to 10).map("one-step, priority " + _) ~> priorityPool2.priorityJobsIn

    priorityPool2.resultsOut ~> Sink.foreach(println)

    ClosedShape
  }

  val runnable: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(graph)

  runnable.run()

}
