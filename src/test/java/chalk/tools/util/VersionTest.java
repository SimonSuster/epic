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

package chalk.tools.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import chalk.tools.util.Version;

/**
 * Tests for the {@link Version} class.
 */
public class VersionTest {

  @Test
  public void testParse() {
    Version referenceVersion = Version.currentVersion();
    assertEquals(referenceVersion, Version.parse(referenceVersion.toString()));
    
    assertEquals(new Version(1,5,2, false), Version.parse("1.5.2-incubating"));
    assertEquals(new Version(1,5,2, false), Version.parse("1.5.2"));
  }
  
  @Test
  public void testParseSnapshot() {
    assertEquals(new Version(1,5,2, true), Version.parse("1.5.2-incubating-SNAPSHOT"));
    assertEquals(new Version(1,5,2, true), Version.parse("1.5.2-SNAPSHOT"));
  }

  @Test
  public void testParseInvalidVersion() {
    try {
      Version.parse("1.5.");
    }
    catch (NumberFormatException e) {
      return;
    }

    assertTrue(false);
  }

  @Test
  public void testParseInvalidVersion2() {
    try {
      Version.parse("1.5");
    }
    catch (NumberFormatException e) {
      return;
    }

    assertTrue(false);
  }
}