package org.rg.sbc

import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

import scala.util.{Try,Success,Failure}
import scala.collection.Seq
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors

import scala.io.Source

case class Mining(who : String) extends Order
case class JsonChain(replyTo: ActorRef[String]) extends Order
case class FullChain(replyTo: ActorRef[BlockChain]) extends Order
case class Resolve(other: BlockChain) extends Order

case object GracefulShutdown extends Order

object BlockChainActor :

  def init : Behavior[Order] = Behaviors.setup[Order] {
        context =>
          context.setLoggerName("org.rg.sbc")
          context.log.info("Starting up BlockChainActor")
          // TODO : load backuped blockchain
          val cfgPath = Settings.pathForBackupFile
          context.log.info(s"config file path for loading backuped blockchain : $cfgPath")
          val root = Try(Files.readString(Paths.get(cfgPath))) match
            case Success(source: String) =>
              context.log.info("found blockchain saved. Reload it.")
              context.log.debug(source)
              BlockChain(source)
            case Failure(exception) =>
              context.log.info(s"exception is $exception")
              context.log.info(s"no blockchain saved founded. New one.")
              BlockChain()

          apply(root)
  }

  def apply(root: BlockChain):  Behavior[Order] = Behaviors.receive {
    (context, message) => message match

      case GracefulShutdown =>
        context.log.info(s"receive shutdown order")
        val cfgPath = Settings.pathForBackupFile
        if cfgPath != "" then
          context.log.info(s"config file path for  backuping blockchain : $cfgPath")
          Files.write(Paths.get(cfgPath), root.toJsonString.getBytes(StandardCharsets.UTF_8))
        else
          context.log.info(s"no backup file, no saving : $cfgPath")

        Behaviors.stopped { () => () }

      case t: Transaction =>
        context.log.info(s"receive order : adding a tranaction : $t")
        BlockChainActor(root.addTransaction(t))

      case Mining(who) =>
        context.log.info(s"receive order : mining block from $who")
        BlockChainActor(root.miningBlock(who))

      case Resolve(other) =>
        context.log.info(s"receive order : resolve")
        if BlockChain.validateChain(other) then
          BlockChainActor(BlockChain.resolveConflicts(root)(Seq(other)))
        else
          Behaviors.same

      case JsonChain(replyTo) =>
        context.log.info(s"receive order : the string block chain ${root.toString}")
        replyTo ! root.toJsonString
        Behaviors.same

      case FullChain(replyTo) =>
        context.log.info(s"receive order : the block chain itself")
        replyTo ! root
        Behaviors.same
  }

end BlockChainActor

