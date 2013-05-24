package epic.trees
/*
 Copyright 2012 David Hall

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/


case class Span(begin: Int, end: Int)  {
  require(begin <= end)

  def isEmpty = begin == end
  def nonEmpty = !isEmpty

  def length = end - begin

  def map[U](f: Int=>U) = Range(begin,end).map(f)

  def contains(pos: Int) = pos >= begin && pos < end

  /**
   * Returns true if this and other overlap but containment or equality does not hold.
   * @param other
   * @return
   */
  def crosses(other: Span) = (
    (begin < other.begin && end < other.end && end > other.begin)
    ||  (other.begin < begin && other.end < end && other.end > begin)
  )



  /**
  * Return true if this' range contains the other range.
  */
  def contains(other:Span) = {
    begin <= other.begin && end >= other.end
  }

  override def toString = s"Span($begin, $end)"
}
