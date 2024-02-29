import gears.async.AsyncOperations
import gears.async.Async
import gears.async.ReadableChannel
import gears.async.BufferedChannel

object Tick:
  def after(duration: Long): AsyncOperations ?=> Async ?=> ReadableChannel[Unit] =
      val out = BufferedChannel[Unit](1)
      try
        AsyncOperations.sleep(duration)
        out.send(())
      finally out.close()

      out

  def every(duration: Long): AsyncOperations ?=> Async ?=> ReadableChannel[Unit] =
      val out = BufferedChannel[Unit](1)
      try
        while true
        do
          AsyncOperations.sleep(duration)
          out.send(())
      finally out.close()

      out