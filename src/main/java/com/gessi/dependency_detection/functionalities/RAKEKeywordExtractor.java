package com.gessi.dependency_detection.functionalities;

import com.gessi.dependency_detection.domain.Requirement;
import com.linguistic.rake.Rake;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RAKEKeywordExtractor {
    private Double cutoff = 3.0;
    private TextPreprocessing preprocess = new TextPreprocessing();

    /**
     * Passes the text through Lucene's token analyzer
     * @param text Text to clean
     * @param analyzer Analyzer to use
     * @return Returns a cleaned list of strings
     */
    public static List<String> getAnalyzedStrings(String text, Analyzer analyzer) throws IOException {
        List<String> result=new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        return result;
    }


    /**
     * Extracts keywords using RAKE algorithm from a given corpus
     * @param corpus Corpus to be used for RAKE
     * @return Returns a list of maps, compromised of <word,rake_value>
     */
    public List<Map<String, Double>> extractKeywords(List<Requirement> corpus) throws IOException {
        List<Map<String, Double>> res = new ArrayList<>();
        Rake rake = new Rake();
        for (Requirement s : corpus) {
            String text = "";
            for (String k : RAKEanalyzeNoStopword(s.getDescription())) {
                text = text + " " + k;
            }
            Map<String, Double> aux = rake.getKeywordsFromText(text);
            String sum = "";
            for (String j : aux.keySet()) {
                Double val = aux.get(j);
                if (val >= cutoff) sum = sum + " " + j;
            }
            List<String> result = RAKEanalyze(sum);
            Map<String, Double> helper = new HashMap<>();
            for (String i : result) {
                helper.put(i, aux.get(i));
            }
            res.add(helper);
        }
        return res;
    }

    /**
     * Extracts skills using RAKE algorithm
     * @param corpus Requirement corpus to be analyzed
     * @return Returns a map of maps, compromised by <Requirement_id, <Word,RAKE_value>>
     */
    public Map<String, String> computeRake(List<Requirement> corpus) throws IOException {
        List<Map<String, Double>> res = extractKeywords(corpus);
        Map<String,String> processedRequirements=new HashMap<>();
        int counter=0;
        for (Requirement r: corpus) {
            String newText="";
            for (String s:res.get(counter).keySet()) {
                newText=newText+" "+s;
            }
            processedRequirements.put(r.getId(),newText);
            ++counter;
        }
        return processedRequirements;
    }

    /**
     * Cleans text
     * @param text Text to clean
     * @return Returns a cleaned list of strings
     */
    List<String> RAKEanalyze(String text) throws IOException {
        text = preprocess.text_preprocess(text);
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("kstem")
                .build();
        return getAnalyzedStrings(text, analyzer);
    }

    /**
     * Cleans text for RAKE algorithm to use
     * @param text Text to clean
     * @return Returns a cleaned list of strings
     */
    public List<String> RAKEanalyzeNoStopword(String text) throws IOException {
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("kstem")
                .build();
        return getAnalyzedStrings(text, analyzer);
    }

}
