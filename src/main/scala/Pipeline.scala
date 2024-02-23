import gears.async.Channel
import gears.async.SyncChannel
import gears.async.Async
import scala.compiletime.ops.double
import gears.async.Future
import gears.async.ChannelMultiplexer
import scala.util.Try
import scala.util.Failure
import scala.util.Success

object Pipeline {
  def generator[T](values: T*)(using Async): (Channel[T], Future[Unit]) =
    val channel = SyncChannel[T]()

    val f = Future:
      try
        values.foreach(channel.send(_))
      finally
        channel.close()

    (channel, f)

  def applyBinOp[T](binOp: (T, T) => T, intStream: Channel[T], by: T)(using
      Async
  ): Channel[T] =
    val resultStream = SyncChannel[T]()

    Future:
      var isOpen = true
      while isOpen do
        intStream.read() match
          case Left(_)  => isOpen = false
          case Right(v) => resultStream.send(binOp(v, by))

      resultStream.close()

    resultStream

  type Stage[T] = Channel[T] => Channel[T]
  def scale[T](stage: Stage[T], num: Int)(using Async): Stage[T] =
    inputStream =>
      val proxyOutputStream = SyncChannel[Try[T]]()
      val multiplexer = ChannelMultiplexer[T]()
      multiplexer.addSubscriber(proxyOutputStream)

      val proxyInputStream = SyncChannel[T]()
      Future:
        var isOpen = true
        while isOpen do
          inputStream.read() match
            case Left(_)  => isOpen = false
            case Right(v) => proxyInputStream.send(v)

        proxyInputStream.close()
        // FIXME Uncommenting following code results in failure
        // multiplexer.close()

      (0 until num).foreach(_ =>
        Future:
          val stageOutputStream = stage(proxyInputStream)
          multiplexer.addPublisher(stageOutputStream)
      )

      val outputStream = SyncChannel[T]()

      Future:
        var isOpen = true
        while isOpen do
          proxyOutputStream.read() match
            case Left(_) | Right(Failure(_)) => isOpen = false
            case Right(Success(v))           => outputStream.send(v)

        outputStream.close()

      Future:
        multiplexer.run()

      outputStream
}
