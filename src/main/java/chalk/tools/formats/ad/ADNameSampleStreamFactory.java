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

package chalk.tools.formats.ad;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import chalk.tools.cmdline.ArgumentParser;
import chalk.tools.cmdline.CmdLineUtil;
import chalk.tools.cmdline.StreamFactoryRegistry;
import chalk.tools.cmdline.ArgumentParser.OptionalParameter;
import chalk.tools.cmdline.ArgumentParser.ParameterDescription;
import chalk.tools.formats.LanguageSampleStreamFactory;
import chalk.tools.namefind.NameSample;
import chalk.tools.util.ObjectStream;
import chalk.tools.util.PlainTextByLineStream;


/**
 * A Factory to create a Arvores Deitadas NameSampleDataStream from the command line
 * utility.
 * <p>
 * <b>Note:</b> Do not use this class, internal use only!
 */
public class ADNameSampleStreamFactory extends LanguageSampleStreamFactory<NameSample> {

  interface Parameters {
    //all have to be repeated, because encoding is not optional,
    //according to the check if (encoding == null) { below (now removed)
    @ParameterDescription(valueName = "charsetName",
        description = "encoding for reading and writing text, if absent the system default is used.")
    Charset getEncoding();

    @ParameterDescription(valueName = "sampleData", description = "data to be used, usually a file name.")
    File getData();
    
    @ParameterDescription(valueName = "split", description = "if true all hyphenated tokens will be separated (default true)")
    @OptionalParameter(defaultValue = "true")
    Boolean getSplitHyphenatedTokens();

    @ParameterDescription(valueName = "language", description = "language which is being processed.")
    String getLang();
  }

  public static void registerFactory() {
    StreamFactoryRegistry.registerFactory(NameSample.class,
        "ad", new ADNameSampleStreamFactory(Parameters.class));
  }

  protected <P> ADNameSampleStreamFactory(Class<P> params) {
    super(params);
  }

  public ObjectStream<NameSample> create(String[] args) {

    Parameters params = ArgumentParser.parse(args, Parameters.class);

    language = params.getLang();

    FileInputStream sampleDataIn = CmdLineUtil.openInFile(params.getData());

    ObjectStream<String> lineStream = new PlainTextByLineStream(
        sampleDataIn.getChannel(), params.getEncoding());

    return new ADNameSampleStream(lineStream, params.getSplitHyphenatedTokens());
  }
}
