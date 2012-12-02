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

package chalk.tools.formats;

import java.io.FileInputStream;

import chalk.tools.cmdline.ArgumentParser;
import chalk.tools.cmdline.CmdLineUtil;
import chalk.tools.cmdline.StreamFactoryRegistry;
import chalk.tools.cmdline.params.LanguageFormatParams;
import chalk.tools.coref.CorefSample;
import chalk.tools.coref.CorefSampleDataStream;
import chalk.tools.util.ObjectStream;
import chalk.tools.util.ParagraphStream;
import chalk.tools.util.PlainTextByLineStream;


public class CorefSampleStreamFactory extends LanguageSampleStreamFactory<CorefSample> {

  interface Parameters extends LanguageFormatParams {
  }
  
  protected CorefSampleStreamFactory() {
    super(Parameters.class);
  }

  public static void registerFactory() {
    StreamFactoryRegistry.registerFactory(CorefSample.class,
        StreamFactoryRegistry.DEFAULT_FORMAT, new CorefSampleStreamFactory());
  }
  
  public ObjectStream<CorefSample> create(String[] args) {
    Parameters params = ArgumentParser.parse(args, Parameters.class);

    language = params.getLang();

    CmdLineUtil.checkInputFile("Data", params.getData());
    FileInputStream sampleDataIn = CmdLineUtil.openInFile(params.getData());

    ObjectStream<String> lineStream = new ParagraphStream(new PlainTextByLineStream(sampleDataIn
        .getChannel(), params.getEncoding()));

    return new CorefSampleDataStream(lineStream);
  }
}
