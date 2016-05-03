package in.roadrunnr.PriorityWorker

import akka.NotUsed
import akka.stream.Graph
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import PriorityWorkerPoolShape

object PriorityWorkerPool {
  def apply[In, Out](worker: Flow[In, Out, Any],workerCount: Int): Graph[PriorityWorkerPoolShape[In, Out], NotUsed] = {

    val graph = GraphDSL.create() { implicit b ⇒

      val priorityMerge = b.add(MergePreferred[In](1))
      val balance = b.add(Balance[In](workerCount))
      val resultsMerge = b.add(Merge[Out](workerCount))

      // After merging priority and ordinary jobs, we feed them to the balancer
      priorityMerge ~> balance

      // Wire up each of the outputs of the balancer to a worker flow
      // then merge them back
      for (i <- 0 until workerCount)
        balance.out(i) ~> worker ~> resultsMerge.in(i)

      // We now expose the input ports of the priorityMerge and the output
      // of the resultsMerge as our PriorityWorkerPool ports
      // -- all neatly wrapped in our domain specific Shape
      PriorityWorkerPoolShape(
        jobsIn = priorityMerge.in(0),
        priorityJobsIn = priorityMerge.preferred,
        resultsOut = resultsMerge.out)
    }

    graph

  }

}
