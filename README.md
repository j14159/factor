factor
======

"function".head + "actor"

A simple experiment using only Scala functions as actors, no enclosing class/trait/etc necessary.

#What?

A simple exploration of a few things that have come up while digging deeper into both Akka and Erlang.

Example:

	def myActorFunc: Factor[String] = { case (s: String, _, state) => Ok(state + s) }
	new FactorSystem.spawn(myActorFunc, "")

It's still very basic, no thread pool/FJP stuff yet, no supervision, etc.  I'll update this page as that changes and expect a blog post or two about it.

The short list of things I want to do at present:

* supervision, links
* use Doug Lea's FJ stuff for parallelism (should be pretty easy)
* experiment with some simple scheduler design
* OIO wrapper func that will spin a new thread allowed to block, will expand on this later.

#Why?

Erlang's approach to actors using recursive functions is awfully nice, gen\_server even more so.  It conveniently elminates any sort of mutable state.  Obviously you _could_
close over some external state with your actor functions but you and I both know that clearly would be **stupid** (so please don't do that).  This code is roughly
based on some ideas from gen\_server.  _Roughly_.

The idea here is to build actors from absolutely nothing but functions.  You might define those functions in classes, objects, traits, whatever - the key being that you
use nothing but the function parameters for state tracking.  I've modelled this currently as partial functions, might change that to normal functions later.

#Is This Supposed To Replace Something?

No.  Don't use it for anything serious, it's just a little experiment.

#What About Akka?

You should use Akka.  It's good.  A few directions I'm planning to explore with this are based on some assumptions and knowledge I have about how Akka does actors.