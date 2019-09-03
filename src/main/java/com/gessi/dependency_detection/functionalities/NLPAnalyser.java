package com.gessi.dependency_detection.functionalities;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gessi.dependency_detection.util.Control;
import dkpro.similarity.algorithms.api.SimilarityException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import dkpro.similarity.algorithms.lsr.LexSemResourceComparator;
import dkpro.similarity.algorithms.lsr.path.WuPalmerComparator;

import org.springframework.core.io.ClassPathResource;

import com.gessi.dependency_detection.components.Node;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.*;

import de.tudarmstadt.ukp.dkpro.core.clearnlp.ClearNlpLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.clearnlp.ClearNlpParser;
import de.tudarmstadt.ukp.dkpro.core.clearnlp.ClearNlpPosTagger;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class NLPAnalyser {

	private static ClassPathResource sentPath = new ClassPathResource("en-sent.bin");
	private static ClassPathResource tokenPath = new ClassPathResource("en-token.bin");

	private SentenceDetectorME sentenceDetector = null;
	private LexSemResourceComparator comparatorWN = null;

	private AnalysisEngineDescription tagger = null;
	private AnalysisEngineDescription lemma = null;
	private AnalysisEngine parserEngine = null;
	private AnalysisEngine lemmaEngine = null;
	private LexicalSemanticResource wordnet;

	/**
	 * Constructor
	 * @throws IOException
	 */
	public NLPAnalyser() {
		super();
		try {
			wordnet = ResourceFactory.getInstance().get("wordnet", "en");
			wordnet.setIsCaseSensitive(false);
		} catch (ResourceLoaderException e) {
			// TODO Auto-generated catch block
			Control.getInstance().showErrorMessage(e.getMessage());
		}
	}


	/**
	 * The approach of dependency detection
	 * @param requirement
	 * @return
	 * @throws IOException
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 */
	public List<Node> requirementAnalysis(String requirement)
			throws IOException, ResourceInitializationException, UIMAException {

		ArrayList<Node> synResult = new ArrayList<>();

		String[] listReq = requirement.replaceAll("\\.(\\s)?(\\((\\w|\\W)+\\))$", ".").split("\n");
		for (String partReq : listReq) {
			// Sentence Boundary Disambiguation
			String[] splitString = sentenceDetection(partReq);

			for (String sentence : splitString) {
				if (!sentence.equals("")) {
					// Noisy text cleaning
					String clearedSnetence = clearSentence(sentence);
					try {
						if (!clearedSnetence.equals("")) {
							// tokenization
							String[] tokens = tokenization(clearedSnetence);
							String reqSent = "";
							for (int i = 0; i < tokens.length; i++) {
								if (tokens[i].matches("(\\w\\.)$") && (i + 1) == tokens.length) {
									reqSent = reqSent.concat(" " + tokens[i].substring(0, tokens[i].length() - 1) + " .");
								} else {
									reqSent = reqSent.concat(" " + tokens[i]);
								}
							}

							// PoSTagg & Parser & Lemma
							Node root = dependencyParser(reqSent);
							// Information Extraction (IE)
							DependencyTreeIE ie = new DependencyTreeIE(root);
							synResult.addAll(ie.applyIE());
						}
					} catch (NullPointerException e) {
						Control.getInstance().showErrorMessage("[ERROR] The grammar of the sentence is not correct!");
					}
				}
			}
		}
		return synResult;
	}

	/**
	 * Noisy text cleaning
	 * Rule-based method
	 * It uses regexp to replace and remove noisy text
	 * @param sentence
	 * @return
	 */
	private String clearSentence(String sentence) {
		// (\(LC\))$
		sentence = sentence.replaceAll("^\t", "");
		sentence = sentence.replaceAll("\\.(\\s)?(\\((\\w|\\W)+\\))$", ".");
		sentence = sentence.replaceAll("^(\\s)?(\\((\\w|\\W)+\\))$", ".");
		// ^(\(?[a-zA-Z](\)|\.)(\t)?)|^(\(?\d+((\.\d+)+)?(\)|\.)?(\t)?)|^[\u2022,\u2023,\u25E6,\u2043,\u2219,\W]+(\s|\t)?
		sentence = sentence.replaceAll(
				"^(\\(?[a-zA-Z](\\)|\\.)(\\t)?)|^(\\(?\\d+((\\.\\d+)+)?(\\)|\\.)?(\\t)?)|^[\\u2022,\\u2023,\\u25E6,\\u2043,\\u2219,\\W]+(\\s|\\t)?",
				"");
		//
		sentence = sentence.replaceAll("^(\\(?(ix|iv|v?i{1,3}|x?i{1,3})\\)?)(?![a-zA-Z]).?(\\t|\\s)?", "");
		// |^(RBC)\s\d+(\.)?\s?
		sentence = sentence.replaceAll(
				"^(NOTE)\\s\\d+(\\.)?\\s?|^(RBC)\\s\\d+(\\.)?\\s?|^(OBU)\\s\\d+(\\.)?\\s?|^(EA)\\s\\d+(\\.)?\\s?|^(CE)\\s\\d+(\\.)?\\s?|^(GEN)\\s\\d+(\\.)?\\s?|^(LED)\\s\\d+(\\.)?\\s?",
				"");
		// \\\/
		sentence = sentence.replaceAll("\\/", " / ");

		// \w+('s)(?!\w)
		sentence = sentence.replaceAll("('s)(?!\\w)", " 's");
		// parentheses, quotation marks
		sentence = sentence.replaceAll("\\(", " ( ").replaceAll("\\)", " ) ").replaceAll("[\"“”]", " \" ");
		// \.(\s) -> used for separate the end point if the sentence detection don't
		// split the phrase correctly
		sentence = sentence.replaceAll("\\.(\\s)", " . ");
		sentence = sentence.replaceAll("\\s+", " ");
		
		// Check the endpoint of the sentence
		if (sentence.length() > 1) {
			if (sentence.substring(sentence.length() - 1).equals(";")
					|| sentence.substring(sentence.length() - 1).equals(",")
					|| sentence.substring(sentence.length() - 1).equals(":")) {
				sentence = sentence.substring(0, sentence.length() - 1);
			}
			if (!sentence.substring(sentence.length() - 1).equals(".")) {
				sentence = sentence + ".";
			}
		}
		return sentence;
	}
	
	/**
	 * Tokenization (OpenNLP)
	 * @param requirmenet
	 * @return
	 * @throws IOException
	 */
	public String[] tokenization(String requirmenet) throws IOException {
		InputStream inputFile = null;
		inputFile = tokenPath.getInputStream();
		TokenizerModel model = new TokenizerModel(inputFile);
		Tokenizer tokenizer = new TokenizerME(model);
		return tokenizer.tokenize(requirmenet);
	}

	/**
	 * Sentence Boundary Disambiguation (SBD) (openNLP)
	 * @param sentence
	 * @return
	 * @throws IOException
	 */
	public String[] sentenceDetection(String sentence) throws IOException {
		String[] sentences = null;
		// Loading sentence detector model
		InputStream inputStream = null;
		if (sentenceDetector == null) {
			inputStream = sentPath.getInputStream();
		}
		try {
			if (sentenceDetector == null) {
				SentenceModel model = new SentenceModel(inputStream);

				// Instantiating the SentenceDetectorME class
				sentenceDetector = new SentenceDetectorME(model);
			}
			// Detecting the sentence
			sentences = sentenceDetector.sentDetect(sentence);
		} catch (IOException e) {
			Control.getInstance().showErrorMessage(e.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					Control.getInstance().showErrorMessage(e.getMessage());
				}
			}
		}
		return sentences;
	}

	/**
	 * Used to set the requirement text into the pipeline
	 * @param aEngine
	 * @param aLanguage
	 * @param aText
	 * @return
	 * @throws UIMAException
	 */
	public static JCas runParser(AnalysisEngine aEngine, String aLanguage, String aText) throws UIMAException {

		JCas jcas = aEngine.newJCas();

		jcas.setDocumentLanguage(aLanguage);

		TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
		tb.buildTokens(jcas, aText);

		aEngine.process(jcas);

		return jcas;
	}

	/**
	 * Lemmatization engine (clearNLP)
	 * @param term
	 * @return
	 * @throws UIMAException
	 */
	public String lemmatization(String term) throws UIMAException {
		if (lemmaEngine == null && tagger == null && lemma == null) {

			tagger = createEngineDescription(ClearNlpPosTagger.class);
			lemma = createEngineDescription(ClearNlpLemmatizer.class);

			lemmaEngine = createEngine(createEngineDescription(tagger, lemma));
		}
		JCas jcas = runParser(lemmaEngine, "en", term);
		Collection<Lemma> lemmas = JCasUtil.select(jcas, Lemma.class);
		String ret = "";
		String[] terms = term.split(" ");
		int i = 0;
		if (!lemmas.isEmpty()) {
			for (Lemma l : lemmas) {
				if (!l.getValue().matches("\\d+")) {
					if (!ret.equals(""))
						ret = ret.concat(" " + l.getValue());
					else
						ret = l.getValue();
				} else {
					if (!ret.equals(""))
						ret = ret.concat(" " + terms[i]);
					else
						ret = terms[i];
				}
				i++;
			}
		}
		return ret;
	}

	/**
	 * Dependency parser engine (clearNLP)
	 * This function generates a dependency tree from the dependency parser results.
	 * @param aText
	 * @return
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 * @throws IOException
	 */
	public Node dependencyParser(String aText) throws ResourceInitializationException, UIMAException, IOException {
		if (parserEngine == null) {
			parserEngine = createEngine(createEngineDescription(createEngineDescription(tagger, lemma),
					createEngineDescription(ClearNlpParser.class)));
		}
		JCas jcas = runParser(parserEngine, "en", aText);
		Node root = null;
		ArrayList<Node> dependencyTree = new ArrayList<>();
		Collection<Dependency> deps = JCasUtil.select(jcas, Dependency.class);
		if (!deps.isEmpty()) {
			for (Dependency d : deps) {
				Node node = new Node(d.getDependent().getBegin(), d.getGovernor().getBegin(),
						d.getDependent().getPosValue(), d.getDependencyType(), d.getDependent().getCoveredText(),
						d.getDependent().getLemmaValue(), d);
				dependencyTree.add(node);
			}

			root = fillTreeLinks(dependencyTree);
		}
		return root;
	}

	/**
	 * Update the tree information
	 * @param tree
	 * @return
	 */
	private Node fillTreeLinks(ArrayList<Node> tree) {
		Node root = null;
		for (Node n : tree) {
			if (n.getParentId() > n.getId()) {
				int pIdx = findParent(tree, n.getParentId(), tree.indexOf(n) + 1, n.getParentId() > n.getId());
				n.setParentNode(tree.get(pIdx));
				tree.get(pIdx).addSonNodes(n);

			} else if (n.getParentId() < n.getId()) {
				int pIdx = findParent(tree, n.getParentId(), tree.indexOf(n) - 1, n.getParentId() > n.getId());
				n.setParentNode(tree.get(pIdx));
				tree.get(pIdx).addSonNodes(n);
			} else {
				root = n;
			}
		}
		return root;
	}

	/**
	 * Find the parent of the node from the dependncy parser results
	 * @param tree
	 * @param parentId
	 * @param idx
	 * @param next
	 * @return
	 */
	private int findParent(ArrayList<Node> tree, int parentId, int idx, boolean next) {
		boolean find = false;
		while (!find) {
			if (tree.get(idx).getId() == parentId) {
				find = true;
			} else if (next) {
				idx++;
			} else {
				idx--;
			}
		}
		return idx;
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
	}
}