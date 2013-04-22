package com.noisycode.factor

import scala.annotation.tailrec

/**
 * Context is provided to actors so that they know their PID (without self()) and
 * their enclosing system.
 */
case class Context[T](pid: FactorPid[T], sys: FactorSystem)

/**
 * Manages a set of actors and their concurrent execution.
 */
class FactorSystem {
  /**
   * Shorthand type.
   */
  type ActorQueue = List[FactorPid[_]]

  private [factor] var actors: ActorQueue = List()

  /**
   * Create a new actor.
   * @param f the initial function to use for processing messages.
   * @param init the initial actor state.
   */
  def spawn[T](f: Factor.Factor[T], init: T) = {
    actors = FactorPid(f, init, this) :: actors
    actors.head
  }
    
  /**
   * Helper method that will let each actor process a single message without
   * spinning up any threads, etc.
   */
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
