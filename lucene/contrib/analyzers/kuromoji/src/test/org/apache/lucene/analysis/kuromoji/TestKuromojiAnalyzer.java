package org.apache.lucene.analysis.kuromoji;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.kuromoji.KuromojiTokenizer.Mode;

/**
 * Test Kuromoji Japanese morphological analyzer
 */
public class TestKuromojiAnalyzer extends BaseTokenStreamTestCase {
  /** This test fails with NPE when the 
   * stopwords file is missing in classpath */
  public void testResourcesAvailable() {
    new KuromojiAnalyzer(TEST_VERSION_CURRENT);
  }
  
  /**
   * An example sentence, test removal of particles, etc by POS,
   * lemmatization with the basic form, and that position increments
   * and offsets are correct.
   */
  public void testBasics() throws IOException {
    assertAnalyzesTo(new KuromojiAnalyzer(TEST_VERSION_CURRENT), "多くの学生が試験に落ちた。",
        new String[] { "多く", "学生", "試験", "落ちる" },
        new int[] { 0, 3, 6,  9 },
        new int[] { 2, 5, 8, 11 },
        new int[] { 1, 2, 2,  2 }
      );
  }

  /**
   * Test that search mode is enabled and working by default
   */
  public void testDecomposition() throws IOException {

    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, null, Mode.SEARCH,
                                            KuromojiAnalyzer.getDefaultStopSet(),
                                            KuromojiAnalyzer.getDefaultStopTags());

    // Senior software engineer:
    assertAnalyzesToPositions(a, "シニアソフトウェアエンジニア",
                              new String[] { "シニア",
                                             "シニアソフトウェアエンジニア", // zero pos inc
                                             "ソフトウェア",
                                             "エンジニア" },
                              new int[] { 1, 0, 1, 1},
                              new int[] { 1, 3, 1, 1}
                              );

    // Senior project manager: also tests katakana spelling variation stemming
    assertAnalyzesToPositions(a, "シニアプロジェクトマネージャー",
                              new String[] { "シニア",
                                              "シニアプロジェクトマネージャ", // trailing ー removed by stemming, zero pos inc
                                              "プロジェクト",
                                              "マネージャ"}, // trailing ー removed by stemming
                              new int[]{1, 0, 1, 1},
                              new int[]{1, 3, 1, 1}
                              );

    // Kansai International Airport:
    assertAnalyzesToPositions(a, "関西国際空港",
                              new String[] { "関西",
                                             "関西国際空港", // zero pos inc
                                             "国際",
                                             "空港" },
                              new int[] {1, 0, 1, 1},
                              new int[] {1, 3, 1, 1}
                              );

    // Konika Minolta Holdings; not quite the right
    // segmentation (see LUCENE-3726):
    assertAnalyzesToPositions(a, "コニカミノルタホールディングス",
                              new String[] { "コニカ",
                                             "コニカミノルタホールディングス", // zero pos inc
                                             "ミノルタ", 
                                             "ホールディングス"},
                              new int[] {1, 0, 1, 1},
                              new int[] {1, 3, 1, 1}
                              );

    // Narita Airport
    assertAnalyzesToPositions(a, "成田空港",
                              new String[] { "成田",
                                             "成田空港",
                                             "空港" },
                              new int[] {1, 0, 1},
                              new int[] {1, 2, 1}
                              );

