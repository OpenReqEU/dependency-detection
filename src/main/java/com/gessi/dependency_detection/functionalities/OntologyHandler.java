package com.gessi.dependency_detection.functionalities;

import java.io.IOException;
import java.util.*;

import com.gessi.dependency_detection.WordEmbedding;
import com.gessi.dependency_detection.components.Node;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.S;
import org.apache.log4j.varia.NullAppender;
import org.apache.uima.UIMAException;

import com.gessi.dependency_detection.components.Dependency;
import com.gessi.dependency_detection.components.DependencyType;
import com.gessi.dependency_detection.components.Status;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import dkpro.similarity.algorithms.api.SimilarityException;

public class OntologyHandler {

	private OntModel model;
	private String source;
	private ArrayList<OntClass> ontClasses;
	private ArrayList<String[]> classesWords;
	private ArrayList<String[]> classesLemmas;
	private HashMap<String, ArrayList<String>> synonyms = new HashMap<>();
	private HashMap<String, ArrayList<String>> noSynonyms = new HashMap<>();

	/**
	 * Constructor
	 */
	public OntologyHandler() {
		org.apache.log4j.BasicConfigurator.configure(new NullAppender());
	}

	/**
	 * Load an ontology
	 * 
	 * @param source
	 * @param path
	 * @throws IOException
	 */
	public void loadOnt(String source, String path) throws IOException {
		// create the base model
		this.source = source;
		this.model = ModelFactory.createOntologyModel();
		this.model.read(path, "RDF/XML");
	}

	/**
	 * Analyse ontology classes and extract its infromation (terms, lemmas)
	 * 
	 * @throws IOException
	 * @throws UIMAException
	 * @return
	 */
	public int searchClassesTfIdfBased() throws IOException, UIMAException {
		ontClasses = new ArrayList<>();
		classesWords = new ArrayList<>();
		classesLemmas = new ArrayList<>();
		int max=1;
		ExtendedIterator<?> rootClasses = this.model.listClasses();
		while (rootClasses.hasNext()) {
			OntClass thisClass = (OntClass) rootClasses.next();

			if (thisClass.getLocalName() != null) {
				String ontTerm = "";
				String[] words = thisClass.getLocalName()
						.split("_|\\s|(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])|(?<!(^|[A-Z]))(?=\\d)");
				for (int i = 0; i < words.length; i++) {
					if (!ontTerm.equals("")) {
						words[i] = words[i].toLowerCase();
						ontTerm = ontTerm.concat(" " + words[i]);
					} else {
						words[i] = words[i].toLowerCase();
						ontTerm = words[i];
					}
				}
				String[] lemmas = extractLemmas(ontTerm);
				ontClasses.add(thisClass);
				classesWords.add(words);
				if (words.length>max) max=words.length;
				classesLemmas.add(lemmas);
				for (int i = 0; i < lemmas.length; i++) {
					synonyms.put(lemmas[i], new ArrayList<>());
					noSynonyms.put(lemmas[i], new ArrayList<>());
				}
			}
		}
		return max;
	}

	/**
	 * Analyse ontology classes and extract its infromation (terms, lemmas)
	 *
	 * @param analizer
	 * @throws IOException
	 * @throws UIMAException
	 */
	public void searchClasses(NLPAnalyser analizer) throws IOException, UIMAException {
		ontClasses = new ArrayList<>();
		classesWords = new ArrayList<>();
		classesLemmas = new ArrayList<>();
		ExtendedIterator<?> rootClasses = this.model.listClasses();
		while (rootClasses.hasNext()) {
			OntClass thisClass = (OntClass) rootClasses.next();

			if (thisClass.getLocalName() != null) {
				String ontTerm = "";
				String[] words = thisClass.getLocalName()
						.split("_|\\s|(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])|(?<!(^|[A-Z]))(?=\\d)");
				for (int i = 0; i < words.length; i++) {
					if (!ontTerm.equals("")) {
						words[i] = words[i].toLowerCase();
						ontTerm = ontTerm.concat(" " + words[i]);
					} else {
						words[i] = words[i].toLowerCase();
						ontTerm = words[i];
					}
				}
				String[] lemmas = extractLemmas(ontTerm, analizer);

				ontClasses.add(thisClass);
				classesWords.add(words);
				classesLemmas.add(lemmas);
				for (int i = 0; i < lemmas.length; i++) {
					synonyms.put(lemmas[i], new ArrayList<>());
					noSynonyms.put(lemmas[i], new ArrayList<>());
				}
			}
		}
	}


