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

package chalk.tools.tokenize;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;


import org.junit.Test;

import chalk.tools.cmdline.tokenizer.TokenEvaluationErrorListener;
import chalk.tools.tokenize.TokenSample;
import chalk.tools.tokenize.Tokenizer;
import chalk.tools.tokenize.TokenizerEvaluationMonitor;
import chalk.tools.tokenize.TokenizerEvaluator;
import chalk.tools.util.InvalidFormatException;
import chalk.tools.util.Span;

public class TokenizerEvaluatorTest {

  @Test
  public void testPositive() throws InvalidFormatException {
    OutputStream stream = new ByteArrayOutputStream();
    TokenizerEvaluationMonitor listener = new TokenEvaluationErrorListener(
        stream);

    TokenizerEvaluator eval = new TokenizerEvaluator(new DummyTokenizer(
        TokenSampleTest.createGoldSample()), listener);
    
    eval.evaluateSample(TokenSampleTest.createGoldSample());

    assertEquals(1.0, eval.getFMeasure().getFMeasure());

    assertEquals(0, stream.toString().length());
  }

  @Test
  public void testNegative() throws InvalidFormatException {
    OutputStream stream = new ByteArrayOutputStream();
    TokenizerEvaluationMonitor listener = new TokenEvaluationErrorListener(
        stream);

    TokenizerEvaluator eval = new TokenizerEvaluator(new DummyTokenizer(
        TokenSampleTest.createGoldSample()), listener);
    
    eval.evaluateSample(TokenSampleTest.createPredSample());

    assertEquals(.5d, eval.getFMeasure().getFMeasure(), .1d);

    assertNotSame(0, stream.toString().length());
  }

  /** a dummy tokenizer that always return something expected */
  class DummyTokenizer implements Tokenizer {

    private TokenSample sample;

    public DummyTokenizer(TokenSample sample) {
      this.sample = sample;
    }

    public String[] tokenize(String s) {
      return null;
    }

    public Span[] tokenizePos(String s) {
      return this.sample.getTokenSpans();
    }

  }

}
