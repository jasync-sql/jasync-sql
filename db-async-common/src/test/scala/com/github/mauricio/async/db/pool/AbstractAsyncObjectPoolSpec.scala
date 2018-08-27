package com.github.mauricio.async.db.pool

import com.github.mauricio.async.db.pool.AbstractAsyncObjectPoolSpec.Widget
import org.mockito.Mockito.reset
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.util.Failure

import scala.reflect.runtime.universe.TypeTag
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * This spec is designed abstract to allow testing of any implementation of AsyncObjectPool, against the common
  * requirements the interface expects.
  *
  * @tparam T the AsyncObjectPool being tested.
  */
abstract class AbstractAsyncObjectPoolSpec[T <: AsyncObjectPool[Widget]](implicit tag: TypeTag[T])
  extends Specification
    with Mockito {

  import AbstractAsyncObjectPoolSpec._

  protected def pool(factory: ObjectFactory[Widget] = new TestWidgetFactory, conf: PoolConfiguration = PoolConfiguration.Default): T

  // Evaluates to the type of AsyncObjectPool
  s"the ${tag.tpe.erasure} variant of AsyncObjectPool" should {

    "successfully retrieve and return a Widget" in {
      val p = pool()
      val widget = Await.result(p.take, Duration.Inf)

      widget must not beNull

      val thePool = Await.result(p.giveBack(widget), Duration.Inf)
      thePool must be(p)
    }

    "reject Widgets that did not come from it" in {
      val p = pool()

      Await.result(p.giveBack(Widget(null)), Duration.Inf) must throwAn[IllegalArgumentException]
    }

    "scale contents" >> {
      sequential

      val factory = spy(new TestWidgetFactory)

      val p = pool(
        factory = factory,
        conf = PoolConfiguration(
          maxObjects = 5,
          maxIdle = 2,
          maxQueueSize = 5,
          validationInterval = 2000
        ))



      var taken = Seq.empty[Widget]
      "can take up to maxObjects" in {
        taken = Await.result(Future.sequence(for (i <- 1 to 5) yield p.take), Duration.Inf)

        taken must have size 5
        taken.head must not beNull;
        taken(1) must not beNull;
        taken(2) must not beNull;
        taken(3) must not beNull;
        taken(4) must not beNull
      }

      "does not attempt to expire taken items" in {
        // Wait 3 seconds to ensure idle check has run at least once
        there was after(3.seconds).no(factory).destroy(any[Widget])
      }

      reset(factory) // Considered bad form, but necessary as we depend on previous state in these tests
      "takes maxObjects back" in {
        val returns = Await.result(Future.sequence(for (widget <- taken) yield p.giveBack(widget)), Duration.Inf)

        returns must have size 5

        returns.head must be(p)
        returns(1) must be(p)
        returns(2) must be(p)
        returns(3) must be(p)
        returns(4) must be(p)
      }

      "protest returning an item that was already returned" in {
        val resultFuture = p.giveBack(taken.head)

        Await.result(resultFuture, Duration.Inf) must throwAn[IllegalStateException]
      }

      "destroy down to maxIdle widgets" in {
        Thread.sleep(3000)
        there were 5.times(factory).destroy(any[Widget])
      }
    }

    "queue requests after running out" in {
      val p = pool(conf = PoolConfiguration.Default.copy(maxObjects = 2, maxQueueSize = 1))

      val widgets = Await.result(Future.sequence(for (i <- 1 to 2) yield p.take), Duration.Inf)

      val future = p.take

      // Wait five seconds
      Thread.sleep(5000)

      val failedFuture = p.take

      // Cannot be done, would exceed maxObjects
      future.isCompleted must beFalse

      Await.result(failedFuture, Duration.Inf) must throwA[PoolExhaustedException]

      Await.result(p.giveBack(widgets.head), Duration.Inf) must be(p)

      Await.result(future, Duration(5, SECONDS)) must be(widgets.head)
    }

    "refuse to allow take after being closed" in {
      val p = pool()

      Await.result(p.close, Duration.Inf) must be(p)

      Await.result(p.take, Duration.Inf) must throwA[PoolAlreadyTerminatedException]
    }

    "allow being closed more than once" in {
      val p = pool()

      Await.result(p.close, Duration.Inf) must be(p)

      Await.result(p.close, Duration.Inf) must be(p)
    }


    "destroy a failed widget" in {
      val factory = spy(new TestWidgetFactory)
      val p = pool(factory = factory)

      val widget = Await.result(p.take, Duration.Inf)

      widget must not beNull

      factory.validate(widget) returns Failure(new RuntimeException("This is a bad widget!"))

      Await.result(p.giveBack(widget), Duration.Inf) must throwA[RuntimeException](message = "This is a bad widget!")

      there was atLeastOne(factory).destroy(widget)
    }

    "clean up widgets that die in the pool" in {
      val factory = spy(new TestWidgetFactory)
      // Deliberately make it impossible to expire (nearly)
      val p = pool(factory = factory, conf = PoolConfiguration.Default.copy(maxIdle = Long.MaxValue, validationInterval = 2000))

      val widget = Await.result(p.take, Duration.Inf)

      widget must not beNull

      Await.result(p.giveBack(widget), Duration.Inf) must be(p)

      there was atLeastOne(factory).validate(widget)
      there were no(factory).destroy(widget)

      there was after(3.seconds).atLeastTwo(factory).validate(widget)

      factory.validate(widget) returns Failure(new RuntimeException("Test Exception, Not an Error"))

      there was after(3.seconds).one(factory).destroy(widget)

      Await.ready(p.take, Duration.Inf)

      there was two(factory).create
    }

  }

}

object AbstractAsyncObjectPoolSpec {

  case class Widget(factory: TestWidgetFactory)

  class TestWidgetFactory extends ObjectFactory[Widget] {

    override def create: Widget = Widget(this)

    override def destroy(item: Widget) = {}

    override def validate(item: Widget): Try[Widget] = Try {
      if (item.factory eq this)
        item
      else
        throw new IllegalArgumentException("Not our item")
    }
  }

}


class SingleThreadedAsyncObjectPoolSpec extends AbstractAsyncObjectPoolSpec[SingleThreadedAsyncObjectPool[Widget]] {

  import AbstractAsyncObjectPoolSpec._

  override protected def pool(factory: ObjectFactory[Widget], conf: PoolConfiguration) =
    new SingleThreadedAsyncObjectPool(factory, conf)

  "SingleThreadedAsyncObjectPool" should {
    "successfully record a closed state" in {
      val p = pool()

      Await.result(p.close, Duration.Inf) must be(p)

      p.isClosed must beTrue
    }

  }

}
