/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chalk.tools.stemmer;

import chalk.tools.stemmer.PorterStemmer;
import junit.framework.TestCase;

public class PorterStemmerTest extends TestCase{

  private PorterStemmer stemmer = new PorterStemmer();

  public void testNotNull() {
    assertNotNull(stemmer);
  }

  public void testStemming() {
	assertEquals(stemmer.stem("deny"), "deni" );
	assertEquals(stemmer.stem("declining"), "declin" );
	assertEquals(stemmer.stem("diversity"), "divers" );
	assertEquals(stemmer.stem("divers"), "diver" );
	assertEquals(stemmer.stem("dental"), "dental" );
  }
}
