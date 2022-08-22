package org.rg.sbc

import scala.annotation.tailrec
import scala.collection.Seq

import upickle.default._
import upickle.default.{ReadWriter => RW, macroRW}
import upickle._

trait Order
case class Transaction(sender: String, receiver: String, amount: Long) extends Order
given RW[Transaction] = macroRW

case class Block(
                  predecessorHash: String,
                  index: Long,
                  data: Seq[Transaction],
                  dataHash: String,
                  timestamp: Long,
                  proof: String
                )
given RW[Block] = macroRW

case class BlockChain(chain: Seq[Block], transactions: Seq[Transaction]):

  val ChainedTransactions: Seq[Transaction] =
    chain.flatMap(block => block.data)

  val knownUsers: Seq[String] =
   ChainedTransactions.flatMap(t => Set(t.sender, t.receiver)).distinct

  val knownUsersWithBalance: Map[String, Long] =
    knownUsers.map(user => (user, globalBalanceFor(user))).toMap

  def globalBalanceFor(him : String): Long =
    ChainedTransactions.collect{case t if t.receiver == him => t.amount}.sum -
      ChainedTransactions.collect{case t if t.sender == him => t.amount}.sum

  def addTransaction(t: Transaction): BlockChain =
    this.copy(transactions = transactions :+ t)

  def addBlock(proof: String, previousHash: Option[String] = None): BlockChain =
    val predecessorHash = previousHash.getOrElse(hash(chain.last))
    val dh = hash(transactions)
    val block = Block(predecessorHash, chain.length + 1, transactions, dh, System.currentTimeMillis(), proof)
    BlockChain(chain = chain :+ block, transactions = Seq())

  /**
   * All the difficulty is here :  hash, and proof
   * we can have blockchains without proof, but we always need hash
   */
  def sha256Hash(text: String): String =
    String.format("%064x", new java.math.BigInteger(
      1,
      java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"))))

  def hash(block: Block): String =
    sha256Hash(block.toString)

  def hash(transactions: Seq[Transaction]): String =
    sha256Hash(transactions.toString)

  // find the first number proof that is valid with validateProof
  def proofOfWork(lastProof: String): String =
    LazyList.from(0).filter(proof => validateProof(lastProof, proof.toString)).head.toString

  // a proof is valid if hash(lastProof, proof) contains 4 leading zeroes
  def validateProof(lastProof: String, proof: String): Boolean =
    val guess = s"$lastProof$proof"
    val guessHash = sha256Hash(guess)
    guessHash.endsWith("00000")

  def validateBlock(block: Block, previous: Block): Boolean =
    block.dataHash == hash(block.data) && block.predecessorHash == hash(previous) && validateProof(previous.proof, block.proof)

  def miningBlock(receiver: String): BlockChain =
    if transactions.isEmpty then
      this
    else
      val lastBlock = chain.last
      val lastProof = lastBlock.proof

      // run the proof of work to get the next proof of the chain
      val proof = proofOfWork(lastProof)
      // add a transaction for paying
      val bc = addTransaction(Transaction("0", receiver, 1))
      // forge the new block by adding it to the chain
      bc.addBlock(proof)

  val toJsonString : String =
    write(this, indent = 2)

end BlockChain
given RW[BlockChain] = macroRW

object BlockChain:
  // create the genesis block
  def apply(): BlockChain = BlockChain(Seq(), Seq()).addBlock("100000", Some("1"))

  // create from Json
  def apply(jsonString: String): BlockChain =
    read[BlockChain](jsonString)

  def validateChain(bc: BlockChain): Boolean =
    @tailrec
    def validateChain(chain: Seq[Block]): Boolean =
      if chain.length > 1
      then
        val (lastBlock, restOfBlock) = (chain.last, chain.init)
        bc.validateBlock(lastBlock, restOfBlock.last) && validateChain(restOfBlock)
      else true
    validateChain(bc.chain)

  /**
   * Replacing our chain with the longest one in the network
   */
  def resolveConflicts(my: BlockChain)(neighbours: Seq[BlockChain]): BlockChain =
    val longestAndValid = neighbours.view.filter(bc => validateChain(bc)).maxBy(_.chain.length)
    if my.chain.length >= longestAndValid.chain.length
    then
      my
    else
      my.copy(chain = longestAndValid.chain)
