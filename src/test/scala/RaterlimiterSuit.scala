import gears.async.Future
import gears.async.Async
import gears.async.AsyncOperations
import gears.async.default.given

import scala.concurrent.ExecutionContext

class RateLimiterSuite extends munit.FunSuite {
  given ExecutionContext = ExecutionContext.global

  test("wait rateLimiter") {
    Async.blocking:
      val limiter = RateLimiter.Per(10, 100)
      val start = System.currentTimeMillis()
      for _ <- 0 until 10 do limiter.Wait
      val probe = System.currentTimeMillis()
      assert(probe - start < 10)
      limiter.Wait
      val end = System.currentTimeMillis()
      assert(end - start > 100)
  }

  test("waitN rateLimiter") {
    Async.blocking:
      val limiter = RateLimiter.Every(10)
      val start = System.currentTimeMillis()
      limiter.WaitN(11)
      val end = System.currentTimeMillis()
      assert(end - start >= 100)
  }
}
