package in.roadrunnr.PriorityWorker

import akka.stream.{Inlet, Outlet, Shape}

//Building reusable Graph components
//It is possible to build reusable, encapsulated components of arbitrary input and output ports using the graph DSL.

// a graph junction that represents a pool of workers, where a worker is expressed as a Flow[I,O,_], i.e. a simple transformation of jobs of type I to results of type O
// ( this flow can actually contain a complex graph inside).
// Our reusable worker pool junction will not preserve the order of the incoming jobs (they are assumed to have a proper ID field) and
// it will use a Balance junction to schedule jobs to available workers.
// On top of this, our junction will feature a "fastlane", a dedicated port where jobs of higher priority can be sent.

// A shape represents the input and output ports of a reusable
// processing module

// In general a custom Shape needs to
// be able to provide all its input and output ports,
// be able to copy itself, and also
// be able to create a new instance from given ports.
case class PriorityWorkerPoolShape[In, Out](jobsIn: Inlet[In], priorityJobsIn: Inlet[In], resultsOut: Outlet[Out]) extends Shape {

  // It is important to provide the list of all input and output
  // ports with a stable order. Duplicates are not allowed.
  override val inlets: collection.immutable.Seq[Inlet[_]] = jobsIn :: priorityJobsIn :: Nil
  override val outlets: collection.immutable.Seq[Outlet[_]] = resultsOut :: Nil

  // A Shape must be able to create a copy of itself. Basically
  // it means a new instance with copies of the ports
  override def deepCopy() = PriorityWorkerPoolShape(
    jobsIn.carbonCopy(),
    priorityJobsIn.carbonCopy(),
    resultsOut.carbonCopy())

  // A Shape must also be able to create itself from existing ports
  override def copyFromPorts(inlets: collection.immutable.Seq[Inlet[_]], outlets: collection.immutable.Seq[Outlet[_]]): Shape = {
    assert(inlets.size == this.inlets.size)
    assert(outlets.size == this.outlets.size)
    // This is why order matters when overriding inlets and outlets.
    PriorityWorkerPoolShape[In, Out](inlets(0).as[In], inlets(1).as[In], outlets(0).as[Out])
  }
}