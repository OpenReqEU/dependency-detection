package com.gessi.dependency_detection.functionalities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.gessi.dependency_detection.util.Control;
import dkpro.similarity.algorithms.api.SimilarityException;

import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import dkpro.similarity.algorithms.lsr.LexSemResourceComparator;
import dkpro.similarity.algorithms.lsr.path.WuPalmerComparator;


public class NLPAnalyser {

	private LexSemResourceComparator comparatorWN = null;

	private LexicalSemanticResource wordnet;

	/**
	 * Constructor
	 * @throws IOException
	 */
	public NLPAnalyser() {
		super();
		try {
			System.out.println("Loading");
			wordnet = ResourceFactory.getInstance().get("wordnet", "en");
			wordnet.setIsCaseSensitive(false);
		} catch (ResourceLoaderException e) {
			// TODO Auto-generated catch block
			Control.getInstance().showErrorMessage(e.getMessage());
		}
	}

	/**
	 * Semantic similarity engine (DKPRO & WordNet)
	 * @param term1
	 * @param term2
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	public double semanticSimilarity(String term1, String term2)
			throws SimilarityException, LexicalSemanticResourceException {

		if (comparatorWN == null)
			comparatorWN = new WuPalmerComparator(wordnet, wordnet.getRoot());
		return comparatorWN.getSimilarity(term1, term2);

	}

	public Map<String,List<String>> prepareRequirements(Map<String, String> requirements, int maxSize) throws InterruptedException, ExecutionException, IOException {
		List<com.gessi.dependency_detection.domain.Requirement> recs=new ArrayList<>();
		for (String s:requirements.keySet()) {
			recs.add(new com.gessi.dependency_detection.domain.Requirement(s,requirements.get(s)));
		}
		Map<String,String> keywords;
		Map<String,Map<String,List<Integer>>> wordOrder;
		if (requirements.keySet().size()>100) {
			TFIDFKeywordExtractor extractor=new TFIDFKeywordExtractor();
			 keywords=extractor.computeTFIDF(recs);
			 wordOrder=extractor.getWordOrder();
		}
		else {
			RAKEKeywordExtractor extractor=new RAKEKeywordExtractor();
			keywords=extractor.computeRake(recs);
			wordOrder=extractor.getWordOrder();
		}
		return getNgrams(keywords,wordOrder,maxSize);
	}

	private Map<String, List<String>> getNgrams(Map<String, String> keywords, Map<String, Map<String, List<Integer>>> wordOrder, int maxSize) {
		Map<String,List<String>> result=new HashMap<>();
		for (String s:keywords.keySet()) {
			TreeMap<Integer,String> orderedKeywords=new TreeMap<>();
			for (String k:keywords.get(s).split(" ")) {
				if (wordOrder.get(s).containsKey(k)) {
					for (Integer i : wordOrder.get(s).get(k)) {
						orderedKeywords.put(i, k);
					}
				}
			}
			List<String> ordered=new ArrayList<>();
			for (String o:orderedKeywords.values()) {
				ordered.add(o);
			}
			List<String> ngrams=ngrams(ordered,maxSize);
			result.put(s,ngrams);
		}
		return result;
	}

	private List<String> ngrams(List<String> ordered, int maxSize) {
		List<String> result=new ArrayList<>();
		for (int i=0;i<ordered.size();++i) {
			String cumulative="";
			//for (int j=i;j<maxSize && j<ordered.size();++j) {
			for (int j = i; (j-i) < maxSize && j < ordered.size(); ++j) {
				if (cumulative.equals("")) cumulative=ordered.get(j);
				else cumulative=cumulative+" "+ordered.get(j);
				result.add(cumulative);
			}
		}
		return result;
	}

	/**
	 * Debug
	 * Utility to read a file
	 * @param path
	 * @return
	 */
	public List<String> readFile(String path) {
		ArrayList<String> fileLines = new ArrayList<>();

		try(FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr)) {

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				fileLines.add(sCurrentLine);
			}

		} catch (IOException e) {
			Control.getInstance().showErrorMessage(e.getMessage());
		}
		return fileLines;
	}}