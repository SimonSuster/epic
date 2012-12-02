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


package chalk.tools.util.featuregen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import chalk.tools.util.InvalidFormatException;
import chalk.tools.util.featuregen.AdaptiveFeatureGenerator;
import chalk.tools.util.featuregen.AggregatedFeatureGenerator;
import chalk.tools.util.featuregen.GeneratorFactory;
import chalk.tools.util.featuregen.OutcomePriorFeatureGenerator;
import chalk.tools.util.featuregen.TokenFeatureGenerator;


public class GeneratorFactoryTest {

  @Test
  public void testCreationWihtSimpleDescriptor() throws Exception {
    InputStream generatorDescriptorIn = getClass().getResourceAsStream(
        "/chalk/tools/util/featuregen/TestFeatureGeneratorConfig.xml");
    
    // If this fails the generator descriptor could not be found
    // at the expected location
    Assert.assertNotNull(generatorDescriptorIn);

    Collection<String> expectedGenerators = new ArrayList<String>();
    expectedGenerators.add(OutcomePriorFeatureGenerator.class.getName());

    AggregatedFeatureGenerator aggregatedGenerator =
      (AggregatedFeatureGenerator) GeneratorFactory.create(generatorDescriptorIn, null);



    for (AdaptiveFeatureGenerator generator :
        aggregatedGenerator.getGenerators()) {

        expectedGenerators.remove(generator.getClass().getName());

        // if of kind which requires parameters check that
    }

    // If this fails not all expected generators were found and
    // removed from the expected generators collection
    Assert.assertEquals(0, expectedGenerators.size());
  }
  
  @Test
  public void testCreationWithCustomGenerator() throws Exception {
    InputStream generatorDescriptorIn = getClass().getResourceAsStream(
        "/chalk/tools/util/featuregen/CustomClassLoading.xml");
    
    // If this fails the generator descriptor could not be found
    // at the expected location
    Assert.assertNotNull(generatorDescriptorIn);
    
    AggregatedFeatureGenerator aggregatedGenerator =
      (AggregatedFeatureGenerator) GeneratorFactory.create(generatorDescriptorIn, null);
    
    Collection<AdaptiveFeatureGenerator> embeddedGenerator = aggregatedGenerator.getGenerators();
    
    Assert.assertEquals(1, embeddedGenerator.size());
    
    for (AdaptiveFeatureGenerator generator : embeddedGenerator) {
      Assert.assertEquals(TokenFeatureGenerator.class.getName(), generator.getClass().getName());
    }
  }
  
  /**
   * Tests the creation from a descriptor which contains an unkown element.
   * The creation should fail with an {@link InvalidFormatException}
   */
  @Test(expected = IOException.class)
  public void testCreationWithUnkownElement() throws IOException {
    InputStream descIn = getClass().getResourceAsStream(
        "/chalk/tools/util/featuregen/FeatureGeneratorConfigWithUnkownElement.xml");
    
    try {
      GeneratorFactory.create(descIn, null);
    }
    finally {
      descIn.close();
    }
  }
}