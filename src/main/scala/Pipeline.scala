import gears.async.Channel
import gears.async.SyncChannel
import gears.async.Async
import scala.compiletime.ops.double
import gears.async.Future
import gears.async.ChannelMultiplexer
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.collection.mutable
import gears.async.ReadableChannel
import gears.async.SendableChannel

object Pipeline {
  type Stage[T] = (ReadableChannel[T], SyncChannel[T]) => Async ?=> Unit
  type Generator[T] = SyncChannel[T] => Async ?=> Unit

  def generator[T](values: T*): Generator[T] =
    out =>
      try
        values.foreach(out.send(_))
      finally
        out.close()

  def applyBinOp[T](binOp: (T, T) => T, by: T): Stage[T] =
    (in, out) =>
      try
        var isOpen = true
        while isOpen do
          in.read() match
            case Left(_)  => isOpen = false
            case Right(v) => out.send(binOp(v, by))
      finally out.close()

  extension [T](stage: Stage[T])
    def >>(otherStage: Stage[T]): Stage[T] =
      (in, out) =>
        val connection = SyncChannel[T]()
        val f = Future(stage(in, connection))
        otherStage(connection, out)

        f.await

    def *(num: Int): Stage[T] =
      (in, out) =>
        try
          val (outs, stages) =
            (0 until num)
              .map: _ =>
                val out = SyncChannel[T]()
                (out, Future(stage(in, out)))
              .unzip
          var streams = mutable.Set(outs*)
          while !streams.isEmpty do
            Async.select(
              streams.toSeq.map(stream =>
                stream.readSource handle:
                  case Left(_)  => streams -= stream
                  case Right(v) => out.send(v)
              )*
            )

          stages.awaitAll
        finally out.close()

  extension [T](generator: Generator[T])
    def >>(stage: Stage[T]): Generator[T] =
      out =>
        val connection = SyncChannel[T]()
        val genFuture = Future(generator(connection))
        stage(connection, out)

        genFuture.await
}
