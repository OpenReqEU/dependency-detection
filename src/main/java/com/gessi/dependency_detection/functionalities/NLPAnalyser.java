package com.gessi.dependency_detection.functionalities;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import dkpro.similarity.algorithms.lsr.LexSemResourceComparator;
import dkpro.similarity.algorithms.lsr.path.WuPalmerComparator;

//import org.apache.uima.UIMAException;
//import org.apache.uima.resource.ResourceInitializationException;
//import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
//import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
//import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import org.springframework.core.io.ClassPathResource;

import com.gessi.dependency_detection.components.Node;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
//import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.apache.uima.fit.pipeline.SimplePipeline.*;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.*;

import de.tudarmstadt.ukp.dkpro.core.clearnlp.ClearNlpLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.clearnlp.ClearNlpParser;
import de.tudarmstadt.ukp.dkpro.core.clearnlp.ClearNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Writer;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.testing.TestRunner;
import de.tudarmstadt.ukp.dkpro.core.testing.dumper.DependencyDumper;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class NLPAnalyser {

	private static ClassPathResource parse_path = new ClassPathResource("en-parser-chunking.bin");
	private static ClassPathResource pos_path = new ClassPathResource("en-pos-maxent.bin");
	private static ClassPathResource sent_path = new ClassPathResource("en-sent.bin");
	private static ClassPathResource lemma_path = new ClassPathResource("en-lemmatizer.txt");
	private static ClassPathResource token_path = new ClassPathResource("en-token.bin");

	private static POSModel posModel = null;
	private static SentenceDetectorME sentenceDetector = null;
	private static LexSemResourceComparator comparatorWN = null;

	private static AnalysisEngineDescription tagger = null;
	private static AnalysisEngineDescription lemma = null;
	private static AnalysisEngine parserEngine = null;
	private static AnalysisEngine LemmaEngine = null;
	private static LexicalSemanticResource wordnet;

	/**
	 * Constructor
	 * @throws IOException
	 */
	public NLPAnalyser() throws IOException {
		super();
		try {
			wordnet = ResourceFactory.getInstance().get("wordnet", "en");
			wordnet.setIsCaseSensitive(false);
		} catch (ResourceLoaderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public ArrayList<Node> RequirementAnalisis(String requirement)
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
									reqSent = reqSent + " " + tokens[i].substring(0, tokens[i].length() - 1) + " .";
								} else {
									reqSent = reqSent + " " + tokens[i];
								}
							}

							// PoSTagg & Parser & Lemma
							Node root = dependencyParser(reqSent);
							// Information Extraction (IE)
							DependencyTreeIE ie = new DependencyTreeIE(root);
							synResult.addAll(ie.applyIE());
						}
					} catch (NullPointerException e) {
						System.out.println("[ERROR] The grammar of the sentence is not correct!");
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

//	/**
//	 * Syntactic analysis
//	 * @param requirement
//	 * @return
//	 * @throws IOException
//	 */
//	public ArrayList<String> syntaxNLPAnalysis(String requirement) throws IOException {
//		// System.out.println("Tokens: ");
//		// tokens = tokenization(requirement);
//		// for (String t : tokens) {
//		// System.out.print(t + ", ");
//		// }
//		//
//		// System.out.println("\n\nPOS-tagging: ");
//		// String[] tags = taggingPOS(tokens);
//		// for (String t : tags) {
//		// System.out.print(t + ", ");
//		// }
//
//		// System.out.print("\nParsing: ");
//		if (requirement.substring(requirement.length() - 1).equals(".")
//				|| requirement.substring(requirement.length() - 1).equals(";")
//				|| requirement.substring(requirement.length() - 1).equals(",")
//				|| requirement.substring(requirement.length() - 1).equals(":")) {
//			requirement = requirement.substring(0, requirement.length() - 1);
//		}
//		Parse[] parses = parsing(requirement);
//		// FileWriter outputFileWriter = new FileWriter(new
//		// File("./outData/parsetree.txt"));
//		StringBuffer parseString = new StringBuffer();
//		for (Parse p : parses) {
//
//			// pass this referece into the show method
//			p.show(parseString);
//			// outputFileWriter.write(parseString.toString().replaceAll("\\(",
//			// "[").replaceAll("\\)", "]"));
//			// outputFileWriter.flush();
//			System.out.println(parseString.toString().replaceAll("\\(", "[").replaceAll("\\)", "]"));
//		}
//
//		// System.out.println("\nIE: ");
//		ArrayList<String> topw = ie(parses);
//
//		ArrayList<String> topWords = new ArrayList<>();
//		ArrayList<String> tags = new ArrayList<>();
//		for (String s : topw) {
//			String[] words = s.split(",");
//			for (String w : words) {
//				if (!w.equals("")) {
//					tags.add(w.split("_")[1]);
//					topWords.add(w.split("_")[0].replaceAll(";|,|:|\\.", ""));
//				}
//			}
//		}
//
//		// ArrayList<String> topw_stem = stemming(topWords);
//
//		String[] tokensArray = topWords.toArray(new String[0]);
//		String[] tagsArray = tags.toArray(new String[0]);
//
//		String[] topw_lemm = lemmatize(tokensArray, tagsArray);
//
//		// System.out.print("\nTopWords:");
//		// for (String s : topw) {
//		// System.out.print(s + ", ");
//		// }
//
//		// System.out.print("\nStemming:");
//		// for (String s : topw_stem) {
//		// System.out.print(s + ", ");
//		// }
//
//		// System.out.print("\nLemmatizing:");
//		for (int i = 0; i < topw_lemm.length; i++) {
//			// System.out.print(topw_lemm[i] + ", ");
//			if (!topw_lemm[i].equals("O")) {
//				topWords.add(topw_lemm[i]);
//			}
//		}
//
//		// System.out.println("\n");
//
//		return topWords;
//
//		// for (Parse p : topParses) {
//		// p.showCodeTree();
//		// }
//	}

	// public void semanticNLPAnalysis(String requirement) throws IOException {
	// ArrayList<String> sWordsList = stopWordsRemoval(tokens);
	//// for (String str : sWordsList) {
	//// System.out.print(str + " ");
	//// }
	//
	// ArrayList<String> steamList = stemming(sWordsList);
	//// for (String str : steamList) {
	//// System.out.print(str + " ");
	//// }
	//
	// String[] lemmaList = lemmatize(sWordsList);
	//// for (String str : lemmaList) {
	//// System.out.print(str + " ");
	//// }
	//
	// }

	
	/**
	 * Tokenization (OpenNLP)
	 * @param requirmenet
	 * @return
	 * @throws IOException
	 */
	public String[] tokenization(String requirmenet) throws IOException {
		// SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		//
		// // Tokenizing the given sentence
		// String tokens[] = tokenizer.tokenize(requirmenet);
		InputStream inputFile = null;
		inputFile = token_path.getInputStream();
//		InputStream inputFile = new FileInputStream(TOKEN_PATH.toString());
		TokenizerModel model = new TokenizerModel(inputFile);
		Tokenizer tokenizer = new TokenizerME(model);
		String tokens[] = tokenizer.tokenize(requirmenet);
		return tokens;
	}

	/**
	 * Sentence Boundary Disambiguation (SBD) (openNLP)
	 * @param sentence
	 * @return
	 * @throws IOException
	 */
	public String[] sentenceDetection(String sentence) throws IOException {
		String sentences[] = null;
		// Loading sentence detector model
		// ClassPathResource sent_path = new ClassPathResource("en-sent.bin");
		InputStream inputStream = null;
		if (sentenceDetector == null) {
			inputStream = sent_path.getInputStream();
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
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sentences;
	}

	// public ArrayList<String> stopWordsRemoval(String[] tokens) {
	// ArrayList<String> wordsList = new ArrayList<String>();
	// ArrayList<String> words = new ArrayList<String>();
	// for (String t : tokens) {
	// words.add(t);
	// }
	// // ArrayList<String> stemTokens = stemming(words);
	// for (String token : words) {
	// String wordCompare = token.toLowerCase();
	// if (!stopWords.contains(wordCompare)) {
	// wordsList.add(wordCompare);
	// }
	// }
	//
	// return wordsList;
	// }

	// private ArrayList<String> stemming(ArrayList<String> words) {
	// ArrayList<String> steamList = new ArrayList<String>();
	// PorterStemmer porterStemmer = new PorterStemmer();
	//
	// for (String word : words) {
	// steamList.add(porterStemmer.stem(word));
	// }
	// // words.forEach(word -> {
	// // String stemmed = porterStemmer.stem(word);
	// // logger.info("{} -> {}", stemmed, word);
	// // });
	// return steamList;
	// }

//	/**
//	 * POS tagger and Lemmatization from clearNLP (extracted at the same time)
//	 * @param words
//	 * @return
//	 * @throws IOException
//	 */
//	public String[] POSLemmatization(String[] words) throws IOException {
//		String[] tags = taggingPOS(words);
//		String[] lemmas = lemmatize(words, tags);
//		return lemmas;
//	}

//	private String[] lemmatize(String[] tokens, String[] tags) throws IOException {
//		String[] lemmas = null;
//		// InputStream inputFile = new FileInputStream(LEMMA_PATH.toString());
//		InputStream inputFile = null;
////	if (dictionaryLemmatizer == null) {
//		inputFile = lemma_path.getInputStream();
////	}
//		try {
////	    if (dictionaryLemmatizer == null) {
//			DictionaryLemmatizer dictionaryLemmatizer = new DictionaryLemmatizer(inputFile);
////	    }
//			// String[] tokensSArray = tokens.toArray(new String[0]);
//			// String[] tagsArray = tags.toArray(new String[0]);
//			// String[] tags = taggingPOS(tokensSArray);
//			// System.out.print("Tags: ");
//			// for (int i = 0; i < tokensSArray.length; i++) {
//			// System.out.print(tokensSArray[i] + " - " + tags.get(i) + ",");
//			// }
//			// System.out.println();
//			lemmas = dictionaryLemmatizer.lemmatize(tokens, tags);
//			for (int i = 0; i < lemmas.length; i++) {
//				if (lemmas[i].equals("O"))
//					lemmas[i] = tokens[i].toLowerCase();
//				else
//					lemmas[i] = lemmas[i].toLowerCase();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (inputFile != null) {
//				try {
//					inputFile.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		// LemmatizerModel model = null;
//		//
//		// InputStream modelIn = lemma_path.getInputStream();
//		// model = new LemmatizerModel(modelIn);
//		//
//		// LemmatizerME lemmatizer = new LemmatizerME(model);
//		// String[] tokensSArray = tokens.toArray(new String[0]);
//		// String[] lemmas = lemmatizer.lemmatize(tokensSArray,
//		// taggingPOS(tokensSArray));
//		return lemmas;
//	}
//
//	private String[] taggingPOS(String[] tokens) throws IOException {
//		// POSModel modelPOS = null;
//		POSTaggerME tagger = null;
//		String[] tags = null;
//
//		// POS Model Loading
//		try {
//			// File inputFile = null;
//			// if (new File("src/main/resources/en-pos-maxent.bin").exists()) {
//			// inputFile = new File("src/main/resources/en-pos-maxent.bin");
//			// } else {
//			// System.out.println("File : " + "src/main/resources/en-pos-maxent.bin" + "
//			// does not exists.");
//			// }
//			if (posModel == null) {
//				InputStream inputStream = pos_path.getInputStream();
//				File inputFile = File.createTempFile("upload-dir/documents/posTag", ".bin");
//				try {
//					FileUtils.copyInputStreamToFile(inputStream, inputFile);
//				} finally {
//					IOUtils.closeQuietly(inputStream);
//				}
//
//				if (inputFile != null) {
//					posModel = new POSModelLoader().load(inputFile);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		// POS Tagging
//		try {
//			if (posModel != null) {
//				tagger = new POSTaggerME(posModel);
//				if (tagger != null) {
//					tags = tagger.tag(tokens);
//
//					// Instantiating the POSSample class (for print the tagged sentence)
////					POSSample sample = new POSSample(tokens, tags);
//					// System.out.println(sample.toString());
//
//					// Probabilities for each tag of the last tagged sentence.
////					double[] probs = tagger.probs();
//					// for (double p : probs) {
//					// System.out.println(p);
//					// }
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return tags;
//	}

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

		TokenBuilder<Token, Sentence> tb = new TokenBuilder<Token, Sentence>(Token.class, Sentence.class);
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
		if (LemmaEngine == null && tagger == null && lemma == null) {

			tagger = createEngineDescription(ClearNlpPosTagger.class);
			lemma = createEngineDescription(ClearNlpLemmatizer.class);

			LemmaEngine = createEngine(createEngineDescription(tagger, lemma));
		}
		JCas jcas = runParser(LemmaEngine, "en", term);
		Collection<Lemma> lemmas = JCasUtil.select(jcas, Lemma.class);
		String ret = "";
		String[] terms = term.split(" ");
		int i = 0;
		if (!lemmas.isEmpty()) {
			for (Lemma l : lemmas) {
				if (!l.getValue().matches("\\d+")) {
					if (!ret.equals(""))
						ret = ret + " " + l.getValue();
					else
						ret = l.getValue();
				} else {
					if (!ret.equals(""))
						ret = ret + " " + terms[i];
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
					createEngineDescription(ClearNlpParser.class/* , ClearNlpParser.PARAM_PRINT_TAGSET, false */)
			/* ,createEngineDescription(DependencyDumper.class) */));
		}
//		return TestRunner.runTest(parserEngine, "en", aText);
		JCas jcas = runParser(parserEngine, "en", aText);
		Node root = null;
		ArrayList<Node> dependencyTree = new ArrayList<>();
		Collection<Dependency> deps = JCasUtil.select(jcas, Dependency.class);
		if (!deps.isEmpty()) {
			for (Dependency d : deps) {
//					if (d.getDependent().getCoveredText().equals("#")) {
//						System.out.println();
//						System.out.println("New_req:");
//						continue;
//					}

				Node node = new Node(d.getDependent().getBegin(), d.getGovernor().getBegin(),
						d.getDependent().getPosValue(), d.getDependencyType(), d.getDependent().getCoveredText(),
						d.getDependent().getLemmaValue(), d);
				dependencyTree.add(node);
//				System.out.print("pos: " + d.getDependent().getPosValue() + "  \tname: "+ d.getDependent().getCoveredText() + "\tdep: " + d.getGovernor().getCoveredText());
//				System.out.print("\ttyp: " + d.getDependencyType() + "\tlemma: " + d.getDependent().getLemmaValue());
//				System.out.println("\td_idx: " + d.getDependent().getBegin() + "\t g_idx: " + d.getGovernor().getBegin());

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
//		int idx = tree.indexOf(n)+1;
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

//	private Parse[] parsing(String requirement) throws IOException {
//		Parse[] parses = null;
//		// ClassPathResource parse_path = new
//		// ClassPathResource("en-parser-chunking.bin");
//		// Loading parser model
//		InputStream inputFile = null;
////	if (parser == null) {
//		inputFile = parse_path.getInputStream();
////	}
//		try {
////	    if (parser == null) {
//			ParserModel model = new ParserModel(inputFile);
//
//			// try (InputStream modelIn = pos_path.getInputStream()) {
//			// POSModel posModel = new POSModel(modelIn);
//			// POSTaggerFactory taggerFac = new POSTaggerFactory();
//			//
//			//
//			// }
//
//			// Creating a parser
//			Parser parser = ParserFactory.create(model);
////	    }
//			// Parsing the sentence
//			parses = ParserTool.parseLine(requirement, parser, 1);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			if (inputFile != null) {
//				try {
//					inputFile.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return parses;
//	}

	/**
	 * Semantic similarity engine (DKPRO & WordNet)
	 * @param term1
	 * @param term2
	 * @return
	 * @throws dkpro.similarity.algorithms.api.SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	public double semanticSimilarity(String term1, String term2)
			throws dkpro.similarity.algorithms.api.SimilarityException, LexicalSemanticResourceException {

//        Set<Entity> entities1 = wordnet.getEntity("jewel");
//        Set<Entity> entities2 = wordnet.getEntity("treasure");

		if (comparatorWN == null)
			comparatorWN = new WuPalmerComparator(wordnet, wordnet.getRoot());
		return comparatorWN.getSimilarity(term1, term2);

	}

//	public ArrayList<String> ie(Parse[] parses) {
//		InformationExtraction ie = new InformationExtraction();
//		return ie.applyInfoExtraction(parses[0].getChildren()[0]);
//	}

	/**
	 * Debug
	 * Utility to read a file
	 * @param path
	 * @return
	 */
	public ArrayList<String> readFile(String path) {
		ArrayList<String> fileLines = new ArrayList<String>();
		BufferedReader br = null;
		FileReader fr = null;

		try {
			// br = new BufferedReader(new FileReader(FILENAME));
			fr = new FileReader(path);
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				fileLines.add(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				if (br != null)
					br.close();
				if (fr != null)
					fr.close();

			} catch (IOException ex) {
				ex.printStackTrace();

			}
		}
		return fileLines;
	}

	/**
	 * Debug
	 * Utility to write a file
	 * @param textLines
	 * @param path
	 * @throws IOException
	 */
	public void writeFile(ArrayList<Object> textLines, String path) throws IOException {
		int index = 0;
		// path = "./outData/malletResults.txt";
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		for (Object str : textLines) {
			writer.write(str + "\n");
			index++;
		}
		writer.close();
	}

	// private double tf(List<String> doc, String term) {
	// double result = 0;
	// for (String word : doc) {
	// if (term.equalsIgnoreCase(word))
	// result++;
	// }
	// return result / doc.size();
	// }
	//
	// private double idf(List<List<String>> docs, String term) {
	// double n = 0;
	// for (List<String> doc : docs) {
	// for (String word : doc) {
	// if (term.equalsIgnoreCase(word)) {
	// n++;
	// break;
	// }
	// }
	// }
	// return Math.log(docs.size() / n);
	// }
	//
	// public double tfIdf(List<String> doc, List<List<String>> docs, String term) {
	// return tf(doc, term) * idf(docs, term);
	// }
	//
	// public ArrayList<Formatter> malletTopicModeling(String path) throws Exception
	// {
	// TopicModel tm = new TopicModel();
	// return tm.lda(path, STOPWORDS_PATH);
	// }
	//
	// public void lda_lda() {
	// List<TupleTwo<Doc, Double>> topStrings = null;
	// ArrayList<String> textLines = readFile(DATA_PATH);
	// int index = 0;
	//
	// Lda method = new Lda();
	// method.setTopicCount(1);
	// method.setMaxVocabularySize(20000);
	// for (String line : textLines) {
	// List<String> l = new ArrayList<String>();
	// l.add(line);
	// LdaResult result = method.fit(l);
	//
	// // System.out.println("Topic Count: " + result.topicCount());
	//
	// for (int topicIndex = 0; topicIndex < result.topicCount(); ++topicIndex) {
	// String topicSummary = result.topicSummary(0);
	// List<TupleTwo<String, Integer>> topKeyWords = result.topKeyWords(topicIndex,
	// 5);
	// topStrings = result.topDocuments(topicIndex, 5);
	//
	//// System.out.println("Topic #" + (index + 1) + ": " + topicSummary);
	//
	//// System.out.print("Keywords: ");
	// for (TupleTwo<String, Integer> entry : topKeyWords) {
	// String keyword = entry._1();
	// int score = entry._2();
	// System.out.print(keyword + "(" + score + "), ");
	// }
	//
	// System.out.println();
	//
	// for (TupleTwo<Doc, Double> entry : topStrings) {
	// double score = entry._2();
	// int docIndex = entry._1().getDocIndex();
	// String docContent = entry._1().getContent();
	// System.out.println("Doc (" + docIndex + ", " + score + ")): " + docContent);
	// }
	// }
	// index++;
	// System.out.println();
	// }
	// }

	// public void findSynonyms() {
	//
	// String a[] = new String[2];
	//
	// int j = 0;
	// while (j < 2) {
	//
	// System.setProperty("wordnet.database.dir", "C:\\Program
	// Files\\WordNet\\2.1\\dict");
	// NounSynset nounSynset;
	// NounSynset[] hyponyms;
	// WordNetDatabase database = WordNetDatabase.getFileInstance();
	// Synset[] synsets = database.getSynsets(a[j], SynsetType.NOUN);
	// System.out.println("*********************************************");
	// for (int i = 0; i < synsets.length; i++) {
	// nounSynset = (NounSynset) (synsets[i]);
	// hyponyms = nounSynset.getHyponyms();
	//
	// System.err.println(nounSynset.getWordForms()[0] + ": " +
	// nounSynset.getDefinition() + ") has "
	// + hyponyms.length + " hyponyms");
	//
	// }
	// j++;
	// }
	// System.out.println("*********************************************");
	// }

}