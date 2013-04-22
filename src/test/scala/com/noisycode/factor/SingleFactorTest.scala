package com.noisycode.factor

import org.scalatest._

class SingleFactorTest extends FunSpec with GivenWhenThen {

  describe("A single Factor in a system with no threads") {
    it("Should receive messages correctly when the system steps") {
      given("a FactorSystem")
      val sys = new FactorSystem

      and("a single simple actor")
      //can use lazy val here too:
      lazy val f: Factor.Factor[Int] = {
	case ('step, _, 0) => 
	  println("Got one")
	  Factor.Ok(f, 1)
      }
      val pid = sys.spawn(f, 0)

      when("the expected message is sent and the system stepped")
      pid ! 'step
      sys.fullRun()

      then("the factor's state should be as expected")
      assert(pid.readState == 1)
    }
  }
}
