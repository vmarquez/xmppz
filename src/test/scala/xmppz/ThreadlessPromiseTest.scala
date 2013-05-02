package xmppz
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import util._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.global
import scalaz.{ Writer, WriterT, EitherT, \/, Id, OptionT }
import scalaz.std.list.listMonoid
import scalaz._
import Scalaz._
import xmppz._
import java.util.concurrent.{ TimeUnit, CountDownLatch }

import scalaz.OptionT._
import scalaz.std.list.listMonoid
import scalaz.std.option.optionInstance

import util.PromiseT._

import util.Log._
import util._

@RunWith(classOf[JUnitRunner])
class ThreadlessPromiseTest extends FunSuite {
  test("listener stuff") {
    val p1 = new ThreadlessPromise[Int]()
    val p2 = new ThreadlessPromise[Int]()
    val newp =
      for {
        i1 <- p1.future
        i2 <- p2.future
      } yield "Total = " + (i1 + i2)

    newp.foreach(str => println("!!! = " + str))

    println("fulfilling first")
    p1.success(5)
    println("about to sleep")
    p2.success(2)
    println("done!")
  }

  //final case class OptionT[F[+_], +A](run: F[Option[A]]) {
  test("Testing PromiseT") {
    println("testing logging with futures")
    val latch = new CountDownLatch(1)
    val of1: Option[Future[Int]] = Some(Future {
      Thread.sleep(500)
      4
    })

    val of2: Option[Future[Int]] = Some(Future {
      Thread.sleep(500)
      5
    })

    val futureT1 = PromiseT(of1)
    val futureT2 = PromiseT(of2)
    val i =
      for {
        a <- futureT1
        b <- futureT2
        _ = assert((a + b) == 9)
        _ = latch.countDown()
      } yield a + b
    assert(latch.await(2, TimeUnit.SECONDS))
  }

}

