package com.noisycode.factor

import scala.annotation.tailrec

case class Context[T](pid: FactorPid[T], sys: FactorSystem)

class FactorSystem {
  type ActorQueue = List[FactorPid[_]]

  private var actors: ActorQueue = List()

  def spawn[T](f: Factor.Factor[T], init: T) = {
    actors = FactorPid(f, init, this) :: actors
    actors.head
  }
    

  def fullRun() {
    actors = fullRun(actors, Nil)
  }

  @tailrec
  private def fullRun(toDo: ActorQueue, done: ActorQueue): ActorQueue = toDo match {
    case (pid :: rest) =>
      pid.receive() match {
	case false => fullRun(rest, done)
	case true => fullRun(rest, pid :: done)
      }
    case List() => done
  }
}