	private String[] extractLemmas(String ontTerm) throws IOException {
		TextPreprocessing textPreprocessing=new TextPreprocessing();
		String l=textPreprocessing.text_preprocess(ontTerm);
		RAKEKeywordExtractor rake=new RAKEKeywordExtractor();
		List<String> resAnalysis=rake.RAKEanalyzeNoStopword(l);
		String[] res=new String[resAnalysis.size()];
		return  resAnalysis.toArray(res);
	}
	private String[] extractLemmas(String words, NLPAnalyser analizer) throws IOException, UIMAException {
		String ontLemma = analizer.lemmatization(words);
		return ontLemma.split(" ");
	}




	/**
	 * Find if the set of words contains a correct n-gram that match with the
	 * ontology.
	 * 
	 * @param node
	 * @param lemmas
	 * @param syny
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean extractNGram(String node, String[] lemmas, boolean syny, Double thr,WordEmbedding wordEmbedding) throws SimilarityException, LexicalSemanticResourceException, IOException {
		String[] lemmasNode = node.split(" ");
		Set nodeSet=new HashSet(Arrays.asList(lemmasNode));
		Set lemmaSet=new HashSet(Arrays.asList(lemmas));
		if (syny) {
			return isSynonym(nodeSet,lemmaSet,thr,wordEmbedding);
		}
		else {
			return nodeSet.containsAll(lemmaSet);
		}
	}

	private boolean isSynonym(Set<String> requirementLemmas, Set<String> ontologyLemmas,Double thr,WordEmbedding wordEmbedding) throws IOException {
		boolean isSynonym=true;
		for (String s: ontologyLemmas) {
			boolean synonymExists=false;
			for (String l:requirementLemmas) {
				if (l.equals(s)||wordEmbedding.computeSimilarity(s,l)>=thr) {
					synonymExists=true;
					break;
				}
			}
			isSynonym=isSynonym&&synonymExists;
			if (!isSynonym) return false;
		}
		return true;
	}

	/**
	 * Analyze the potential term candidates extracted from the requirements (n-gram
	 * concepts), and store the requirement within the related ontology class if
	 * they matches with a concept of the ontology.
	 * 
	 * @param keywords
	 * @param reqId
	 * @param requirement
	 * @param syny
	 * @throws IOException
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	public void matching(String keywords, String reqId, String requirement, boolean syny, Double thr,WordEmbedding wordEmbedding) throws IOException, SimilarityException, LexicalSemanticResourceException {
		ArrayList<OntClass> classes = new ArrayList<>();
		String[] lemmas;
		for (int j = 0; j < ontClasses.size(); j++) {
		    lemmas = classesLemmas.get(j);
		    if (keywords.split(" ").length >= lemmas.length && extractNGram(keywords, lemmas, syny,thr,wordEmbedding)) {
				System.out.println("Requirement " + reqId + " contains class " + String.join(" ", lemmas));
				//System.out.println("REQUIREMENT KEYWORDS: "+keywords);
				//System.out.println("ONTOLOGY NAME: "+lemmas.toString());

				classes.add(ontClasses.get(j));
			}
		}

		// Requirement instantiation within the ontology
		for (OntClass cls : classes) {
			//System.out.println("A MATCH WAS MADE");
			Individual individual = this.model.createIndividual(this.source + ":" + reqId + "_" + cls.getLocalName(),
					cls);
			DatatypeProperty req = this.model.getDatatypeProperty(this.source + "#requirement");
			individual.setPropertyValue(req, this.model.createTypedLiteral(requirement));
			DatatypeProperty id = this.model.getDatatypeProperty(this.source + "#id");
			individual.setPropertyValue(id, this.model.createTypedLiteral(reqId));
			DatatypeProperty className = this.model.getDatatypeProperty(this.source + "#class");
			individual.setPropertyValue(className, this.model.createTypedLiteral(cls.getLocalName()));
		}
	}
	/**
	 * Analyze the potential term candidates extracted from the requirements (n-gram
	 * concepts), and store the requirement within the related ontology class if
	 * they matches with a concept of the ontology.
	 *
	 * @param topNodes
	 * @param reqId
	 * @param requirement
	 * @param analizer
	 * @param syny
	 * @param thr
	 * @throws IOException
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */

