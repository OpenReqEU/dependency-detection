package com.gessi.dependency_detection.functionalities;

import com.gessi.dependency_detection.domain.Requirement;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TFIDFKeywordExtractor {

    private Double cutoffParameter=4.0; //This can be set to different values for different selectivity (more or less keywords)
    private HashMap<String, Integer> corpusFrequency = new HashMap<>();
    private TextPreprocessing text_preprocess = new TextPreprocessing();
    private Map<String,Map<String,List<Integer>>> wordOrder=new HashMap<>();


    /**
     * Computes the term frequency of each word in the text, and updates the Idf,
     * @param doc List of strings to analyze
     * @return Returns a map identified by <Word,word_frequency>
     */
    private Map<String, Integer> tf(List<String> doc) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String s : doc) {
            if (frequency.containsKey(s)) frequency.put(s, frequency.get(s) + 1);
            else {
                frequency.put(s, 1);
                if (corpusFrequency.containsKey(s)) corpusFrequency.put(s, corpusFrequency.get(s) + 1);
                else corpusFrequency.put(s, 1);
            }

        }
        return frequency;
    }

    private double idf(Integer size, Integer frequency) {
        return StrictMath.log(size.doubleValue() / frequency.doubleValue() + 1.0);
    }

    /**
     * Preprocesses the text
     * @param text Text to preprocess
     * @param analyzer Analyzer to use
     * @return Returns a list of cleaned strings
     */
    private List<String> analyze(String text, Analyzer analyzer,String reqId) throws IOException {
        text = clean_text(text,reqId);
        return RAKEKeywordExtractor.getAnalyzedStrings(text, analyzer);
    }

    /**
     * Preprocesses the text
     * @param text Text to preprocess
     * @return Returns a list of cleaned strings
     */
    private List<String> englishAnalyze(String text,String reqId) throws IOException {
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("kstem")
                .build();
        return analyze(text, analyzer, reqId);
    }

    /**
     * Computes Tf-Idf on a corpus of requirements
     * @param corpus Corpus to be used for tf-idf
     * @return Returns a map of maps, compromised of <Requirement_id, <word,tf-idf_value>>
     */
    public Map<String, String> computeTFIDF(List<Requirement> corpus) throws IOException, ExecutionException, InterruptedException {
        List<List<String>> trueDocs = new ArrayList<>();
        for (Requirement r : corpus) {
            List<String> s = englishAnalyze(r.getDescription(),r.getId());
            trueDocs.add(s);
        }
        List<Map<String, Double>> res = tfIdf(trueDocs);
        Map<String,String> processedRequirements=new HashMap<>();
        int counter=0;
        for (Requirement r: corpus) {
            String newText="";
            for (String s:res.get(counter).keySet()) {
                newText=newText+" "+s;
            }
            processedRequirements.put(r.getId(),newText);
        }
        return processedRequirements;

    }
    /**
     * Computes Tf-Idf on a list of lists
     * @param docs Corpus to be used for Tf-Idf
     * @return Returns a list of maps, compromised by <Word,tf-idf_value>
     */
    private List<Map<String, Double>> tfIdf(List<List<String>> docs) {
        List<Map<String, Double>> tfidfComputed = new ArrayList<>();
        List<Map<String, Integer>> wordBag = new ArrayList<>();
        for (List<String> doc : docs) {
            wordBag.add(tf(doc));
        }
        int counter = 0;
        for (List<String> doc : docs) {
            HashMap<String, Double> aux = new HashMap<>();
            for (String s : new TreeSet<>(doc)) {
                Double idf = idf(docs.size(), corpusFrequency.get(s));
                Integer tf = wordBag.get(counter).get(s);
                Double tfidf = idf * tf;
                if (tfidf >= cutoffParameter && s.length() > 1) {
                    aux.put(s, tfidf);
                }
            }
            ++counter;
            tfidfComputed.add(aux);
        }
        return tfidfComputed;

    }

    /**
     * Preprocesses the text and adds two special rules to help keyword extraction, these are that any word entirely in capital letters is to be made a keyword,
     * and that any word between [] is to be made a keyword
     * @param text Text to preprocess
     * @return Returns a list of cleaned strings
     */
    private String clean_text(String text,String reqId) throws IOException {
        text = text_preprocess.text_preprocess(text);
        String result = "";
        if (text.contains("[")) {
            Pattern p = Pattern.compile("\\[(.*?)\\]");
            Matcher m = p.matcher(text);
            while (m.find()) {
                text = text + " " + m.group().toUpperCase();
            }
        }
        int index=0;
        Map<String,List<Integer>> wordOrderInterior=new HashMap<>();
        for (String a : text.split(" ")) {
            if (wordOrderInterior.containsKey(a)) {
                List<Integer> order=wordOrderInterior.get(a);
                order.add(index);
                wordOrderInterior.put(a,order);
            }
            else {
                List<Integer> order=new ArrayList<>();
                order.add(index);
                wordOrderInterior.put(a,order);
            }
            index++;
            String helper = "";
            if (a.toUpperCase().equals(a)) {
                for (int i = 0; i < 10; ++i) {
                    helper = helper.concat(" " + a);
                }
                a = helper;
            }
            result = result.concat(" " + a);
        }
        wordOrder.put(reqId,wordOrderInterior);
        return result;
    }


    public HashMap<String, Integer> getCorpusFrequency() {
        return corpusFrequency;
    }

    public void setCorpusFrequency(HashMap<String, Integer> corpusFrequency) {
        this.corpusFrequency = corpusFrequency;
    }

    public Double getCutoffParameter() {
        return cutoffParameter;
    }

    public void setCutoffParameter(Double cutoffParameter) {
        this.cutoffParameter = cutoffParameter;
    }


    public Map<String,Map<String,List<Integer>>> getWordOrder() {
        return wordOrder;
    }
}
