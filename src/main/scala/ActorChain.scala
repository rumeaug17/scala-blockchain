package org.rg.sbc

import scala.collection.Seq

import akka.actor.typed.{ActorSystem, ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors

case class Mining(who : String) extends Order
case class JsonChain(replyTo: ActorRef[String]) extends Order
case class FullChain(replyTo: ActorRef[BlockChain]) extends Order
case class Resolve(other: BlockChain) extends Order

object BlockChainActor :

  def init : Behavior[Order] = Behaviors.setup[Order] {
    context => apply(BlockChain())
  }

  def apply(root: BlockChain):  Behavior[Order] = Behaviors.receive {
    (context, message) => message match

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

