package com.noisycode.factor

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Container object for actor function type and actor result
 * classes.
 */
object Factor {
  /**
   * Shorthand type for defining partial functions for actors.
   */
  type Factor[T] = PartialFunction[(Any, Context[T], T), (Result[T])]

  /**
   * Base trait for all actor results.
   */
  sealed trait Result[T]

  /**
   * Return this from an actor to indicate the actor is continuing to operate
   * without errors.
   * @param next the next function to use for this actor.
   * @param state the updated state to use for the next time the actor processes a message.
   */
  case class Ok[T](next: Factor[T], state: T) extends Result[T]
  /**
   * Return this from an actor to reply to whichever actor sent the initial message.
   * @param reply the message to send back to the calling actor.
   */
  case class Reply[T](next: Factor[T], reply: Any, state: T) extends Result[T]
  /**
   * Indicates a crashed actor.  DERP.
   */
  case class Crash[T](reason: Throwable, state: T) extends Result[T]
  /**
   * Return this to stop the actor in a known/acceptable state (not a crash).
   */
  case class Stop[T](reason: Any, state: T) extends Result[T]
}

/**
 * This class provides a handle to an actor and maintains its mailbox.
 * @param initFunc the first function to use for message handling in this actor.
 * @param init the initial state of this actor.
 * @param sys the [[FactorSystem]] that owns and manages this actor.
 */
case class FactorPid[T](initFunc: Factor.Factor[T], init: T, sys: FactorSystem) {
  private[factor] val inbox = new ConcurrentLinkedQueue[(Any, Option[FactorPid[_]])]()

  import Factor._

  private var state = init
  private var nextFunc = initFunc

  /**
   * Send (asynchronously) a message to this actor, possibly expecting a reply.
   * @param msg the message to pass to this actor.
   * @param sender optionally the actor sending the message.  Supply this if it's being sent by an actor who expects a reply.
   */
  def ! (msg: Any, sender: Option[FactorPid[_]] = None) = inbox add (msg, sender)

  /**
   * Causes the actor to receive and process an individual message from it's inbox.
   * @return true if the actor is still running, false if not.
   */
  private [factor] def receive() = inbox poll match {
    case null => true
    case (msg, sender) => process(msg, sender) match {
      case Some((f, s)) =>
	println(s"got back ${f} ${s}")
	nextFunc = f
	state = s
	true
      case _ => false
    }
  }

  /**
   * Convenience method to peek at the actor's state.
   */
  private [factor] def readState = state

  /**
   * Handles the actual processing of the message and the result type or crash.
   */
  private def process(msg: Any, sender: Option[FactorPid[_]]): Option[(Factor[T], T)] = try {
    nextFunc((msg, Context(this, sys), state)) match {
      case Ok(n, s) => Some((n, s))
      case Reply(n, r, s) =>
	sender map (s => s ! r)
	Some((n, s))
      case Stop(r, s) =>
	//TODO:  handle links
	println(s"Stopping pid, reason ${r} with state ${s}")
	None
      case crash @ Crash(r, s) => processCrash(crash)
    }
  } catch {
    case e: Throwable => processCrash(Crash(e, state))
  }

  /**
   * Deal with the fallout from a crash.
   */
  private def processCrash(crash: Crash[T]) = {
    println(s"Stopping pid for crash, ${crash.reason} ::: ${crash.state}")
    //TODO:  handle links
    None
  }
}
