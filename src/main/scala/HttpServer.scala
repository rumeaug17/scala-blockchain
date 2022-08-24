package org.rg.sbc

import java.time.LocalDateTime
import scala.collection.Seq
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import cask.util.Logger
import org.slf4j.LoggerFactory

import scala.collection.immutable.VectorMap

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

case class ServerInfo(
                     url: String,
                     dateTime: String,
                     description: String,
                     links: Map[String, String]
                     )
given RW[ServerInfo] = macroRW

case class UserList(
                   users: Seq[String]
                   )
given RW[UserList] = macroRW

object HttpServer extends cask.MainRoutes:
  override def port: Int = 8888
  val defaultDuration: FiniteDuration = 60.seconds

  private val system: ActorSystem[Order] =
    ActorSystem(BlockChainActor.init, "BlockChainActor")


  override def debugMode: Boolean = Settings.debugMode

  // Surcharge du logger par défaut de cask par le logger slf4j configuré
  override def log: Logger = new Logger:
    private val logger = LoggerFactory.getLogger(getClass)

    def exception(t: Throwable): Unit =
      logger.error(t.toString)

    def debug(t: sourcecode.Text[Any])(implicit f: sourcecode.File, line: sourcecode.Line): Unit =
      logger.debug(f.value.split('/').last + ":" + line + " " + t.source + " " + pprint.apply(t.value))


  // for ask pattern
  given timeout: Timeout = Timeout(defaultDuration)
  given scheduler: Scheduler = system.scheduler
  given ec: ExecutionContext = system.executionContext

  @cask.get("/")
  def hello(): String =
    // log with same system as akka ?
    val infos = ServerInfo(
      "http://localhost:8888/",
      LocalDateTime.now().toString,
      "simple http (rest api) server for sbc",
      VectorMap(
        "/full" -> "get full json blockchain state",
        "/users" -> "list of users with current balance",
        "/transactions" -> "list of validated transactions",
        "/balance/:user" -> "get balance amount for :user",
        "/transaction" -> "post a new transaction",
        "/mininig" -> "post a mining attempt"
      )
    )
    write(infos, indent = 2)

  //curl http://localhost:8888/full
  @cask.get("/full")
  def sendFullJsonChain(): String =
    val current: Future[String] = system.ask(ref => JsonChain(ref))
    Await.result(current, defaultDuration)

  //curl http://localhost:8888/balance/1
  @cask.get("/balance/:user")
  def getBalanceForUser(user : String): String =
    val current: Future[BlockChain] = system.ask(ref => FullChain(ref))
    val balance = current.map(c => c.globalBalanceFor(user))(using ec)
    write(Map(user -> Await.result(balance, defaultDuration)), indent = 2)

  @cask.get("/users")
  def listIOfUsers() : String =
    val current: Future[BlockChain] = system.ask(ref => FullChain(ref))
    val users = current.map(_.knownUsersWithBalance)(using ec)
    write(Await.result(users, defaultDuration), indent = 2)

  @cask.get("/transactions")
  def listIOfTransactions(): String =
    val current: Future[BlockChain] = system.ask(ref => FullChain(ref))
    val transactions = current.map(_.ChainedTransactions)(using ec)
    write(Await.result(transactions, defaultDuration), indent = 2)


  // curl http://localhost:8888/transaction -d "{\"transaction\": {\"sender\": \"1\", \"receiver\": \"2\", \"amount\": 1, \"description\": \"payment\"}}"
  @cask.postJson("/transaction")
  def addTransaction(transaction : Transaction): String =
    system ! transaction
    write(transaction)


  // curl http://localhost:8888/mining -d "{\"user\": \"1\"}"
  @cask.postJson("/mining")
  def mining(user : String): String =
    system ! Mining(user)
    write(Map("action" -> "mining", "who" -> user))

  log.debug("Http server for serving API sbt initialized")
  initialize()

end HttpServer