    // Kyoto University Baseball Club
    assertAnalyzesToPositions(new KuromojiAnalyzer(TEST_VERSION_CURRENT), "京都大学硬式野球部",
                     new String[] { "京都大",
                                    "学",
                                    "硬式",
                                    "野球",
                                    "部" },
                              new int[] {1, 1, 1, 1, 1},
                              new int[] {1, 1, 1, 1, 1});
    // toDotFile(a, "成田空港", "/mnt/scratch/out.dot");
  }

  
  /**
   * blast random strings against the analyzer
   */
  public void testRandom() throws IOException {
    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, null, Mode.SEARCH,
                                            KuromojiAnalyzer.getDefaultStopSet(),
                                            KuromojiAnalyzer.getDefaultStopTags());
    checkRandomData(random, a, atLeast(10000));
  }
  
  /** blast some random large strings through the analyzer */
  public void testRandomHugeStrings() throws Exception {
    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, null, Mode.SEARCH,
        KuromojiAnalyzer.getDefaultStopSet(),
        KuromojiAnalyzer.getDefaultStopTags());
    checkRandomData(random, a, 200*RANDOM_MULTIPLIER, 8192);
  }

  // Copied from TestKuromojiTokenizer, to make sure passing
  // user dict to analyzer works:
  public void testUserDict3() throws Exception {
    // Test entry that breaks into multiple tokens:
    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, TestKuromojiTokenizer.readDict(),
                                            Mode.SEARCH,
                                            KuromojiAnalyzer.getDefaultStopSet(),
                                            KuromojiAnalyzer.getDefaultStopTags());
    assertTokenStreamContents(a.tokenStream("foo", new StringReader("abcd")),
                              new String[] { "a", "b", "cd"  },
                              new int[] { 0, 1, 2 },
                              new int[] { 1, 2, 4 },
                              new Integer(4)
    );
  }

  // LUCENE-3897: this string (found by running all jawiki
  // XML through KuromojiAnalyzer) caused AIOOBE
  public void testCuriousString() throws Exception {
    final String s = "&lt;li&gt;06:26 2004年3月21日 [[利用者:Kzhr|Kzhr]] &quot;お菓子な家族&quot; を削除しました &lt;em&gt;&lt;nowiki&gt;(即時削除: 悪戯。内容: &amp;#39;ＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫＫ&amp;#39;)&lt;/nowiki&gt;&lt;/em&gt;&lt;/li&gt;";
    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, null, Mode.SEARCH,
                                            KuromojiAnalyzer.getDefaultStopSet(),
                                            KuromojiAnalyzer.getDefaultStopTags());
    checkAnalysisConsistency(random, a, random.nextBoolean(), s);
  }

  // LUCENE-3897: this string (found by
  // testHugeRandomStrings) tripped assert
  public void testAnotherCuriousString() throws Exception {
    final String s = "《〔〘〝」〩〄〯』〴〷〦〯〹】〰。　〆。〡〢〲〆〤〫〱　〜々〲〿〄》〃】〚〗〪〓〨々〮〹〟〯〫』」〨〒〜〃〃〡　〸〜〱〆〿「〱〳。〷〆〃〷〇〛〥〒〖〪〕〦〚〉〷〼〣〒。〕〣〻〒〻〼〔〸〿〖〖〆々〭《〟〚〇〕〸〲〄〿〙」〞〖〪〬〤【〵〘〃々〦〮〠〦〛〲〝〿〽〓〺〷〛》〛『》〇　〽〄〱〙〥〠』〨〉〨〔」》〮〥〽〔〰〄〶】〠〶〨〔々『。〞〙〮》【　〯〦〯〩〩〈〿〫〘〒》』〾〰〰〼〒「〝〰〱〞〹〔〪〭、〬〴【』〧〩】〈。〧〤〢〨〶〄〴〡。〪〭〞〷〣〘〳〄〬〙『　「」【〮〯〔〱〬〴〵〭〬〚〱、〚〣、〚〓〮、〚々】〼〿〦〫〛〲〆〕々。〨〩〇〫〵『『〣〮〜〫〃】〡〯』〆〫〺〻〬〺、〗】〓〕〶〇〞〬。」〃〮〇〞〷〰〲】〆〻。〬〻〄〜〃〲〺〧〘〇〈、〃〚〇〉「〬〣〨〮〆〴〻〒〖〄〒〳〗〶、〙「　〫〚《〩〆〱〡【〶』【〆〫】〢》〔。〵〴〽々〱〖〳〶〱《〈〒』『〝〘【〈〢〝〠〣「〤〆〢〈〚〕〿〣々〢〹〉〡　〷《〤〴『々〉〤〬《』々〾〔〚〆〔〴〪〩〸〦』〉〃　《〼〇〆〾〛〿」〧〝〽〘〠〻【〰〨〥《〯〝〩〩〱〇〳々〚〉〔『〹〳〳』〲『〣」〯〓【々〮〥〃〿〳〞〦〦〶〓〬〛〬〈〈〠『〜〥〒〯〜〜〹〲【〓〪《々〗〚〇〜〄〦『々〃〒〇〖〢〉〹〮〩〽『》〵〔】〣〮】〧、〇〰〒】《〈〆々〾〣【〾〲〘〧『〇〲〼〕〙「〪〆〚々〦〯〵〇〤〆〡」〪》〼』〴〶〪】『〲〢〭〬〈〠〮〽〓〔〧〖」〃〴〬』〣〝〯〣〴『〉〖〄〇〄〰〇〃〤、〤》〔〴〯〫〠〝〷〞〩〛〛〳々〓〟〜〛〜〃　〃〛「、』》》々〢〱〢〸〹〙〃〶〇〮〼」〔〶【〙〮々〣　〵〱〈〡〙〹、〶〘【〘〄〔『〸〵〫〱〈〙〜〸〩〗〷》〽〃〔〕〡〨〆〺〒〧〴〢〈〯〶〼〚〈〪〘〢〘〶〿〾〹〆〉」〠〴〭〉〡〮〫〸〸〦〟〣」〩〶』《〔〨〫〉〃〚〈〡〾〈〵【〼《〴〸〜〜〓《〡〶〫〉〫〼〱〿〢々〩〡〘〓〛〞〖々〢〩「々〦〣】〤〫〼〚〴〡〠〕〴〭。〟「〞》』「、〛〕〤々〈〺〃〸】〶〽〒〓〙》〶〬〸〧〜〲〬〰〪。〞〒【〭〇〢〝〧〰〹〾》〖「〹」〶〕〜〘〿〩〙〺〡〓〆〵〪〬〨〷〯〃】〤〤〞〸》〈〹〖〲〣〬〲〯〗〉〮「〼〨〓々。〭〆〶〩【〦〿》〩〻〢〔〤〟〯【〷〻〚〟」〗《〓〛。〰〃〭〯〘〣》〩〩〆」【〼〡】〳〿〫〳〼〺〶『〟〧』〳〲〔『〦「〳〃〫〷《〟〶〻〪〆〗〲〮〄〨〻』〟〜〓〣〴〓〉、〷〄〝〭〻〲〽〼〥〒〚〬〙〦〓〢〦〒〄。〛〩〿〹「〶〬〖〬〾〭〽〕〲〤〕〚〢〪〸〠〸〠〓〇〄〽〖】〵〮〦〲〸〉〫〢〹〼〗〱〮〢」〝〽〹「〭〥「〠〆〕〃〫々【『〣〝々〧〒〒】〬〖〘〗〰〭〢〠〨〖〶〒》〪〺〇〡》〦〝〾〴〸〓〛〟〞」〓〜。〡』々》〃〼』〨〾】〜〵々〥【〉〾〭〹〯〔〢〺〳〹〜〢〄〵〵〱。〯〹〺〣〭〉〛々〧〫々〛〪。〠〰〖〒〦〠〩〣〾〺〫〬、》「〚〫〲〸〶〧〞〯〨」】【〚〲『〽〡》〘〣〒〕〸『〼〘〿〘〽〤〿〶〫〆〾〔〃〱〫〱〧、　〒〰。〜〸〇〜〔〉〡〬〿〝〼〉〷、〠〘〉』〥〫〧〕》》〡〻〨〲〔〠〮】〰〮」〧〬《〦〼〽〵〭「〷〮〈〴〔〭、〣〔〥〱〔」〄〘》〡〣》〴〙〜〖〬〺〯々〟〗〥〥【〝〨〝〽〼〚。〙』〤〬〞〜〣〮〬〳〽〦〩　〶」〠〄〳〠〇〜〒〶〱々〠『〡』〭〰》〴〉〫〬〒《〽『〉〳〵〄〨〮〔〭〞』〡〚〩〦、〠【〓〯〬〦〛〽〉〜〻『〗〫〞〩〃〼〿〡〕〯　〸』》〼〮〆」〼〪〇〭〣〗〓〻〧」〙〳〱〥〳、〓〕〮〫》〧〃《〣』〹〬〣〶〡〾〙〮〕〶〧《　〨〇〺〳〉《《〕〜〰〱〕〛「〞〩　〓〢〄〣〼〢〽〇〛〟〖〘〳〤〫〡〫〬〦〘〪〶〝《』〜〕〝】〄〡〳〹々〯【〝〝〇〔〹〿〥〄〚〒〻『〺〮〇〲〒〾〙〞〉】〉〪〫〴〒〔〨〮〰〻〷〿〥〮〼〹〩〱〞〈〴〦〰〞【〻〾、〵〻〛〮、〻《〘〱〫〾〄〄〙、〔〖〘　》〻〧〦〃〣〬〰〗】〸〵。〄。〷〄〸〟〰〓、【〖〰〢〾〘〆》〜〶〻》〔〛』〦〩〷〴〃〴〫〱《「〖々〖〒〡〞。〱〡〖〤〫〇〜〒〴〯》〪〶》〘〨》》【〵〹」〤〯《〦〶〯〃〧〙〩」「〤】》々〣〱〯〞〰〢々〵〷〺〾〺〜〜〚〣〿〩〰《〄『〧《〜　〷〓〺〦々〚〨「〧〮《〥〸〞【〡〩〩〱〴〗〙〿『〇〭〖　〹〥〲『〗〛〯〷〃〽〝《〳《〡】〄々〱〆〯〦。〒『〡》〨〃〦」〬〄〬〔〭〫〼〲】』〗〔〼〴〹〠」〺〬〺〔々》〾〿「〺〖〤》〴〶〣〚〒【〤〄】〹〺〟〃〜、】〪〚〯〢〹、〶〖〭】〾〠」〉〆〾々〯〈〙〞〶〩】〺〟〫〽〫〸〵〛〙〃「〰〫〓』〻「〦〤〖〺〇。〨〟　〦〙〘〨〸〒〣〈〩〜〧〾〒〕〤　〇〴〮〝〈〿〢〴〟〷〭〴】〽〇〟〦〬〶〲。〫〸〮〝〆〸〄〣〦〲〢〇〫〻〹〕〶〥〖。〨〬【〥〽〓〵〯、〒〉〳〘〧〼〆〹〉〾〬〽】〹〲《〜〨〟〡〪〱〃〓〬〜〧〝〸、〢〝〦』〝〸》】〩〡〉〫〛〇〢〖〔〠〹〧〕〨〃〙「〲〗〙『〛。。《〸〔〾〧〉〠』〡〼〄〨〲〥〼〠〻　「〸〩〟〷【〮〜〧〿〾〜〈。〣〰〪〘〮〴〨【〩〜〟〟〼〻〦〝「〺〝〄〵〝〲〃〨〺〫〜　〮」〡』〜〿、〪々〕〫〃〒〔〛〻〲〹》【〚〣〯《〢〙〕〝〾〙〭〄〕〗〄〪〵〃〘〺〻〤〟〢〻〆〥〝】〠〬〧〾〮々〪〓』〷』〿〕〒〽、〷〉」〨〨　〄〽〾「〧〴〜〢〮〚〆〣《〬〺〟〥〼〛〆〓〚々〇々〈〉〗〨〳々〣〭〯〉【〩〮〺〪』〭〚〉〦、〃〘〦〮」〴〆〴〔〴〜「〠〴【〰「〫〳〟〾〶〉〨〲〚〩〷〄《〄〝〈。〧〟〳〃〹々〃〄〭〬〰、〥〬〸〱〉〩〴《〔【〠〳〪〧〫〽〓〭】〧」〮〒〸〤。〩』〭〖〛〭〯〨〕〞〮〞〬〹〺々〽〡〷〪〶「〹〯〝々〭〠〼〰「〒〉」」〡〆〜〾〪〾》〇〙『〚〿〽】〛〮〶〚」《〔〔〣、〄〗〩〭〠」〠〰〞《〸〧〺〰」『〾〯〃〓〓〩〣〚【〜〭　〝〨〗〷〒《〫〝〶〘〣〿〜〱〾〨〥〘〃〳〆〇〈〜〲〪〡〶〭〤『〝〖〷〦〾〬〟〠〳〻、」【〣『〺〞〴〳「〵〺〨々〩〰〢〧〣〃『〹」〉〓〘〦〣〄〕〞〵〧〜」〴〠〱》〮〬〄〶〆〬」〘　〺。〲。　〾〷〕〛〣〾〗、〭』〭〧〝。〮」々『〻〒〣「〳〩〪〝〒〥〻〘〰〼〭〆〷〭「」〚〔〬〃〝〮〩〪〽〱。〯〯〰〨〿〷「々》【〴〧〻〰、〶〡〹〩〡〺〲〼。〩〿〯』〟〴〼〦〤〙〢〩〔〲〆〗〲《〟〤〬〷〧〫〧〗〞〣〚〚〧〭〮〛〲〮々〩〩〕〬々　〥〸、〢。〿〵〺〤〲〝　〥》々〰」〮〩〛〛』々『〹〞〃〃々〚【〱。。〹〨〿〻〣〞〨〈〤〼〃〻〩〶『　〲〷〗〭〓〯〯〝〃〾〕〻〖〱《「〹〣〦　」〵〄〮〚〖〞〪〼〖〙〵》〰〃〘。【〺〖〄〪〝〭〆〬〚〬〨〽」〕」「〜〤〯〷〇〝〠〆〫〼」〭〤〓〔々〆〵〷〪〭「〆〖〇〽〄〄「〿〵〷〤〿〮〫〻〢〕〝〪〳〸〘〡〡〞〮〻】「〝〷〘〾〒〺〉〨〰』〳〓〃〒〪【〗〯「〧々〷〩〝』〭〇〒、〯〈〦〣〆〬〸〚〈〉〔〥《々》〹〢〺〤〝々《　〲〘】〚』〚『〯〼〾〱〵〻、〪〟〸〯〽〴〱。〵〪〫々〳〢〣〕〓〩「〘〜〨〻】〿〹〭〛〛〔〹〻。〛〴〤〢〮、〸〷〃〜〜〝〔』〳〮〹』〽　〶〛　〤」〢。〣〖〶〯〥》〢〸〸〤〕〣〘》〧〦」〘〻〶〾〮〢〳〝〙〻〦〺〇〲〢〔〘〶〩〖】〟〓〰〇〮』〦〄々〹〻〄〄〽〷〱〫〒〛〉〿〓〯〺〪〲〢〼、〫〬「〩《〡〕〻』〭〜〗〫】　、〈〙〉【〓〣〫〜〈『〾】〴〪〫〬〶〪〚〬〿〪〮〴〒〶〡〄〉〿〼〜〵』〻〼〢「〵。』〸〖〙〧』〾〖〙《〉〪〦〙〔〈　〤〫〦〸『〗「〣『〓『〡〨〖〥〭《〢〠〦〞〸〞〚〢〕〙〖〾、〩」「〗〈〰〸〤〴〶〤〙《々〆〽〆【『〬〝〸『〙、〪〻』〓〹々〥〲〉〪〹〫〓〽〪〩〷、〹〺〩「〞『】〡々〡「〇〉〺〶〾〔々、〾〻〪〣〖〡〩〥〾〯】〤〰》『〲【〙〭〽〛〿々〟　〢〃〼〕〫〲』〪【〛〯】〔〕〥』」〳』《〖〥〳〄〢【〩〮〫〥〝〯〿〟、〣〹〪〔〱」〖〢〘〛〾〾〜〒〝〷〚〳〣〝〟《】「》〻『〢〄〄　》〱〓〞〛〢〆〺〉《〃〭〙〻〞〷〩〹〥〦〫〞〄〇〯〽〱〼〴〾〕〸〿〱〪〨〟〠々〪〸〔〵〆」〔〖〴〝〟】《〥　』《〒〄〣〿〞》】〃〹〲〛〬。】〒〓〹〴〿〥〴〲〖〧〝〪〶「〕〔〞〜〸〬〒〽【〸〻〢【〱」〪〉　〉〘〪〻〴〞』〯〰〾〥〓〼〻〕』〠〃〟〩〛〔【〻〡「〘〔　〲々〻〚〈〪〱〾〷〗》〯〞【〩　『〕〪〈々〞〞〳〘〵〃〼〨々〇〞〈〹〧〢〃〢〮〆〈〤〘〬〟〽〩、。〲々〺〠〳〸　〸〹〥、〯〒〈〃〠〰〙〪〯〬〖」〔〹〔〘〶〾〨〿〛〈〡〯〕〶〲、〷【》〷〆》〄《】〒〓〔〼〉〒〢〄〢〓〩〰〃〔。〵〙』。〷〼〩」〒〒〇〳〆〘〯「〢〠】〱〱《〤〽〢〄〤〵〪」〆〘〲〪〼〷〕〚〙〢〳〲〦〥〃〩〳〤「〽〽〇〖〶〶〾〴〰〷〨『〟〲〬〵〲〸〩〕〣〫】〝〇〡〿〳〦【〧〖〓〫〿〣〖【〙【〵々〶『〵〟〠〇》』〲〹〾〰〰〙〚〖〳〞〄『〤〠〇、。〆〧　〒〘〱〾〢〲〵〇〼〼〪〤〵〓〴〦〵〛『〘』〭〔〯「〓」〤〼〱〒〤〶〰〖〬〻【〳〵〡〃〙〠〩〛〝〰》〸《』〦　〿〭〵〺〈〓〵〛【〴〤〒。〪〷〢〡』〒　〄〚々〽〄〔〖々。〪〠〢〸〮〵〾「〉〙〆〘〣《〩〽〃〄「〕〢〻〉〷〛〫〇〪〯〵《〷〚〕〇〟〔〛『〣〆』〸〶々〳〾《〭〯〫〄〔〗〨〺〛〴》〻〫〨〢〜〱〇〦〘〺〉〫〇〧〿〶〲〉〖〵〦〹〷〳〈〞』』〡〓〺〟〡〭、〧〺〺〱〟」「〠〡「〠〬〰〙〹〥〙〓〶〫〳〣〢〳〇〫』々〡〚「〮〘〭〹〶〸〮【〔〚〆〆〼〷〖〒〤〲〕〳〴〾〇〔〹「〦〔〹々〘〲〔〃〡〪〚〪〗〉〓〫〦　」〟〳〛〉〹〺〭〲〆〙〽「〱〘〿〡〭〦】、〠〰〢〥《〶《』〶〃〼〄〪〥〙【。』　〸〳〈〇〡〩〮〃〹〘〧〿〱々〿〭》〶】〥〜、〬〖〠〢。〾〫〔〩〥〫〓」〲〢〛〶〚〡〈」〡〦〼〰〔〾〨〔〄〹〬〛〃〇〸。〽〠〵〙〠【〶〉〇〗〔〒〒〇〉〧《〗〮〟〡《〉〻〧〝〓〱〧〜〘〦【〸〘〩、〵〡〈〴〭『〉〕〴〯〰〘〳。〴〃〙〨〄〈〿〒〕〯」〼〳〤〱『〓〚〛〳〣〳〺〒、〃〚〲〲』〳〃〷〵〹〷〾〞〞〹〣〢〨〵》〽〮〒〹〻〨〜」〇〗〨〙〒〃〆〫〹〉〻。〄〔〧〝〒〷〛〲〧〪〺〚〼〳〒〙〫〢「〲】〾〬〸〷〿〉　〱〛〙〰〜〧》〳〉】】〮〈〗〢〧〟〠〣　〭〵〰「〼〽〭〫〘〴〲〺〾〘「〮〯〩〛〤〣〥〛】〱。〬〴〞〰〣〻〵〹〤〇〴〮〦》『〨〛『〡〞〥〄〠〸〽、』〣〬〢〠〯〰〄〇〆》〇〵『〹〛、〃〟〙〡〷〿〩〥〶〲、〓〧〲〪〚〕〞〢〗〖〝〰〵〪〴〿』〱〮〳〫】《〹〟〻〝〓〦〣〞〤〷〠　〃〈〛「〱〿〆〟〟〉〤〿〈〦〥〻《〻〼〇〢〰〢〒【〞〆「〢〻〧〇。〭々【〪『〪〓】〹〃〄〹〕〝〒〚》〔。〕〶〺《』〦〗〳〰〶〨〔々〖《〰〷〛〩〨』〤〻』《。〵〱〼〵〛〝〧〼〡〶〧〾〯〷〞　〧〛。〦〛〪〕〶〱〆〤〻〹〱〰〖〨〥〚々。〾〽〦〸】〛〇〫》〃々々〲《『『〱〘〲〕〦〇〱〈〞」】〞〨〖〚〽〧〥〬〰〬〥〇〡〼〴〲〠〭〖〵〯。〙〪〖〯〄〾〮〗『〉〴〩　〃〚〲〠〨〟』〖〜〥〛〉〲〃〃〮〳〡〳〩　〄々〞〨〛〪「〼〓〭　々〵〘〄〝〭〖〰〾〬〆〸。〻〓〞『〥〗〪〚〇〞〭〤〉〼〬〕【〤】〥〡〛〖〕〆〧〝〧〺《〭〈〸〪〆〺〸〝〭〇、〆〯〴〸〤、〾〒〉〰〛〷〽〶〿〰〫〜〔〪〱〇』』〰〨〞〓〽〻〻〙〪〠〨〗〓〣〨〾。〜〃〘〚〇〟〖〗【〥。〡「〾『〙〢〦〹〩〟〠〘】〾〒〈〔『〣〲〉〉〻『〇〦〽〿〼〾〚〮〧。〷〰〲〧《〹」〕々〻〤〗〦』《〳〢、」〤　〰〞〠〨〾〪〯〮〳〒　〰〜〼〕〰〳〄》〤「〗〽〇〠〔〝〚〽〣》〷〙】〶〷〆』〇》〓〄〤〸【〡】〾『〯〶、〵〨》〼〗〨〶〉〄〭〓〲〞〝〞〡〻〷〻〣〰〈〽〮》〲《。〸〶〿〣〞。】〡」〖〩〔〜〘》〤〦」〓『〨　〹〞『〛〡〧〬〃〷】〔〫〆〤〻〲〆〯〞〿〧〔『　。〓〳〝〢〿〮〯〵〮〨》〴〒」〒〷〻〶〡〽〤〭〽〰》〾〹。〳〔〹》〴〕〫」〹〜〻〦〳〕〺〘〴』〈〽〲〃〔〙。【」〇〨》〨〴〿〄〻」〉》「〚〺〿〹〤』〄〸】〴〩々【「〫〒】〄〛　【〰〯〶〰〉【〮」〦「〣》〴〙〿〽〄〔〈〓〻〠」〚〯　〷〄〆〳《〸　〴〕〩〸〾〡〼〻〆〬〶〞〓〤〩〿〪〻、〠「〲〓〠〦〛〢〓〇〸〡〬〱】〞〫〽〖〉、〻〿〈〸〓〹〯〰〸〰〘〫　〬〬〽〦〣〾々〥《〰〗〩〰〞】〪〆〷〳〚《〯〱〓〣〭〗。〬「〢〸〮〤〓〖〾〣』〘〳〕【〼〤〔」〵〰〪〡〲。〤〃』〧〙【。〝「〶〻〝〖〢〡〿〓〖〺〝〈々】〈、『〼〣》〔〪《〢〣、〛〕〙〞〭〿〧〵」〴〾〯〫「〨〕〨〄〷』〵《〶〼〘〗】々〖　〳〶「《〝〰々〢〙〈〣〶〟〓〱〬〇〷〦〿』【〕〪〶〺〽〄〡〷〽〲》〟〃」〵〤〞〤〠〜〵〽》〉〡〦〖】〉〓〥〤〞　〺《〖〗　、」〯〳「〾【〩〮。〝〮〙】〦〴『」〘〕〉〚〯〳〇。〾。〇〔』』〚》〃、〠【〝〮」〟《〆〮〇」〥。〟〦〿〠〟〰〺〳々〯】〨〸〼〳〭〶〷〮〨〳〘〤〦。〠『〸〖「〰〝〡〻〻、〇」〇〚』〧」》〮〲〫】〱〼〻〲〷〓〉〵〩〢〣〻〚〞〧〰〽〕〭〧々〠〹〃〟〄〰〚〽〣〚〥〺〛〟〄〮〟〴『〾〒《〺〡　〒〜〈〶〔〫〲〃〟」〿〘〥〥〥〓『〝。〧〾〓〶〺〆〷〩〣〫〜〿〿〰「〕〒〓〯〣〘〗【【〪〾〛〕〽〫〹【〿〧〛〵〲〛〒〇〉〧〺」〺〺〡『〳、〪〾〒〈〮〜〞〙〱【《〣〬〈」〣〵〹〥〵〞〻〆〭〵〟〒〲〧〓〖〣〓々〰〞〹〇〮】〪〫〶「〦〽〓〻〓】〽〭「〣〔〹〯〨〖〩〵〦〳〯〯〧。〗』〾『〩〗〴」〼〗〨〵〥〴、。〒〣〧【《〓〜〓〠〢〓】〷〺〼〕〡〆、〦〿〥〾〚】〕〦〖〙　〭〬〙〇〳〄〃〄〻〧〔〚〰〲〟〷『〫　】〲〲〸〳《〢〵〰〟〪〉〜〨〇〶〻〻〩〄』〒〴〨〈』〗〿〚『〝　〹々〳〼〲〗〙「〵〲〢〔〫〵〜　〘〶【〬『〱〗、〧『〛〇〛〒〈　、〦】〙〇〖〤〩〜〉」〉〿〬〧【〶〦〃〘〈〖〄〶〦〚〜】〛〽〡〸〰々〈「〾〼〒〥〞〸」〮〸〒〗〙々『〇〄〈〃〜〺〯〉〉〾〹〺〚〞〽〦〄〢〽〄〞〻　〼〄〘〙】〚〼〫〴〚〫〬〖〭〔。〰〹〶〺〕〨〇〛　」。〇〿〲「。〆〗、《〫〬〨〻〝】〓〥〾〴】〹〈〞〺〜〰〜〬〴〱〜〖〾〣〭〥　〯〩〶〈》〸〝〼》〶〆〆〽〼「〗〓『〕〃】〡〠〹〺〈【〸〝〤〮〸〭〩〼〈〃〃〉】〳〿〃〬《　〩〈〒〢〠〆》〇〭〬〓〖〝】〧〶〞〈〶〘】」〽〝《〡　〈〟〶〯〹〦〨〷〩〧〞《〵〬〰々〞〧〓〥》」》〤〥〧〧〓〛。〦〄〫】〪〔〟〟〷〧〷〟〺〪〩〷〡〘〞「〔〽〯〔〬〈、〴〨》〥〒々〼〒";
    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, null, Mode.SEARCH,
                                            KuromojiAnalyzer.getDefaultStopSet(),
                                            KuromojiAnalyzer.getDefaultStopTags());
    checkAnalysisConsistency(random, a, random.nextBoolean(), s);
  }

  // LUCENE-3897: this string (found by
  // testHugeRandomStrings) tripped assert
  public void testYetAnotherCuriousString() throws Exception {
    final String s = "〦〧〷《〓〄〽〣》〉々〾〈〢』『〛【〽〕〗〝〓〭〷〷〉〨〸〇〾〨〺〗〇〉〲〪〔〃〫〾〫〻〞〪〵〣【〩〱〭〨〸〃々〹〫〻〥〖〘〲〺〓》〻〷〽〺〯〫』〩〒　〇〔】〳　〵〮〇〡「》〭〆〒〜〱〒〮〺〙〼」〤〤〒〓〶〫〟〳〃〺〫〺〺〤〩〲〬　〱〜〝〤〘〻〚〻〹〒〃」〉〔「〺〆々〗〲〔〞〲〴〡〃〿〫」〪〤」「〿〚』〕〆』〭『〥〕〷〰〝〨〺〧【『〘〧〪』〫〝〧〪〨〺〣〗〺〮〽　〪〢】「〼〮〨〝〹〝〹〩〳〞〮【」〰、〳〤〩〄〶〞〠〗〗〙〽々　〟〴〭、《〃〝〈〒〸〷〓〉〉〳」〘」》〮〠〃〓〻〶〟〛〞〮　〇〨〭〹』〨〵〪〡〔〃〤〔〇〲〨〳〖〧〸　〴】〯〬」〛〨〖〟》〺〨〫〲〄〕」〵〦〢〴〰〨〺〃〓【》、〨〯〥〪〪〭〺〉〟〙〚〰〦〉〥々〇】〼〗〩》。〩〓〤〄〛〇〨〞〣〦〿々》〩『〕〡　〧〕〫〨〹。〺〿《〪〭〫〴〟〥〘〞〜〩。〮〄《〹〧〖〿》〰〵〉〯。〨〢〨〗〪〫〸〦〴〒〧〮」〱〕〞〓〲〭〈〩『〹〣〞〵〳〵》〭〷「〇〓〫〲〪『『》〧〇〚〴〤〗〯〰〜〉〒〚〔〠〽、〾〻〷〶》〆〮〉』〦〈〣〄、〟〇〜〱〮〚〕》〕〟〸〜〃〪〲〵〮〫〿〙〣〈　〳〾〟〠〳〙。〮〰〴〈』「〿《〄〛〩〪》「〓〇〶〩〇、〉〦〥〢》〴〷》〦』〉〟〲〚〹〴〲》〣〵〧〡〾〦〡〣「〆々　〔〄〓〡〬〹〣〰。〵〭〛〲〧〜〽〛〺』〛〵〒〽〻〆〚〚〟〵〲〺〠〼〻〄。〯〉〃』〕〫〥〦〕〔〢々〷々〥〥〖』〶〿〘〗」〖『〢〯〫〇〣〒〖〬〜〝〩〉〾〮〈〩、〘〰〦〧〓〬〸〓〺〼〟〰々〩〩〹〣」〓〸〄『〆〰〹》〵〉】】〼』』〸〣〦〾〰〗〴〥〴〤〃〿〡〳」〢〩〡〮〻〘〤〝〗〃〪〘〈〴〪〯「〭〓々〃〯〄〼〚〧々〢〃〈〔。】〆〣〰〜〪〮〣〿〕〮〾〱〇〈〟〭】〔〥〡〝〙〛〔「　〼〶〸々〹〯『〞〒〇〟〃〳〓〩〝〿《〵】〙〛〪　〭〼〈。〷》〨〰〵」〤〄〾〄「〈『〥〽〕〙【〤》〳〝〔〠〤〲〘〱〈『〴〫〚「」〛〸〹】〱〒〆」。〯〃】〼〮〒〄》〾〷〥〟〞〲〜〲〟〫〕〆〇〸〸〹〾〰【》〨〤〭「〇】〳〯〤、〙〳〺『〲〽〬〥〠。〹〃」〹〪〭〒　〇〶〧〟〻【」】〙〤〡〱〖》〇々〽〬〥〨〠〘〺〳【〫〄〜〹〄〚〯〈〸〻〓〥〤〻〮〃〗々〪〺〿〬〙〈『〭〩〟〽〬〝〄〦〇〥【〨〫〦〗〯〞〜〈〒〽〖〧〼〈〭〓〶〃〰〙「〧〉〹〢〕〼〒〸〼〣〡〔〩〯〼〚〲〖〪〯〒〮】〥〙〯〆〡〲〾〭〫〕〘、〖〮】〟〺〝〨〤〯〓〛》〳〢「〒〥『〿〔〸。〫〬〡〓〝「々」。〘〣〲〴〆〲】〽〮〮〲〓〞。〲〘〉【〲〭〰〨〩〱「〆〩。〦〉〇〄〺〱」〮〄〯。《〭〹〳〸〜〮〧〷〜〹〥〾〨〬〦〮』〖】〖〥〞〕〧〹〽、〺〜〯〒《々〠〠〴〝〤〇〷『〳〞〠〤〣。】〝。〛〉《〩、〦〻々〄〙〞〽〒〧】〉〺〦〔〄〯〙　〫〴〈〽〴《〰〱〗〢〓〔〗〖〖〪〷〠。〨〠〙〴〷〿〻〴〪〠　》〉「〛」〟〗「「〚〤「〫〨〣〉〶〥〢〈〯〄〈】〃〵〪〼〸「〾〥〒〲〮】〙》〡〯〓〵〡《〬〾〛】〄〡〦〪、〆、〵〒〹〰〴〜〬〶〭〕〟〠〰〜〶〵〨〾《　〻〵〔〘〟〾「〡〃〼。〤〺〭〨。〪》〄〇〄〔〖〺〪』〆〸『〰〭〆〗〪〪　〇〜〡〨〞〧〇〛〥」〼〇〼『〸『〵〼〇　〽〹〨〪〗〳〽』〵〽〸〷〄〿　〩〢〺〳〗〞〹〒〼〕。〇〷〔〯〜〘〾」」。〥〯〤〖〛〙〹〘〯〡〱〮》〰〾〚〚〣〆〰〹〾〝〉〲〠〗】〤〿〶〱〾〇〽〤〰〆〭〝】〤〰〼〪〬〰〸〓、〃〵〄〉〤〲〱〨〵〴〮〹〬〧〜〭〶〒〯〺〬〒〭〲〡〔〚〹〇〫【〯〥〪〻々々〨〧〳〛〯〿　〈〽〥〘〖〣〿〫〲〶〚　〓〙〫〴〆〙〶〽〉、〔〪〫】〤〟〓〃〝、〧〡〸〸。〸【〹　〧〡】〡「〗〴〴〳〶『〱〖「〺〠〼〾〱〃〖〤「〧〭〟〇〧〙〕〩〭〻〤〩〪〳〪〟々。〷〥〗。〳〸〆〢「〆〿〻〚〳〚〸〟〘〡〘〇〶〖〡〇〾〥〖〝〝〹』〦。〖「》〥〞〳〛〕〖〥〻〙〾〔〬〈〇〓」〭〹〷〪〖《〫〾〒〙〺〻〨〼〇〝〾〣〴〚〩〴〕〢〦〩』〭〧〵〾〟〣〬〥〟〣〜」、《〲〧〪〸〸【〙〹、〤〽〰。〦〩〮〹】〸〆〹〗〓〶〇〤〳】〾〨〞〩〱〡〇〱〮。〶》〝〱〗〃〘〣〬〲〽〈〒〻〃〥〪〭〤〗〰『〵〹〙〇〵【〕『〤〄〕〥〵〸〮。〳〮、〤〣〱〧』〯〜』〉】『〷　〰〵〓〙〃〟〆〼〞「〫〄」』　〨〹〸《〷〔〫《〝〞〆〬〩〟」】〾〷〄》　〵　〫〵》〻〨〰　〟〈〰〽「」〸〣〪〮〛〞〜〦〱〚』〕〱〪〲〩〥「〚〓〺〣〶〨』〕〇〮〹〟〞〕〶〡〭〠〕〦〦〢〽〤〈〈〻〣〧〱〿〵】〖〞〖【〢〩〼【　〻〘〃〤〫。〠、〗〢〷」】〼〘〖。〤〘〄〢〴、〘〆〯〱〜〃「〦『〯〰〘〫〹〶〷〿、】々〙〛〜「〹々〮〿「〸〉』〯〱〄〓〥〣〩〥』〖〤〛【〭〿〺「「〳〛〧〉『〈〆〒〠》〳〈〳〩〃〮〚〼」〲〮〩〮〮〢〸〿》〈〉〗〾〇〕〩〸〖〾〠》〃〞〄〣〭〡〕〣〚〆〤〄。〸〞。《〼〄〤〸」〿》〤「〵〥【〔〕々〙〸〛〛『〶〾。〷〫〼〽〤〨〓〭〻〈〶〿〾〨』〤【〾」〇〤〒〠〺〜〸〼〪〢〷〔》〣〤〬〣〱〝〇〺〢〠〤〹〡「〪〲〿〬〘〡〯、】〖、〈〶〛〢〕々〽〼〼〚〿〘】〢〰〡〿〗《〉〙《《『〶【、、】〡〓〦〞〵〤〧』〝〕　〄〃〸〈〤〪〻〭〉〘〷〉〕〨〻〢〢〡〸〔〮〧〹「〦〘〉〾〉　〺〽〷「〺〖〺〝》〃　〇〪〜〶〺〣〇〭　〾」〣〼〞〷々〽〤〶々》〻〈〽〒〕『〬」〈〟〕〷〼〲〄〚〜〴。〮》々〧〻〔〕〈「〾『。〴〷〯〢〿〦〈〸〩〻〃〻〚〞〤〈。〧〇〾〺〢〓〵〸〛〔〡〷【〜〺　〕〶〦〣〻〟」》】〺〚〷〺〹〙〳〺〬〓〢』〘〕「〸、〙〾》〖》々〬〄〇『。〵【〩『〺〆〮〮〙〵〫《〃〽、〓〠〨〚〕〈『〦【〗　〄〴〫〡〮〱〔〆〗〟〵】〻々》〲【〬〢〚〛』〱〰〫　〇〤〴〮〾。。〮】〇〲〻〙〰〥〚」〟〜〄〟。〤々〞》〧〉〳【〿〺〆〈〖》『〤〄』〾〵〲〸。〈『〕〺〘〣〶〬『〪〆〳〽《。〒「〽〨〸〜〚〘〪〤々〦〆〺『〣〆〽〇〿「〥〵〒〲〟〜〳〭〼〆〡〮〆「〆〥〺》〱『〺〔〃〙〻〥々《々〙〼〪〼〵〙』〥「　〵〯〓〩。〰〕《〟〦〝、〦〦〤〗〴〩〹〶〠〰〡〇〤〹〓」〣〆〜〴〘〔〃「〤〈〩　〠【〃〙〢々〉〝〬〙〭【〮〗〙〤〿〖〓〫〻』〞〤〼〳〹〄〵〾〔〛〮。〒〉〤〣〭〰〨》〭〲〗〃〇〆〡〜〱〲〮〫〄〬〄〉〯〈〮〩【　〮〦《〪〲〣〡〶〬『〲〵〇〶〰〒〭〽　〰〄〻〄『〬〩〠』〕〫〤　〼〶〳〮、〓〸〲〓〜〳〺〈〫〺〒　〨〡〡【〷〆〇』〝〩〨〗〕〪】〪　〛〛〺〙〷〦〠」〱〞〼〸」、〢　〺」』〲〆〃〟〱〟〝「《〸〳〒〖〨】〥〖〈〧〼。〫『〙〧〡『】〔々、〼〝　〕〙〇〘〲〔〝〺〘〄〓〒〼〈〛『〺々〩〱。』〬【〱「〳〜〼〬〴』《〗〔〡〰〪〤〥〲《』〥〉〪【〶〤【〻〡〒〯〜【〽〪〉〠〾〙〰〚〵〦〦〴々》〙〠」》〠〱〓【〶〦々〻【〽〶〼〺〷。〶【〘》〻〗〳〣、」】〳〓〞〆〆〾』「〈〙〕〱〢〳〨〰〡〸。〣〪〤「〱「『〙〽〇々【〜〖〮〚〟」。〜〰〉〔。〣〽〇〖〬〆〥〖〧〨〱〡〸〪〣々』〄「『〞〶　〴〰】〃〱〱「〶〝】〞〭〚〴〶〻〟〧〡〳〬〧、〣】〕〼「〠〃〷〣〩　〭〄〩〝〦』〟〇〦〟〕。〩』「〵〩》〿〻『〙〼〲〰》〨〉〆〓、〺〹〸。〞〧〗〘〳〓〞〹〕〡〼〔〖〴〄】〚〻〯〴〣〮〦〧〣〵〼〚〾〫〼〣〔〚〽『〵〒【【〝〹〮》『〨〜〠〸〠〵〨〙【〧〸〈》〱〗【〓〤】〰】】、〩〽〈〸〔「〵〻〙〓〰〇〚〞〗〙〢々〭〜〈　。〧〿〧〨〵〾〝〬【』〫〦〸〬〈、〒〢〉〞〵〒〼〝》〻〫〧〤〶〹〼〩〛〫〣】〿」〴》〺〬〤〕〲〕〙〔〪〰〿〬〒〔〞〆〻〴〘〩〨〤》〩〪〭〳〇〣〚〟〚〕〓〴〱　〵〃〠〭〠〚〗〃〃〸〰〢〡〿〭『〗〉、〲〕〧「〛〛〓〜〰〮』〱〨〬〨〽〸〽〶〣〯〫、〯々、〴　」〕〥』〻】〖〴」〨。〖〤『〜〰〩〣〣〸、〫〝、〯〹〷〳〚〄〷【〃」〼「〤】　〢〖　〣〙〺〽〽〱〤〔〓々〣〭〽〘〦〻〪〿〞〝〱、《〆」〸〷〛〓〕〹〜〪〹〶、〵〦〛〲〒〹〪〦〃〥「〸〪〙〧〱〠〰〝〆〠〯《〼　〛〚〔〟〽〗」、〲〥〞〴〃、「『〖〼〞〪〼〇』〿〶々〙〻》〥、〵〛〞〠〫〟　　〹〾〵』〤〿〣〪〗〃〖〬〩〴〗々〓〝〥〥〜〲〯〗〤》〛〮》々〚〘〫「〙〉【〆〽〨〹〮〧〷「〴〝〬〷〗『〔〷〮〟〲〬〸〸〟〹〆〖〨。〣〄』〴〚】〘〲〚〚〦〈〛〗〞〉〞〯〆〵〸　〗〕、》【〸〮〵〉〥〨〕〟〭【〾〇〵〬〾」〱〹〚〟〛〡」〩〃〄〬〱〭〚〱〆〛》〣』〝〡〦〣〫〒〗〛〿〤〇〼〠〲〢〬〿〓〠」〚〇〛〈〴《〦〱〤〹〝〱〶〟〙〴〶〣〝〮　〜〲〱〿〳〪〄〝〃〰〙〖〼〰、〬〰』「〭〻〮　〩』〱【〆〻〺〸〾〤〗〸〥〽〼】〤〣〖「《〡〙。〸、」』〠》〴〈、〴〢〣〲〟〳〸〒〠〣〵〢〿》』〿『〾〔〢〶〦〟〠《〹「〷〽〷〆〇〉〲〿〵〙』〫〠々々〘』《〽〒〦〽〓〳、〮〻〫〞〲〰】【〗」々〥』【〫〆〫〳〾〣〖〺〷〙〘〄〈〼、〧〻〭〮〳』〘〾〇〸〉〽〗『〙〽〻〟〇〘〽〖〴〄〓〞「〦〪〚〾〨。〕〻〰〟〉〢「〉〿〯〔〹〃〛〛〝〔《〵「「〴「〗〸〖〞〦【々〣〲〤〾〿〽〲〥〢〥』〳〳〼『】〆〼》〩》」。〛〲〡〳「〢〥》〘〠〃〳〃〒　〧〓〡〤〄〲」〦〶〷〟〛〠〱〽〫〫〸〇〔、〪〛〠　」〢〳〸『〸〚〹〈〘〉〫〇〲〲〈〕〙〱】〯々【〬〖〿〒】〔〭〣〚〄〈』〧〗〹】〇〬〸〾　〭〺』〯〫〻『〘〻〱　〴〆〘「〠〈〫〡》〤〕】〜〙〵〒〙。〦〮〞〪』〴〓〪〾〝〹〴〼《〦〞〖〆《〥〸〻〈〽〪〤【〖〶〞〤〃〰〨〱』〨〼〱〠〣」〝〹〝〕〼〔〃、〮】〤【〼〤〼〥〪〲〓〦〘〟〞〭〜〸】〚〸〵〞〙〧〈〽〹〄『『〙〓〸〯。〜〺。「〖　〶々〉〈〮《〢〭〶】〘〜〺〸〒〥〢〾〈』〱〃〤〳〖〉〼〫〛〚〽〫〳〰〫〥〜〜〺〷〲《〢『〛〭〈〧〳〣〜〝〧、〥〾〻〳〺〕〥〥〼》」〺〮〒〣〥〲〟〠〫」〾〱〼「〄〆「〓〽〹〵〈〙〛〵〰〩〟〫〈〔々〒〟々〉、〷〚〶〆〘〛。　【「〸〸〖〫〕〰〱〺〟〫〿〹〩〇。〾〒〚〲〾〛〳〨〦〙〒》。〺〧〡〞〒〚〩〪〶〘〣〨〶〩〛〺〙〪〄〼〮〰〒〡〼〓〙〒〇〽『〃　』〇》〽〃《〒〠「〚〨〗〶〴〪〮〵〘〨々〓〗〚〠』〗〮〳〺〲〙〒「〴〼〻〤〉〯〨〧〈】〾〟〝〒〃〘〧『〶〿、〤〝】〜〴〰〷〽〮〱、〩〽〺〯〫〜【〴〈〳〖〬】〦〘〗〜〝〄〚〚〤〨〲』【〞「〰〔》〷〥〈〡〳〢〾〮《〭〫〡〴〹〻〚「〰〻〉〣〢〤〤〝〩〧〙《〓】〺〺〓〿〹〈〚〱〬〘《〽〈〕》〣〓〒〴〆〜〭〖〛〝〷〧〴〮》〳〘〸〴〿〥〙〒〔「》〓〕〦〯〾〯〝、〮、〯〆〛』〞〝〵〥〬〚〡〰〔〵】。〽〥〿』〩〇〝〄〴〪〭〸〫〡〣〧〆〚〫〴〙〦〽〉〸〼。〱〨〛〠。〮』〝》〻〹〈〄《〻〱〥〞〽〾〄〝〢〿。〴〆〲『〰〢〖〲〼〯〃〠【〲〵〛〣〝〕〬〺〰〪〻『〨】〖〥〵〹〯　〒「〠〮〈〃〹〽〬』〹〷〫〕〧〟〒〉〉、〈「〟》〼〪〰〗〘『〞〉〹〚〤〩〦〗〖〮〰〇〠〫」〔》〮、〆〡〛〻〙「〵。〯〹〘「〵〫〼。】〃〢〺〴〛〪〬　〞〟〓」〭】、〸〘〻〈〤》〓〩〽〆〵〨〈「〦〠々〨〒〢〛〝〿〗〥〱〕〩〖〣〄〚〿〆〗〢〉々《〚〩〶》〥　【『〪〯〾〸〪〲〞〠　〡〓〻〷〢〕』「〹〯〛〫〲〗〗〚」〵【〪〢〥〫〆》〦〥〱〯【【〉〧〺〻〉〬〳〒〳〾〲〲〇〇。〪〙〧〿〆【」〇〪〸〽〦〚〽〿　〠〺〥〦々〬〄〟〪〭、】〴〾〸〛。　『、《〫〺〯〛〩》〓〴〪》」々〧【〦〇〮〬〲〗〔〦〴〣〼〨〖〩〬〼々〛〇」〴〦〉〤〺〪《　〒〧々〤〧〣〘【〵〛〢〵《〛〘〵〓〶〳〤〺〨〣〭〤〪〮〺〷《〗〵〞〻〠〭〃】〄〒〯々〶〉〞々〽〤〇〦『〦〽〩〬〠』〷〄〩〙〖〝『〘『々〔【〿〰〶〪〱〉〘》〃〙〧〦〇「》《〹〰〯】〹〄〈〪〜〵、〮〣〇〯〲〛〬〕】々〸〹〩〟〳〆〥〯〬〠〭〯『〙〆〾『「〈〬〹〕〾、〸」〷〥〆〺〾〖。〆〒〮〻〡。〉々〕』『〨〼　〢〓『〢」々『　〘、〖〤〜【々〤〷〵〳〤〽」〟〥〴』、〒〥〆〙〬〧〔〡〄》〷。〣〉〪〙〚〾〣〵〰〮〔〇〝〫〫〩。〪〷〩。〇〿】〲〦〳〕《〄〴〦〽〔、〱〧〟。〻〺〔〝【〲〔〦〙〖》〠〫】〵〙〰。〖〸〼〣〗〲々〤〢〷〝〰】〳〳〯〟〓〬〺〤〿〲〩〞〡〧〲〧〭〽〪〰〥〧〴〈〈〢〕〯〔〨々〭〸〡〖〓〤〒〝〻〻』〣々〸【〸〸〷〓〇〦〻〤』〉〾〛「〢〢《】〜々〛〇〠〒〹〖〽〮〚〫〜〼〄〓　〹〽《〽》〮【〺〦〠〨〰〸〘〲』」〹〳〤〽〴〴〰〳〷〟】〼〽〓〇、〡〚〶　〥〄〉〴〵、〷〳〥〬〳〓〩〯〜〪〯〬々〢〾〆〨〥」』〪〄〨〽〗〭〯〼〒〡「々〩』　〉〔〓《〉〺〫〖〽〱〳〡〪〯』〼〉〝〟〹〯〇〠〥〨〖「〢「〥〲〘『〹〥〶〜〥『〃。〲〗〢〩〮〕〨〸』〪〯〲】〠〻〟〶〣〸〵〩〔〾〞〳〾〇〵〥〟〭〳〡〆〾〤〶】〈〓〄〮〢〒〩《〔〭〄》『〰〧〡〖〵〥〵〒〭〳〵〝〜〱々〞〰〴〦〱〿〾〴〪〥〧〚〚〒〚〘〿〛〾〫〚〕〷〔〗〢〻〠』〘〾〖〿〦〥〮〆〼〞〴〹〸〻〵〞〄々〷〔《】〛〒〻〓〴〮〛〺》〫〬々〦〦〬」〯〞〼〚〘〰〿〝〾〘〠〵〴〃〞、〹〢〗〹〰〤「〔』〇〒〭〫　〞〉〿〜〳〫〩〿〧〵〟〾〤々〩〝「《〬〃〇〬】〔〇〆〷〭〬〵〾〚〺〬〧〻『」〈」〻〹〞、】。〉〯〫〺〒〙」〱〛〻「』〱〺〠〄【〿〦〰〸『〬〴〓〨〢《〣〓〜〒〡『〼〔『〵〕〝〗〳《〲〳〼〝「〽〬〱〺〠〱〽〘〗〹〨〆〕〠々〓〤】〺〉〴〰〮」〰〿〹〳『〠〔〇〧〭〼〪〭〯〖〶〬〃〱〔〙》〺〜〵々】〡〧〲』〕〛〳〥〩〱〮《〦〫】〖〈》〞〻〤〢〦〪〬〲〗〢〷　　〳〰〓〕〜〥」〬〗〒〜〉〩〆〬々〿〪『〣〘〡〘〯〳【〄〠〸〼〈〰『。〟〲〭〡〷〥〯〴「。〤〓〪〆〦〆〒〽〫〰〚〡〨【〯〹「〧」〓〖〘〳」〕〲〚〣〕〆〃〱〞〷〺〻〃』〩〫〦〱〴〟、〰〘〞《」〛〤〿〔　〤〱》〗〷〡〡〗〞〦〿。〤〳】。〟〻〉「〻〙〖〿〄〶」〾〫〽〸〕〢〰〞〞〒〜〻〠〭〫　〞〴〰〶〺《〣々〩〲〡〴》、〩〝〞【〼〓〱〻〩〒〖〿〮〱〧〟〒〶、〿〈「〻〴』』〇〉〝〛〢〜〼〘〰〇〢〃〲〟〨〟〣〟〰〉〮〘〽〧。〓〳〩〺〳〓〘〗〖〈〜〴〟〽〣〣〾〽〩〲〜〇〰〩〕〧〚〄〴〴〴〨〠〦】、〣〺〖》〯〷』〒〤』〙〗〬。〧〆〜　〧〩〯〞〜〬〡〆、〞〔。〾〩〈〛〼」〾〮〤〾〟』〉〔〞〾〛〲《〈〫〝〽〳〞〔【〿〽〩。〈〨「〯《々〇、〯〜〾〝〯〼〆〟〉〝〮〙〪〚〮〱〹〯〜〟〠、〄〹〧〳〱〯〖〯】〩〴【〫〇「「〿〩〷〾〴〯〦〼〦〟〖〤〪〥〰〔〻〪〄〖〳〵〟〕〰〬〶〚『〘〻〇〽〪「〉】〮「〣〿〇〭〕〓〵〽〆〳　〨〩〕〬〵〸〻〲【『〥〖〚〢〰』〠。、〮〣〆〴『《〲〓〷《〱〰々〫〶〢〯〗〚〙〶〫〖〃〻》〰『〱〘〫〛〄〉「〠〱〚〖〕》〤《　〵〶〢〯〗〳〛〚〽〗〟〛〪〾〶〞〶々〆〯〇〝〕〨〨〣〫〄〵〞〛〬〣《〦〦〒〉〙〫》〞〨〜『〝〻〒〟〓〜》〡〡〫〻』〆〒　〔。〓》《〨〙〿〙〔〘〮〦「〚〻、「〵〠〉〬。〭】〱〸「〶〈〞〈〪〟〻〝〲〮〆〼〯『〱〡〙〮〕〒〣」〳〥〙〡〡『〇〠〡〭〷〜々』〣」〼々、〗〡『〽〻〽〳〉〄〵〬〽〯〥〾〙〉〿〮〴〷〥〡〰〹〰《〺【〒」〙〾〽。〴〘〕〝　〳」〡〇〩〥〾〆〨〉〫〠〙〤〒【〸々〣〓〰」〈〪〵〠〚】〈〆〵〗〜〦〣〃〼〔〉》〆〞〚〆〄〫〺〽〪々〩〴〵〹〿〔〥〜〩〪〤〗。「〽〨〟、〄〽】〩〙〝〺〶〸〟〯《《〥〣〻『〟〽〮〄》〙〕『」〾〼〷』々〥〒【、〗〔〯】〮　〹〩】〡〇〟〫〢〨〡〭」〄〼〙〪〻〪々〙。〫〧〪〞〾〄』〟〶〇〞〜〥〘。〝〨〸】〕〔〨〕〾〃〾〒」〈〒〓〼〗〖〕〱〙〘〓〝〾〔【〵〿〖〸〷〵〩【〞々〼〢〧〻〥〰〦〤》〰〛〡。〖〝〙〒〽〜〕〘「。〵〇〒〾〼〽〈〣〇〒〙〢〸、〞〲》〪〰〴　〽〭〷〸〫〆〞〾〨〆〛〔〤〜」〈〨〃〈〴〽〲」。【〞〒〉。〱〕〨〽。　〷』〦》〵〩〪〡〕〞〹〃〧〃〝〢〴。〃〛〭〻〣〸〖〞〻【〛》〜〳〜〟〘〄」〸〬〶〥》〨〭〡〦〇〇《〱】〸〼〺〬〛〓〔」〰〈〧、【〕」〳〼〗〯〉〒〖」〧〩》〴」〺。〰〷』〩〚〭〞〰〶〚〲〙〥〢。〽〵〱」】〓〘〦。〭《〥〙、〱〹〦】〕》〲、〘〓〙〷、〪〕〉〭、〇〜々〖〨〞」〠〕〲〨〕〔〻〿〙〘〙』〼〘〡〢〧〚〢〷〸〰〟〰〗」〪〛【〪〺〒〱〈〦〽、『〥　〙〪〕〝〄〛〣〴〯〆〒〰〜〪〆〠〞〾〃〭〬〡〉】〄〃〥〥〒〶〕〢〵〣〢〨〘〩〹〖〧〒〺〫〕〡〆〭〘〿〠〹〲〔〫》〪〰〇「〯〫〈〾〱〄、〮『》〹〿〿〱〦】〳〰」。【〘〆〞〚〱》〫〷〸〠〲〚〶〷〘〩〯〛〄々　』〪〭〬〖〪〦々〼》〇〤。〉〯〟〮〢〤〬〜〪〬〺〿〹〖〔】〕〖〣　『〵〸》〧〻〺〜〧〯〄";
    final Analyzer a = new KuromojiAnalyzer(TEST_VERSION_CURRENT, null, Mode.SEARCH,
                                            KuromojiAnalyzer.getDefaultStopSet(),
                                            KuromojiAnalyzer.getDefaultStopTags());
    checkAnalysisConsistency(random, a, random.nextBoolean(), s);
  }
}

