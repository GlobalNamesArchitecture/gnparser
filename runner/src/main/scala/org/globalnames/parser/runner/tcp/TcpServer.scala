package org.globalnames
package parser
package runner
package tcp

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}

import scala.collection.immutable.Queue
import scala.concurrent.duration._

import ScientificNameParser.{instance => snp}
import GnParser.Config

object TcpServer {
  def run(config: Config): Unit = {
    implicit val system = ActorSystem("global-names-tcp-system")
    val server = system.actorOf(Props(new TcpServiceActor(config)),
                                name = "global-names-tcp-actor")

    val endpoint = new InetSocketAddress(config.host, config.port)
    implicit val bindingTimeout = Timeout(1.second)
    import system.dispatcher

    val boundFuture = IO(Tcp) ? Tcp.Bind(server, endpoint)

    boundFuture.onSuccess { case Tcp.Bound(address) =>
      println(
        s"""Bound Global Names TCP server to $address
           |Run `telnet ${config.host} ${config.port}`, type something and press RETURN
           |Type `STOP` to exit
           |""".stripMargin)
    }
  }
}

class TcpServiceActor(config: Config) extends Actor with ActorLogging {
  var childrenCount = 0

  def receive = {
    case Tcp.Connected(_, _) =>
      val tcpConnection = sender
      val newChild = context.watch(context.actorOf(Props(
        new TcpServiceConnection(tcpConnection, config))))
      childrenCount += 1
      sender ! Tcp.Register(newChild)
      log.debug("Registered for new connection")

    case Terminated(_) if childrenCount > 0 =>
      childrenCount -= 1
      log.debug("Connection handler stopped, another {} connections open",
                childrenCount)

    case Terminated(_) =>
      log.debug("Last connection handler stopped, shutting down")
      context.system.terminate()
  }
}

class TcpServiceConnection(tcpConnection: ActorRef, config: Config)
  extends Actor with ActorLogging {

  context.watch(tcpConnection)

  def receive = idle

  def idle: Receive = stopOnConnectionTermination orElse {
    case Tcp.Received(data) if data.utf8String.trim == "STOP" =>
      log.info("Shutting down")
      tcpConnection ! Tcp.Write(ByteString("Shutting down...\n"))
      tcpConnection ! Tcp.Close

    case Tcp.Received(data) =>
      val inputNames = data.utf8String.split("\n").map { _.trim }.filterNot { _.isEmpty }
      val result = if (inputNames.isEmpty) {
        "No name provided?\n"
      } else {
        val parsedNames = inputNames.map { name =>
          val result = snp.fromString(name)
          config.renderResult(result)
        }
        parsedNames.mkString("", "\n", "\n")
      }
      tcpConnection ! Tcp.Write(ByteString(result), ack = SentOk)
      context.become(waitingForAck)

    case x: Tcp.ConnectionClosed =>
      log.debug("Connection closed: {}", x)
      context.stop(self)
  }

  def waitingForAck: Receive = stopOnConnectionTermination orElse {
    case Tcp.Received(data) =>
      tcpConnection ! Tcp.SuspendReading
      context.become(waitingForAckWithQueuedData(Queue(data)))

    case SentOk =>
      context.become(idle)

    case x: Tcp.ConnectionClosed =>
      log.debug("Connection closed: {}, waiting for pending ACK", x)
      context.become(waitingForAckWithQueuedData(Queue.empty, closed = true))
  }

  def waitingForAckWithQueuedData(queuedData: Queue[ByteString],
                                  closed: Boolean = false): Receive =
    stopOnConnectionTermination orElse {
      case Tcp.Received(data) =>
        context.become(waitingForAckWithQueuedData(queuedData.enqueue(data)))

      case SentOk if queuedData.isEmpty && closed =>
        log.debug("No more pending ACKs, stopping")
        tcpConnection ! Tcp.Close
        context.stop(self)

      case SentOk if queuedData.isEmpty =>
        tcpConnection ! Tcp.ResumeReading
        context.become(idle)

      case SentOk =>
        tcpConnection ! Tcp.Write(queuedData.head, ack = SentOk)
        context.become(waitingForAckWithQueuedData(queuedData.tail, closed))

      case x: Tcp.ConnectionClosed =>
        log.debug("Connection closed: {}, waiting for completion of {} " +
                  "pending writes", x, queuedData.size)
        context.become(waitingForAckWithQueuedData(queuedData, closed = true))
    }

  def stopOnConnectionTermination: Receive = {
    case Terminated(`tcpConnection`) =>
      log.debug("TCP connection actor terminated, stopping...")
      context.stop(self)
  }

  object SentOk extends Tcp.Event
}
