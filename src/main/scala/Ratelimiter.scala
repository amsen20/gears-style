import scala.collection.mutable
import gears.async.Async
import gears.async.AsyncOperations
import RateLimiter.Limiter

trait RateLimiter {
  def Wait: AsyncOperations ?=> Async ?=> Unit
  def WaitN(n: Int): AsyncOperations ?=> Async ?=> Unit
}

object RateLimiter {
  private class Limiter(val expectedThroughput: Int, val period: Long)
      extends RateLimiter {
    var pastRequests: mutable.Queue[Long] = mutable.Queue.empty

    def relaxQueue: Unit =
      val now = System.currentTimeMillis()
      while pastRequests.nonEmpty && pastRequests.head < now - period do
        pastRequests.dequeue()

    def Wait =
      synchronized {
        relaxQueue
        while pastRequests.length >= expectedThroughput do
          val amount = period - (System.currentTimeMillis() - pastRequests.head)
          if amount > 0 then AsyncOperations.sleep(amount)

          relaxQueue

        pastRequests.enqueue(System.currentTimeMillis())
      }

    def WaitN(n: Int) =
      for _ <- 0 until n do Wait
  }

  def Every(durationMillis: Long): RateLimiter = new Limiter(1, durationMillis)
  def Per(n: Int, durationMillis: Long): RateLimiter =
    new Limiter(n, durationMillis)
  def PerSecond(n: Int): RateLimiter = Per(n, 1000)
}
