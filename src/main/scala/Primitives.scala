// import gears.async.Future.Promise
// import gears.async.Async
// import java.util.concurrent.locks.Condition
// import scala.util.Success
// import gears.async.Async.Source
// import gears.async.AsyncOperations

// TODO test this
// object Primitives:
//   Currently, I don't know how to implement CondVar using gears
//   class GearsCondVar:
//     private var hangers: List[Promise[Unit]] = Nil
//     def gWait(): Source[Unit] =
//       val p = Promise[Unit]()
//       hangers = hangers.appended(p)
//       p.asFuture.asInstanceOf[Source[Unit]]

//     def gNotifyOne =
//       hangers match
//         case Nil => false
//         case p :: ps =>
//           hangers = ps
//           p.complete(Success(()))

//     def gNotifyAll =
//       hangers.foreach(_.complete(Success(())))
//       hangers = Nil
