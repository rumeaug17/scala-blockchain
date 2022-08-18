package org.rg.sbc

import scala.collection.Seq

import akka.actor.typed.{ActorSystem, ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors

// for ask pattern and await
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import concurrent.duration.DurationInt
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern.*


object Main:
  @main def test(): Unit =
    val chain = BlockChain()
    val longest = chain.addTransaction(Transaction("bank", "me", 1000))
      .addTransaction(Transaction("me", "you", 1000))
      .addTransaction(Transaction("you", "him", 500))
      .miningBlock("me")
    val someChain = longest.toJsonString
    println(someChain)

  @main def TestWithAkka(): Unit =
    val system: ActorSystem[Order] =
      ActorSystem(BlockChainActor.init, "BlockChainActor")

    system ! Transaction("bank", "me", 1000)
    system ! Transaction("me", "you", 100)
    system ! Transaction("you", "him", 50)
    system ! Transaction("you", "her", 50)
    system ! Transaction("me", "her", 50)
    system ! Transaction("me", "him", 50)
    system ! Mining("me")
    system ! Mining("you")
    system ! Transaction("me", "you", 100)
    system ! Transaction("her", "me", 10)
    system ! Mining("me")

    // for ask pattern
    given timeout: Timeout = Timeout(600.seconds)
    given scheduler: Scheduler = system.scheduler
    given ec: ExecutionContext = system.executionContext

    val chain2: Future[String] = system.ask(ref => JsonChain(ref))
    chain2.map(b => println(s"*** $b"))

    val fullChain: Future[BlockChain] = system.ask(ref => FullChain(ref))
    fullChain.foreach(
        c =>
          println("Is chain valid? " + BlockChain.validateChain(c))
          println ("Global balance for me : " + c.globalBalanceFor("me"))
          println ("Global balance for you : " + c.globalBalanceFor("you"))
    )

    val nstr = """|{
        |  "chain": [
        |    {
        |      "predecessorHash": "1",
        |      "index": 1,
        |      "data": [
        |
        |      ],
        |      "dataHash": "26507fcb9517dc2c0f05aa3dd9c28f8b4031b900f4523463cc5aa0d12e32fee5",
        |      "timestamp": 1660835969068,
        |      "proof": "10000"
        |    },
        |    {
        |      "predecessorHash": "ef8662117d48ce9bf7fd59d1546a804efb97df5467c085421a3bca91c9878563",
        |      "index": 2,
        |      "data": [
        |        {
        |          "sender": "bank",
        |          "receiver": "me",
        |          "amount": 1000
        |        },
        |        {
        |          "sender": "me",
        |          "receiver": "you",
        |          "amount": 100
        |        },
        |        {
        |          "sender": "you",
        |          "receiver": "him",
        |          "amount": 50
        |        },
        |        {
        |          "sender": "you",
        |          "receiver": "her",
        |          "amount": 50
        |        },
        |        {
        |          "sender": "me",
        |          "receiver": "her",
        |          "amount": 50
        |        },
        |        {
        |          "sender": "me",
        |          "receiver": "him",
        |          "amount": 50
        |        },
        |        {
        |          "sender": "0",
        |          "receiver": "me",
        |          "amount": 1
        |        }
        |      ],
        |      "dataHash": "c20daf87068700d9922884b75f1910d7b85581b69ae4f6518e050e7414b1b496",
        |      "timestamp": 1660835970475,
        |      "proof": "252608"
        |    },
        |    {
        |      "predecessorHash": "ca687342b6280c9dc914cecf76aabb6fdaece99cf32b8bb21b48d7ebeac847ed",
        |      "index": 3,
        |      "data": [
        |        {
        |          "sender": "me",
        |          "receiver": "you",
        |          "amount": 100
        |        },
        |        {
        |          "sender": "her",
        |          "receiver": "me",
        |          "amount": 10
        |        },
        |        {
        |          "sender": "0",
        |          "receiver": "me",
        |          "amount": 1
        |        }
        |      ],
        |      "dataHash": "aef04daee5631977b3503a205802d7ef65375e87429193ade38d86f325a0fe88",
        |      "timestamp": 1660835970761,
        |      "proof": "97021"
        |    }
        |  ],
        |  "transactions": [
        |
        |  ]
        |}""".stripMargin

    val nbc = BlockChain(nstr)
    if BlockChain.validateChain(nbc) then
      system ! Resolve(nbc)
    else
      ()

    system.ask(ref => JsonChain(ref)).map(b => println(s"*** *** $b"))