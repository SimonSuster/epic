package scalanlp.parser

import scalanlp.collection.mutable.OpenAddressHashArray
import collection.mutable.ArrayBuffer
import scalanlp.util.{Index, Encoder}
import scalala.tensor.Counter2
import scalanlp.trees._


/**
 * A minimalist grammar that encodes just enough information to get reachability.
 * That is, it has enough information to decide what parse trees for a given
 * sentence are admissible given a tagged sentence.
 *
 * @author dlwh
 */
@SerialVersionUID(1)
final class BaseGrammar[L] private (
                                   /** The "start" symbol. Usually "TOP" in this parser. */
                                     val root: L,
                                    /** Index for all symbols, including synthetic symbols from binarization */
                                    val labelIndex: Index[L],
                                    /** Index over all acceptable rules */
                                    val index: Index[Rule[L]],
                                    indexedRules: Array[Rule[Int]],
                                    binaryRulesByParent: Array[Array[Int]],
                                    unaryRulesByParent: Array[Array[Int]],
                                    binaryRulesByLeftChild: Array[Array[Int]],
                                    binaryRulesByRightChild: Array[Array[Int]],
                                    unaryRulesByChild: Array[Array[Int]]) extends Encoder[Rule[L]] with Serializable {
  def labelEncoder  = Encoder.fromIndex(labelIndex)

  // Accessors for properties of indexed rules
  /**
   * Returns the parent label index from the rule index
   * @param r
   * @return
   */
  def parent(r: Int):Int = indexedRules(r).parent
  /**
   * Returns the left child label index from the rule index
   * @param r
   * @return
   */
  def leftChild(r: Int): Int = indexedRules(r).asInstanceOf[BinaryRule[Int]].left
  /**
   * Returns the right child label index from the rule index
   * @param r
   * @return
   */
  def rightChild(r: Int): Int = indexedRules(r).asInstanceOf[BinaryRule[Int]].right
  /**
   * Returns the child label index from the (unary) rule index
   * @param r
   * @return
   */
  def child(r: Int): Int = indexedRules(r).asInstanceOf[UnaryRule[Int]].child

  // query by parent or child
  /**
   * Gives all binary rule indices with this parent
   * @param l
   * @return
   */
  def indexedBinaryRulesWithParent(l: Int) = binaryRulesByParent(l)
  /**
   * Gives all unary rule indices with this parent
   * @param l
   * @return
   */
  def indexedUnaryRulesWithParent(l: Int) = unaryRulesByParent(l)
  /**
   * Gives all unary rule indices with this child
   * @param l
   * @return
   */
  def indexedUnaryRulesWithChild(l: Int) = unaryRulesByChild(l)

  /**
   * gives all binary rule indices with this left child
   * @param b the left child index
   * @return
   */
  def indexedBinaryRulesWithLeftChild(b: Int) = binaryRulesByLeftChild(b)

  /**
   * gives all binary rule indices with this right child
   * @param c the right child index
   * @return
   */
  def indexedBinaryRulesWithRightChild(c: Int) = binaryRulesByRightChild(c)

  def prettyString = {
    val builder = new StringBuilder()
    builder ++= ("Root: " + root.toString + "\n")
//    builder ++= labelIndex.addString(builder, "Labels:\n", ", ", "\n\n")
    val labelStrings = labelIndex.map(_.toString).toIndexedSeq
    val startLength = labelStrings.view.map(_.length).max + 1
    val blocks = indexedRules.groupBy(_.parent)
    for( (parent,block) <- blocks) {
      var first = true
      for (r <- block) {
        if(!first)
          builder ++= (" "*startLength)
        else
          builder ++= labelStrings(parent).padTo(startLength, ' ')

        builder ++= "-> "

        r match {
          case UnaryRule(a, b) =>
            builder ++= labelStrings(b)
          case BinaryRule(a, b, c) =>
            builder ++= labelStrings(b)
            builder += ' '
            builder ++= labelStrings(c)
        }
        builder += '\n'

        first = false

      }
    }
    builder.toString()
  }
}

object BaseGrammar {
  /**
   * Builds a grammar just from some productions
   */
  def apply[L, W](root: L, productions: TraversableOnce[Rule[L]]): BaseGrammar[L] = {
    val index = Index[L]();
    val ruleIndex = Index[Rule[L]]()
    val lex = new ArrayBuffer[LexicalProduction[L, W]]()
    for(r <- productions) {
      index.index(r.parent);
      r.children.foreach(index.index(_))
      ruleIndex.index(r)
    }
    apply(root, index, ruleIndex)
  }

  /**
   * Given a bunch of counts, builds a grammar.
   * @param root the root label
   * @param binaries presumably counts of binary rules
   * @param unaries presumably counts of unary rules
   * @tparam L label type
   * @return a base grammar instance
   */
  def apply[L](root: L,
               binaries: Counter2[L, _<:Rule[L], _],
               unaries: Counter2[L, _ <: Rule[L], _]): BaseGrammar[L] = {
    apply(root, binaries.keysIterator.map(_._2) ++ unaries.keysIterator.map(_._2))
  }

  /**
   * Given the indices necessary to make a grammar, builds the other data structures
   * that enable fast access to parent/child information, etc.
   * @param root root label
   * @param labelIndex index of grammar symbols
   * @param ruleIndex index of rules
   * @return
   */
  def apply[L, W](root: L,
                  labelIndex: Index[L],
                  ruleIndex: Index[Rule[L]]):BaseGrammar[L] = {
    val indexedRules = for ( r <- ruleIndex.toArray) yield r match {
      case BinaryRule(a, b, c) => BinaryRule(labelIndex(a), labelIndex(b), labelIndex(c)):Rule[Int]
      case UnaryRule(a, b) => UnaryRule(labelIndex(a), labelIndex(b)):Rule[Int]
    }

    val binaryRuleTable = Array.fill(labelIndex.size)(new OpenAddressHashArray[Int](labelIndex.size * labelIndex.size, -1, 4))
    val unaryRuleTable = new OpenAddressHashArray[Int](labelIndex.size * labelIndex.size, -1)

    val binaryRulesByParent: Array[ArrayBuffer[Int]] = Array.fill(labelIndex.size)(new ArrayBuffer[Int]())
    val unaryRulesByParent = Array.fill(labelIndex.size)(new ArrayBuffer[Int]())
    val binaryRulesByLeftChild = Array.fill(labelIndex.size)(new ArrayBuffer[Int]())
    val binaryRulesByRightChild = Array.fill(labelIndex.size)(new ArrayBuffer[Int]())
    val unaryRulesByChild = Array.fill(labelIndex.size)(new ArrayBuffer[Int]())
    for ( (r, i) <- indexedRules.zipWithIndex) r match {
      case BinaryRule(p, l, rc) =>
        binaryRulesByParent(p) += i
        binaryRulesByLeftChild(l) += i
        binaryRulesByRightChild(rc) += i
        binaryRuleTable(p)(rc + labelIndex.size * l) = i
      case UnaryRule(p, c) =>
        unaryRulesByParent(p) += i
        unaryRulesByChild(c) += i
        unaryRuleTable(c + labelIndex.size * (p)) = i
    }

    new BaseGrammar(
      root,
      labelIndex,
      ruleIndex,
      indexedRules,
      binaryRulesByParent.map(_.toArray),
      unaryRulesByParent.map(_.toArray),
      binaryRulesByLeftChild.map(_.toArray),
      binaryRulesByRightChild.map(_.toArray),
      unaryRulesByChild.map(_.toArray))
  }
}

