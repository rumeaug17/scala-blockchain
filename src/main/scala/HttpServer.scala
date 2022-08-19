package org.rg.sbc

import scala.collection.Seq
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

// for ask pattern and await
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.Await
import concurrent.duration.DurationInt
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern.*

import upickle.default._
import upickle.default.{ReadWriter => RW, macroRW}
import upickle._

// an api server on front of the actor system
// https://com-lihaoyi.github.io/cask/index.html

object HttpServer extends cask.MainRoutes:
  override def port: Int = 8888
  val defaultDuration: FiniteDuration = 60.seconds

  private val system: ActorSystem[Order] =
    ActorSystem(BlockChainActor.init, "BlockChainActor")

  // for ask pattern
  given timeout: Timeout = Timeout(defaultDuration)
  given scheduler: Scheduler = system.scheduler
  given ec: ExecutionContext = system.executionContext

  @cask.get("/")
  def hello() =
    log.debug("init http server")
    "This is the HttpServer for BlockChainActor System. Have a nice trip on it"

  @cask.get("/full")
  def sendFullJsonChain() =
    val current: Future[String] = system.ask(ref => JsonChain(ref))
    Await.result(current, defaultDuration)

  initialize()

  @cask.get("/balance/:user")
  def getBalanceForUser(user : String) =
    val current: Future[BlockChain] = system.ask(ref => FullChain(ref))
    val balance = current.map(c => c.globalBalanceFor(user))(using ec)
    write(Map(user -> Await.result(balance, defaultDuration)))

  @cask.postJson("/transaction")
  def addTransaction(transaction : Transaction) =
    system ! transaction
    write(transaction)

  @cask.postJson("/mining")
  def mininig(user : String) =
    system ! Mining(user)
    write(Map("action" -> "mining", "who" -> user))

end HttpServer
