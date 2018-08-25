package org.globalnames
package parser
package runner
package tcp

import akka.actor.{ActorSystem, NoScopeGiven, Scope}
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import GnParser.Config
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scalaz.syntax.std.option._

class TcpServiceConnectionSpec(_system:ActorSystem) extends TestKit(_system)
 with WordSpecLike with Matchers with BeforeAndAfterAll {

  import TcpServiceConnection._

  def this() = this(ActorSystem("test"))

  val config: Config = {
    val address = "127.0.0.1"
    val port = 4356
    Config(
      format = GnParser.Format.Simple(delimiter = "|".some),
      host = address, port = port
    )
  }

  trait scoping extends Scope {
    val tcpProbe = TestProbe()
    val testRef = TestActorRef(new TcpServiceConnection(tcpProbe.ref, config))

    override def withFallback(other: Scope): Scope = NoScopeGiven
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A TcpServiceConnection" must {
    "handle empty message" in new scoping {
      testRef ! Tcp.Received(ByteString(""))
      tcpProbe.expectMsgPF() {
        case Tcp.Write(msg, _) =>
          msg.decodeString("utf-8") shouldBe NoNameProvidedMessage + "\n"
      }
    }

    "parse single name" in new scoping {
      testRef ! Tcp.Received(ByteString("Homo sapiens"))
      tcpProbe.expectMsgPF() {
        case Tcp.Write(msg, _) =>
          msg.decodeString("utf-8") shouldBe
            "16f235a0-e4a3-529c-9b83-bd15fe722110|Homo sapiens|Homo sapiens|Homo sapiens|||1\n"
      }
    }

    "parse multiple names" in new scoping {
      testRef ! Tcp.Received(ByteString("Homo sapiens\nSalinator solida"))
      tcpProbe.expectMsgPF() {
        case Tcp.Write(msg, _) =>
          msg.decodeString("utf-8") shouldBe
         """16f235a0-e4a3-529c-9b83-bd15fe722110|Homo sapiens|Homo sapiens|Homo sapiens|||1
           |da1a79e5-c16f-5ff7-a925-14c5c7ecdec5|Salinator solida|Salinator solida|Salinator solida|||1
           |""".stripMargin
      }
    }

    "filter empty name requests" in new scoping {
      testRef ! Tcp.Received(ByteString("\nHomo sapiens\n\n\nSalinator solida\n"))
      tcpProbe.expectMsgPF() {
        case Tcp.Write(msg, _) =>
          msg.decodeString("utf-8") shouldBe
            """16f235a0-e4a3-529c-9b83-bd15fe722110|Homo sapiens|Homo sapiens|Homo sapiens|||1
              |da1a79e5-c16f-5ff7-a925-14c5c7ecdec5|Salinator solida|Salinator solida|Salinator solida|||1
              |""".stripMargin
      }
    }
  }
}
