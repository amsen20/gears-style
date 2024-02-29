import org.scalacheck.Prop.forAll
import gears.async.Future
import gears.async.Async
import gears.async.AsyncOperations

// This import got me stuck for a while,
// It defines the default Async implicit
import gears.async.default.given

import scala.concurrent.ExecutionContext
import scala.util.Success
import gears.async.SyncChannel
import Pipeline.>>
import Pipeline.*

class PipelineSuite extends munit.FunSuite {
  given ExecutionContext = ExecutionContext.global

  val simplePipeline =
    (ls: List[Int]) =>
      Pipeline.generator(ls*) >>
        Pipeline.applyBinOp[Int](_ * _, 2) >>
        Pipeline.applyBinOp[Int](_ + _, 10)

  test("simple pipeline") {
    Async.blocking:
      val ls = List(1, 2, 3, 4, 5)

      val outputStream = SyncChannel[Int]()
      val f = Future(simplePipeline(ls)(outputStream))

      for (x <- ls)
      do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => assertEquals(v, x * 2 + 10)

      f.await
  }

  test("pipeline cancellation") {
    Async.blocking:
      val ls = List(1, 2, 3, 4, 5)

      val outputStream = SyncChannel[Int]()
      val f = Future(simplePipeline(ls)(outputStream))

      val lls = ls.slice(0, ls.length / 2)
      for (x <- lls)
      do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => assertEquals(v, x * 2 + 10)

      f.cancel()
      outputStream.read()

      outputStream.read() match
        case Left(_)  => ()
        case Right(v) => println(v); fail(s"Expected end of stream but got $v")
  }

  test("scaled pipeline output") {
    Async.blocking:
      val myPipeline =
        (ls: List[Int]) =>
          Pipeline.generator(ls*) >>
            Pipeline.applyBinOp[Int](_ * _, 2) * ls.length >>
            Pipeline.applyBinOp[Int](_ + _, 10)

      val ls = List(1, 2, 3, 4, 5)
      val outputStream = SyncChannel[Int]()
      val f = Future(simplePipeline(ls)(outputStream))

      var outputSet = Set.empty[Int]

      for (_ <- (0 until 5)) do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => outputSet += v

      assertEquals(outputSet, ls.map(_ * 2 + 10).to(Set))
  }

  test("scaled pipeline being parallelized") {
    Async.blocking:
      val start = System.currentTimeMillis()
      val DELAY = 100

      def delayStage[T](stage: Stage[T], amount: Long): Stage[T] =
        (in, out) =>
          val connection = SyncChannel[T]()
          val f = Future(stage(connection, out))
          try
            var isOpen = true
            while isOpen do
              in.read() match
                case Left(_) => isOpen = false
                case Right(v) =>
                  AsyncOperations.sleep(amount)
                  connection.send(v)
          finally connection.close()
          f.await

      val myPipeline =
        (ls: List[Int]) =>
          Pipeline.generator(ls*) >>
            delayStage(Pipeline.applyBinOp[Int](_ * _, 2), DELAY) * ls.length >>
            Pipeline.applyBinOp[Int](_ + _, 10)

      val ls = List(1, 2, 3, 4, 5)
      val outputStream = SyncChannel[Int]()
      val f = Future(myPipeline(ls)(outputStream))

      var outputSet = Set.empty[Int]

      for (_ <- (0 until 5)) do
        outputStream.read() match
          case Left(_)  => fail("Unexpected end of stream")
          case Right(v) => outputSet += v

      assertEquals(outputSet, ls.map(_ * 2 + 10).to(Set))

      f.await

      val end = System.currentTimeMillis()
      assert(end - start < DELAY * 2)
  }
}
