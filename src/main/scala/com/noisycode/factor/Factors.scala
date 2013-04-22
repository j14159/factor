package com.noisycode.factor

import java.util.concurrent.ConcurrentLinkedQueue

object Factor {
  type Factor[T] = PartialFunction[(Any, Context[T], T), (Result[T])]

  sealed trait Result[T]

  case class Ok[T](next: Factor[T], state: T) extends Result[T]
  case class Reply[T](next: Factor[T], reply: Any, state: T) extends Result[T]
  case class Crash[T](reason: Throwable, state: T) extends Result[T]
  case class Stop[T](reason: Any, state: T) extends Result[T]
}

case class FactorPid[T](initFunc: Factor.Factor[T], init: T, sys: FactorSystem) {
  private[factor] val inbox = new ConcurrentLinkedQueue[(Any, Option[FactorPid[_]])]()

  import Factor._

  private var state = init
  private var nextFunc = initFunc

  def ! (msg: Any, sender: Option[FactorPid[_]] = None) = inbox add (msg, sender)

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

  private [factor] def readState = state

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

  def processCrash(crash: Crash[T]) = {
    println(s"Stopping pid for crash, ${crash.reason} ::: ${crash.state}")
    //TODO:  handle links
    None
  }
}
