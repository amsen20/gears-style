import munit.Clue.generate
import org.scalacheck.Prop.forAll
import gears.async.Future
import gears.async.Async
import gears.async.AsyncOperations

// This import got me stuck for a while,
// It defines the default Async implicit
import gears.async.default.given

import scala.concurrent.ExecutionContext
import scala.util.Success
import Pipeline.scale

class PiplelineSuite extends munit.FunSuite {
  given ExecutionContext = ExecutionContext.global

  test("simple pipeline") {
    Async.blocking:
      val stagePlus10 = stream => Pipeline.applyBinOp[Int](_ + _, stream, 10)
      val stageMul2 = stream => Pipeline.applyBinOp[Int](_ * _, stream, 2)
      val pipeline = stageMul2 andThen stagePlus10

      val ls = List(1, 2, 3, 4, 5)
      val (inputStream, _) = Pipeline.generator(ls: _*)
      val outputStream = pipeline(inputStream)

      for (x <- ls)
      do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => assertEquals(v, x * 2 + 10)
  }

  test("pipeline cancellation") {
    Async.blocking:
      val stagePlus10 = stream => Pipeline.applyBinOp[Int](_ + _, stream, 10)
      val stageMul2 = stream => Pipeline.applyBinOp[Int](_ * _, stream, 2)
      val pipeline = stageMul2 andThen stagePlus10

      val ls = List(1, 2, 3, 4, 5)
      val (inputStream, generatorFuture) = Pipeline.generator(ls: _*)
      val outputStream = pipeline(inputStream)

      val lls = ls.slice(0, ls.length / 2)
      for (x <- lls)
      do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => assertEquals(v, x * 2 + 10)

      generatorFuture.cancel()
      outputStream.read()

      outputStream.read() match
        case Left(_)  => ()
        case Right(v) => println(v); fail(s"Expected end of stream but got $v")
  }

  def delayedPlus(a: Int, b: Int)(using Async) =
    AsyncOperations.sleep(1000)
    a + b

  test("scaled pipeline output") {
    Async.blocking:
      val stagePlus10 = stream => Pipeline.applyBinOp[Int](_ + _, stream, 10)
      val stageMul2 = stream => Pipeline.applyBinOp[Int](_ * _, stream, 2)
      val pipeline = stageMul2 andThen scale(stagePlus10, 10)

      val ls = List(1, 2, 3, 4, 5)
      val (inputStream, _) = Pipeline.generator(ls: _*)
      val outputStream = pipeline(inputStream)

      var outputSet = Set.empty[Int]

      for (_ <- (0 until 5)) do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => outputSet += v

      assertEquals(outputSet, ls.map(_ * 2 + 10).to(Set))
  }
}