	public void matchingRuleBased(List<Node> topNodes, String reqId, String requirement, NLPAnalyser analizer, boolean syny,
						 Double thr) throws IOException, SimilarityException, LexicalSemanticResourceException {
		ArrayList<OntClass> classes = new ArrayList<>();
		String[] words;
		String[] lemmas;
		for (int i = 0; i < topNodes.size(); i++) {
			for (int j = 0; j < ontClasses.size(); j++) {
				words = classesWords.get(j);
				lemmas = classesLemmas.get(j);
				if (topNodes.get(i).getTerm().split(" ").length >= words.length && extractNGramRuleBased(topNodes.get(i), words, lemmas, analizer, syny, thr)) classes.add(ontClasses.get(j));
			}
		}

		// Requirement instantiation within the ontology
		for (OntClass cls : classes) {
			System.out.println("I WAS IN");
			Individual individual = this.model.createIndividual(this.source + ":" + reqId + "_" + cls.getLocalName(),
					cls);
			DatatypeProperty req = this.model.getDatatypeProperty(this.source + "#requirement");
			individual.setPropertyValue(req, this.model.createTypedLiteral(requirement));
			DatatypeProperty id = this.model.getDatatypeProperty(this.source + "#id");
			individual.setPropertyValue(id, this.model.createTypedLiteral(reqId));
			DatatypeProperty className = this.model.getDatatypeProperty(this.source + "#class");
			individual.setPropertyValue(className, this.model.createTypedLiteral(cls.getLocalName()));
		}
	}


	/**
	 * Analyze the ontology and extract dependencies
	 * @return
	 */
	public List<Dependency> ontConflictDetection() {
		List<Dependency> dependencies = new ArrayList<>();
		ExtendedIterator<?> rootClasses = this.model.listClasses();
		while (rootClasses.hasNext()) {
			OntClass thisClass = (OntClass) rootClasses.next();
			if (thisClass.getLocalName() != null) {
				List<List<Object>> depClasses = new ArrayList<>();
				for (Iterator<OntClass> supers = thisClass.listSuperClasses(); supers.hasNext();) {
					depClasses.add(displayType(supers.next()));
				}

				/* Create dependencies */
				List<String> from;
				List<String> to;
				if (!depClasses.isEmpty()) {
					from = findIndividuals(thisClass);
					for (List<Object> dep : depClasses) {
						if (!dep.isEmpty()) {
							to = findIndividuals((OntClass) dep.get(1));
							for (String f : from) {
								for (String t : to) {
									if (!f.equals(t)) {
										Dependency newDep = new Dependency(f, t, Status.PROPOSED,
												DependencyType.valueOf((String) dep.get(0).toString().toUpperCase()));
										if (!dependencies.contains(newDep)) dependencies.add(newDep);
									}
								}
							}
						}
					}

				}
			}
		}
		return dependencies;
	}

	/**
	 * Find requirements within the ontology class
	 * @param thisClass
	 * @return
	 */
	private List<String> findIndividuals(OntClass thisClass) {

		ArrayList<String> individualIds = new ArrayList<>();
		ExtendedIterator<?> instances = thisClass.listInstances();
		while (instances.hasNext()) {
			Individual thisInstance = (Individual) instances.next();

			DatatypeProperty className = this.model.getDatatypeProperty(this.source + "#class");
			String namemore = thisInstance.getPropertyValue(className).toString();
			String auxName = namemore.split("\\^\\^")[0];
			if (auxName.equals(thisClass.getLocalName())) {
				DatatypeProperty id = this.model.getDatatypeProperty(this.source + "#id");
				individualIds.add(thisInstance.getPropertyValue(id).toString().split("\\^\\^")[0]);
			}
		}
		return individualIds;
	}

	private List<Object> displayType(OntClass supClass) {
		if (supClass.isRestriction()) {
			return displayRestriction(supClass.asRestriction());
		}
		return new ArrayList<>();
	}

	private List<Object> displayRestriction(Restriction supRest) {
		if (supRest.isAllValuesFromRestriction()) {
			return displayRestriction(supRest.getOnProperty(), supRest.asAllValuesFromRestriction().getAllValuesFrom());
		} else if (supRest.isSomeValuesFromRestriction()) {
			return displayRestriction(supRest.getOnProperty(),
					supRest.asSomeValuesFromRestriction().getSomeValuesFrom());
		}
		return new ArrayList<>();
	}

