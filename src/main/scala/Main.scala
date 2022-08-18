package org.rg.sbc

object Main:
  @main def test(): Unit =
    val chain = BlockChain()
    val longest = chain.addTransaction(Transaction("bank", "me", 1000))
      .addTransaction(Transaction("me", "you", 1000))
      .addTransaction(Transaction("you", "him", 500))
      .miningBlock("me")
    val someChain = longest.toJsonString
    println(someChain)
