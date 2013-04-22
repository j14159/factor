package com.noisycode.factor

import Factor._

import org.scalatest._

class PingPongFactorTest extends FunSpec with GivenWhenThen {
  case class Ping[T](pid: FactorPid[T])
  case object Pong

  case class PingState(ponger: Option[FactorPid[_]], count: Int)

  describe("A ping actor and pong actor in a system with no threads") {
    it("They should ping/pong correctly (HURR)") {
      given("A FactorSystem, pinger and ponger")
      val sys = new FactorSystem

      val pinger = sys.spawn(pingFunc, PingState(None, 0))
      val ponger = sys.spawn(pongFunc, None)

      when("the pinger starts and the FactorSystem steps once to load the pings state")
      pinger ! PingState(Some(ponger), 0)
      sys.fullRun()

      and("the FactorSystem steps two more times")
      sys.fullRun()
      sys.fullRun()

      then("pinger's count in state should be as expected")
      assert(pinger.readState == PingState(Some(ponger), 1))

      and("when the FactorSystem runs 4 more times, the pinger should have stopped")
      for(_ <- 1 to 6) sys.fullRun()
      assert(sys.actors == List(ponger))
    }
  }

  def pingFunc: Factor[PingState] = {
    case (PingState(ponger, count), Context(pid, _), _) => 
      ponger map (_ ! Ping(pid))
      Ok(PingState(ponger, count))
    case (Pong, Context(pid, _), PingState(ponger, count)) if count < 3 =>
      ponger.map(_ ! Ping(pid))
      Ok(PingState(ponger, count + 1))
    case (Pong, _, s @ PingState(_, 3)) =>
      Stop('done, s)
  }

  def pongFunc: Factor[Any] = {
    case (Ping(pinger), _, state) => 
      pinger ! Pong
      Ok(state)
  }
}
