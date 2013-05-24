package epic.parser
/*
 Copyright 2012 David Hall

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

import math._
import java.util.Arrays
import breeze.util.Index
import breeze.collection.mutable.TriangularArray
import breeze.numerics.logSum
import collection.mutable.BitSet
import collection.immutable
import collection.immutable.BitSet.BitSetN
import java.util
import epic.constraints.{ChartConstraints, LabeledSpanConstraints}
import breeze.linalg.{Counter, Counter2}

@SerialVersionUID(4)
class ParseChart[L](val index: Index[L],
                             // label => number of refinements
                             refinementsFor: Array[Int],
                             val length: Int,
                             sparsity: ChartConstraints[L]) extends Serializable {
  private val grammarSize = index.size

  final val top = new ChartScores(sparsity.top)
  final val bot = new ChartScores(sparsity.bot)

  /**
   * A ChartScores is just a triangular array whose entries are arrays. Admits efficient
   * iteration over "on" elements in an (i, j) index.
   * @author dlwh
   */
  final class ChartScores private[ParseChart](constraints: LabeledSpanConstraints[L]) {

    import ParseChart._

    /** (begin,end) -> label ->  refinement -> score */
    // fill in arrays for spans we might touch
    val score: TriangularArray[Array[Array[Double]]] = TriangularArray.tabulate(length+1){(begin, end) =>
      if(sparsity.isAllowedSpan(begin, end)) {
        new Array[Array[Double]](index.size)
      } else {
        null
      }
    }

    // now fill in components of touch spans
    for(b <- 0 until length; e <- b to length) {
      val arr = score(b, e)
      if(arr != null) {
        var l = 0
        while(l < grammarSize) {
          if(constraints.isAllowedLabeledSpan(b, e, l))
            arr(l) = mkGrammarVector(refinementsFor(l), zero)
          l += 1
        }
      }
    }


    /** (begin,end) -> which labels are on */
    val enteredLabels: Array[BitSet] = mkBitSetArray(TriangularArray.arraySize(length+1))
    /** (begin,end) -> label -> which refinements of label are on */
    val enteredRefinements: Array[Array[BitSet]] = mkRefinementArray(TriangularArray.arraySize(length+1),grammarSize)

    /** Same as labelScore */
    def apply(begin: Int, end: Int, label: Int, ref: Int) = labelScore(begin, end, label, ref)

    /**
     * Returns the score of this (label,refinement) pair over the span (begin,end)
     * @return
     */
    @inline def labelScore(begin: Int, end: Int, label: Int, ref: Int):Double = {
      val forSpan = score(begin, end)
      if (forSpan eq null) Double.NegativeInfinity
      else if (forSpan(label) eq null) Double.NegativeInfinity
      else forSpan(label)(ref)
    }

    @inline def labelScore(begin: Int, end: Int, parent: L, ref: Int):Double = {
      labelScore(begin, end, index(parent), ref)
    }

    def enteredLabelIndexes(begin: Int, end: Int): BitSet = {
      enteredLabels(TriangularArray.index(begin, end))
    }

    def isLabelEntered(begin: Int, end: Int, l: Int): Boolean = {
      enteredLabels(TriangularArray.index(begin,end)).contains(l)
    }

    def enteredLabelRefinements(begin: Int, end: Int, label: Int) = {
      enteredRefinements(TriangularArray.index(begin, end))(label)
    }

    def enteredLabelScores(begin: Int, end: Int) = {
      val scoreArray = score(begin, end)
      if(scoreArray eq null) Iterator.empty
      else enteredLabels(TriangularArray.index(begin, end)).iterator.map { i => (index.get(i), scoreArray(i))}
    }

    def decodedLabelScores(begin: Int, end: Int):Counter2[L,Int,Double] = {
      val scoreArray = score(begin, end)
      if(scoreArray eq null) Counter2()
      else {
        val ret = Counter2[L, Int, Double]()
        for(i <- enteredLabels(TriangularArray.index(begin, end))) {
          val l = index.get(i)
          for((v,s) <- scoreArray(i).zipWithIndex) {
            if(v != zero)
              ret(l,s) = v
          }
        }
        ret
      }
    }

    def decodedLabelScores(begin: Int, end: Int, label: Int):Counter[Int,Double] = {
      val scoreArray = score(begin, end)
      if(scoreArray == null || scoreArray(label) == null) Counter()
      else {
        val ret = Counter[Int, Double]()
        for((v,s) <- scoreArray(label).zipWithIndex) {
          if(v != zero)
            ret(s) = v
        }
        ret
      }
    }


    private def rawEnter(begin: Int, end: Int, parent: Int, ref: Int, w: Double) = {
      val arrx = score(begin, end)
      val arr = arrx(parent)
      val oldScore = arr(ref)
      arr(ref) = w
      oldScore
    }


    def enter(begin: Int, end: Int, parent: Int, ref: Int, w: Double) {
      val oldScore =  rawEnter(begin, end, parent, ref, w)

      if(oldScore == zero) {
        val index = TriangularArray.index(begin, end)
        updateExtents(index, parent, ref, begin, end)
      }
    }

   /**
     * Updates the extent arrays for a given set of refinements
     */
    private def updateExtents(index: Int, parent: Int, ref: Int, begin: Int, end: Int) {
      enteredLabels(index) += parent
      enteredRefinements(index)(parent) += ref

      rightMostBeginForEnd(end)(parent)(ref) = math.max(begin, rightMostBeginForEnd(end)(parent)(ref))
      leftMostBeginForEnd(end)(parent)(ref) = math.min(begin, leftMostBeginForEnd(end)(parent)(ref))
      rightMostEndForBegin(begin)(parent)(ref) = math.max(end, rightMostEndForBegin(begin)(parent)(ref))
      leftMostEndForBegin(begin)(parent)(ref) = math.min(end, leftMostEndForBegin(begin)(parent)(ref))

      coarseRightMostBeginForEnd(end)(parent) = math.max(begin, coarseRightMostBeginForEnd(end)(parent))
      coarseLeftMostBeginForEnd(end)(parent) = math.min(begin, coarseLeftMostBeginForEnd(end)(parent))
      coarseRightMostEndForBegin(begin)(parent) = math.max(end, coarseRightMostEndForBegin(begin)(parent))
      coarseLeftMostEndForBegin(begin)(parent) = math.min(end, coarseLeftMostEndForBegin(begin)(parent))
    }

    /** right most place a constituent with label l can begin and end at position i, for right > i. (begin)(sym)(ref) */
    val rightMostBeginForEnd: Array[Array[Array[Int]]] = Array.tabulate(length+1, grammarSize){ (i, l) =>
      val arr = new Array[Int](refinementsFor(l))
      Arrays.fill(arr, -1)
      arr
    }
    /** left most place a constituent with label l can begin and end at position i, for left < i. (begin)(sym)(ref) */
    val leftMostBeginForEnd = Array.tabulate[Array[Int]](length+1, grammarSize){ (i, l) =>
      val arr = new Array[Int](refinementsFor(l))
      Arrays.fill(arr, length + 1)
      arr
    }
    /** left most place a constituent with label l--which starts at position i--can end. (end)(sym)(ref) */
    val leftMostEndForBegin = Array.tabulate[Array[Int]](length+1, grammarSize){ (i, l) =>
      val arr = new Array[Int](refinementsFor(l))
      Arrays.fill(arr, length + 1)
      arr
    }
    /** right-most place a constituent with label l--which starts at position i--can end. (end)(sym)(ref) */
    val rightMostEndForBegin = Array.tabulate[Array[Int]](length+1, grammarSize) { (i, l) =>
      val arr = new Array[Int](refinementsFor(l))
      Arrays.fill(arr, -1)
      arr
    }

    /** right most place a left constituent with label l can begin and end at position i. (begin)(sym) */
    val coarseRightMostBeginForEnd = makeCoarseExtentArray(-1)
    /** left most place a left constituent with label l can begin and end at position i  (begin)(sym) */
    val coarseLeftMostBeginForEnd = makeCoarseExtentArray(length+1)
    /** left most place a right constituent with label l--which starts at position i--can end. (end)(sym) */
    val coarseLeftMostEndForBegin = makeCoarseExtentArray(length+1)
    /** right-most place a right constituent with label l--which starts at position i--can end. (end)(sym)*/
    val coarseRightMostEndForBegin = makeCoarseExtentArray(-1)

    def feasibleSpan(begin: Int, end: Int, b: Int, refB: Int, c: Int, refC: Int) = {
      if(leftMostEndForBegin(begin)(b) == null || rightMostBeginForEnd(end)(c) == null) {
        Range(0,0)
      } else {
        val narrowR = leftMostEndForBegin(begin)(b)(refB)
        val narrowL = rightMostBeginForEnd(end)(c)(refC)
        var split = math.max(narrowR, leftMostBeginForEnd(end)(c)(refC))
        val endSplit = math.min(rightMostEndForBegin(begin)(b)(refB), narrowL) + 1
        val canBuildThisRule = narrowR < end && narrowL >= narrowR && split <= narrowL && split < endSplit
        if(!canBuildThisRule)
          split = endSplit

        Range(split, endSplit)
      }
    }

    private def makeCoarseExtentArray(value: Int) = {
      val arr = Array.ofDim[Int](length + 1, grammarSize)
      for(arr1 <- arr) {
        Arrays.fill(arr1, value)
      }
      arr
    }

  }

  protected final def zero = Double.NegativeInfinity
}



