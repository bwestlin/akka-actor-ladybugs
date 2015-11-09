package ladybugs.http

import akka.actor._
import ladybugs.http.WSStats.{StatsUpdates, RegisterConnection, PublishStats}
import org.joda.time.{Seconds, DateTime}

import scala.concurrent.duration._


object WSStats {
  def props() = Props(classOf[WSStats])

  case class RegisterConnection(wsConnection: ActorRef)
  case class PublishStats()
  case class StatsUpdates(numConnections: Int, upTime: Int)
}

class WSStats extends Actor with ActorLogging {
  import context.dispatcher

  final val PublishInterval = 1000.milliseconds

  val schedule = context.system.scheduler.schedule(PublishInterval, PublishInterval, self, PublishStats())

  val start = DateTime.now

  def receive = default()

  def default(connections: Seq[ActorRef] = Nil): Receive = {
    case RegisterConnection(wsConnection) =>
      context.watch(wsConnection)
      context.become(default(connections :+ wsConnection))

    case Terminated(wsConnection) =>
      context.become(default(connections.filterNot(_ == wsConnection)))

    case PublishStats() =>
      context.system.eventStream.publish(StatsUpdates(
        numConnections = connections.size,
        upTime = Seconds.secondsBetween(start, DateTime.now).getSeconds
      ))
  }
}
