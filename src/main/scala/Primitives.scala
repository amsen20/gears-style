import gears.async.Future.Promise
import gears.async.Async
import java.util.concurrent.locks.Condition
import scala.util.Success

// TODO test this
object Primitives:

  class GearsCondVar:
    private var hangers: List[Promise[Unit]] = Nil
    def gWait(using Async) =
      val p = Promise[Unit]()
      hangers = hangers.appended(p)
      Async.await(p.asFuture)

    def gNotifyOne =
      hangers match
        case Nil => false
        case p :: ps =>
          hangers = ps
          p.complete(Success(()))

    def gNotifyAll =
      hangers.foreach(_.complete(Success(())))
      hangers = Nil

@main def hello(): Unit =
  println("Hello world!")
  println(msg)

def msg = "I was compiled by Scala 3. :)"