object ParseChart {

  def apply[L](g: Index[L], refinements: Array[Int], length: Int) = logProb(g, refinements, length)

  @SerialVersionUID(1)
  trait Factory[+Chart[X]<:ParseChart[X]] extends Serializable {
    def apply[L](g: Index[L], refinements: Array[Int], length: Int):Chart[L] = {
      apply[L](g, refinements, length, ChartConstraints.noSparsity)
    }

    def apply[L](g: Index[L], refinements: Array[Int], length: Int, sparsity: ChartConstraints[L]):Chart[L]
  }

  object logProb extends Factory[ParseChart] {
    def apply[L](g: Index[L], refinements: Array[Int], length: Int, sparsity: ChartConstraints[L]) = {
      new ParseChart(g, refinements, length, sparsity)
    }
  }

  private def mkGrammarVector(grammarSize: Int, fill: Double) = {
    val arr = new Array[Double](grammarSize)
    Arrays.fill(arr, fill)
    arr
  }

  private def mkBitSetArray(grammarSize: Int) = {
    val arr = new Array[collection.mutable.BitSet](grammarSize)
    var i = 0
    while (i < arr.length) {
      arr(i) = new collection.mutable.BitSet()
      i += 1
    }
    arr
  }

  private def mkRefinementArray(length: Int, grammarSize: Int): Array[Array[BitSet]] = {
    val arr = new Array[Array[BitSet]](length)
    var i = 0
    while (i < arr.length) {
      arr(i) = mkBitSetArray(grammarSize)
      i += 1
    }
    arr
  }



}