	private List<Object> displayRestriction(OntProperty property, Resource constraint) {
		List<Object> result = new ArrayList<>();
		result.add(property.getLocalName());
		result.add(constraint);
		return result;
	}
	private boolean extractNGramRuleBased(Node node, String[] words, String[] lemmas, NLPAnalyser analizer, boolean syny,
								 Double thr) throws SimilarityException, LexicalSemanticResourceException {
		String[] termsNode = node.getTerm().split(" ");
		String[] lemmasNode = node.getLemma().split(" ");
		int n = words.length;
		Stack<String> ngramTerm = new Stack<>();
		Stack<String> ngramLemma = new Stack<>();

		return findPotentialNgram(0, 1, n, termsNode, lemmasNode, ngramTerm, ngramLemma, words, lemmas, analizer, syny, thr);
	}
	/**
	 * Find all the combinations of the n-gram to check if the req. concept matches
	 * with the ont. concept
	 *
	 * @param idx
	 * @param level
	 * @param n
	 * @param termsNode
	 * @param lemmasNode
	 * @param ngramTerm
	 * @param ngramLemma
	 * @param words
	 * @param lemmas
	 * @param analizer
	 * @param syny
	 * @param thr
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean findPotentialNgram(int idx, int level, int n, String[] termsNode, String[] lemmasNode,
									   Stack<String> ngramTerm, Stack<String> ngramLemma, String[] words, String[] lemmas, NLPAnalyser analizer,
									   boolean syny, Double thr) throws SimilarityException, LexicalSemanticResourceException {
		boolean find = false;
		for (int j = idx; j < termsNode.length && !find; j++) {
			ngramTerm.push(termsNode[j]);
			ngramLemma.push(lemmasNode[j]);
			if (level < n) {
				find = findPotentialNgram(j + 1, level + 1, n, termsNode, lemmasNode, ngramTerm, ngramLemma, words,
						lemmas, analizer, syny, thr);
			}
			if (level == n && isSameNgram(ngramTerm, ngramLemma, words, lemmas, analizer, syny, thr)) return true;
			ngramTerm.pop();
			ngramLemma.pop();
		}
		return find;
	}

	/**
	 * check if a ordered set of words is the same of the set of words of the
	 * ontology
	 *
	 * @param ngramTerm
	 * @param ngramLemma
	 * @param words
	 * @param lemmas
	 * @param analizer
	 * @param syny
	 * @param thr
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean isSameNgram(Stack<String> ngramTerm, Stack<String> ngramLemma, String[] words, String[] lemmas,
								NLPAnalyser analizer, boolean syny, Double thr)
			throws SimilarityException, LexicalSemanticResourceException {
		boolean find = false;
		ArrayList<Integer> idxOntLemmaAnalized = new ArrayList<>();
		ArrayList<Integer> idxReqLemmaAnalized = new ArrayList<>();
		for (int i = 0; i < ngramTerm.size(); i++) {
			if (!find && i > 0) {
				return false;
			}
			find = false;
			int j = 0;
			while (j < words.length && !find) {
				if (!idxOntLemmaAnalized.contains(j)
						&& isSameTerm(ngramTerm.get(i), ngramLemma.get(i), words[j], lemmas[j])) {
					find = true;
					idxReqLemmaAnalized.add(i);
					idxOntLemmaAnalized.add(j);
				}
				j++;
			}
		}

		// of it is not detected, check the synonymy
		if (!find && syny) {

			for (int i = 0; i < ngramLemma.size(); i++) {
				if (!idxReqLemmaAnalized.contains(i)) {
					if (!find && i > 0) {
						return false;
					}
					find = false;
					int j = 0;
					while (j < lemmas.length && !find) {
						if (!idxOntLemmaAnalized.contains(j)
								&& isSynonymRuleBased(ngramLemma.get(i), lemmas[j], analizer, thr)) {
							find = true;
							idxOntLemmaAnalized.add(j);
						}
						j++;
					}
				} else find = true;
			}
		}
		return find;
	}

	/**
	 * Check if the req. term match with the term of the ontology
	 *
	 * @param term
	 * @param lemma
	 * @param ontWord
	 * @param ontLemma
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean isSameTerm(String term, String lemma, String ontWord, String ontLemma)
			throws SimilarityException, LexicalSemanticResourceException {

		if (term.equalsIgnoreCase(ontWord))
			return true;
		if (lemma.equals(ontWord))
			return true;
		if (lemma.equals(ontLemma))
			return true;
		if (term.equalsIgnoreCase(ontLemma))
			return true;

		if (term.toLowerCase().matches(ontWord + "s|es"))
			return true;
		if (lemma.matches(ontWord + "s|es"))
			return true;
		if (lemma.matches(ontLemma + "s|es"))
			return true;
		if (term.toLowerCase().matches(ontLemma + "s|es"))
			return true;

		return false;
	}


	/**
	 * Check the similarity between two terms
	 *
	 * @param reqTerm
	 * @param ontLemma
	 * @param analizer
	 * @param thr
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean isSynonymRuleBased(String reqTerm, String ontLemma, NLPAnalyser analizer, Double thr)
			throws SimilarityException, LexicalSemanticResourceException {
		if (!ontLemma.matches("\\d+|\\W+")) {
			if (!synonyms.get(ontLemma).contains(reqTerm) && !noSynonyms.get(ontLemma).contains(reqTerm)) {
				if (analizer.semanticSimilarity(reqTerm, ontLemma) >= thr) {
					synonyms.get(ontLemma).add(reqTerm);

					return true;
				} else {
					noSynonyms.get(ontLemma).add(reqTerm);
				}
			} else if (synonyms.get(ontLemma).contains(reqTerm)) {
				return true;
			} else if (noSynonyms.get(ontLemma).contains(reqTerm)) {
				return false;
			}
		}
		return false;
	}

}
