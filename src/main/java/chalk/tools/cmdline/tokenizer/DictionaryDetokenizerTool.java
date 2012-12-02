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

package chalk.tools.cmdline.tokenizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import chalk.tools.cmdline.AbstractBasicCmdLineTool;
import chalk.tools.cmdline.CLI;
import chalk.tools.cmdline.CmdLineUtil;
import chalk.tools.cmdline.PerformanceMonitor;
import chalk.tools.tokenize.Detokenizer;
import chalk.tools.tokenize.DictionaryDetokenizer;
import chalk.tools.tokenize.WhitespaceTokenizer;
import chalk.tools.util.ObjectStream;
import chalk.tools.util.PlainTextByLineStream;


public final class DictionaryDetokenizerTool extends AbstractBasicCmdLineTool {

  public String getHelp() {
    return "Usage: " + CLI.CMD + " " + getName() + " detokenizerDictionary";
  }
  
  public void run(String[] args) {
    
    
    if (args.length != 1) {
      System.out.println(getHelp());
    } else {
    
      Detokenizer detokenizer = new DictionaryDetokenizer(
          new DetokenizationDictionaryLoader().load(new File(args[0])));

      ObjectStream<String> tokenizedLineStream =
        new PlainTextByLineStream(new InputStreamReader(System.in));

      PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
      perfMon.start();

      try {
        String tokenizedLine;
        while ((tokenizedLine = tokenizedLineStream.read()) != null) {

          // white space tokenize line
          String tokens[] = WhitespaceTokenizer.INSTANCE.tokenize(tokenizedLine);

          System.out.println(detokenizer.detokenize(tokens, null));

          perfMon.incrementCounter();
        }
      }
      catch (IOException e) {
        CmdLineUtil.handleStdinIoError(e);
      }

      perfMon.stopAndPrintFinalResult();
    }
  }
}
