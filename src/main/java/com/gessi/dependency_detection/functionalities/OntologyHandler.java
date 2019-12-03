package com.gessi.dependency_detection.functionalities;

import java.io.IOException;
import java.util.*;

import com.gessi.dependency_detection.WordEmbedding;
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
	 * @param analizer
	 * @throws IOException
	 * @throws UIMAException
	 * @return
	 */
	public int searchClasses(NLPAnalyser analizer) throws IOException, UIMAException {
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

	private String[] extractLemmas(String ontTerm) throws IOException {
		TextPreprocessing textPreprocessing=new TextPreprocessing();
		String l=textPreprocessing.text_preprocess(ontTerm);
		RAKEKeywordExtractor rake=new RAKEKeywordExtractor();
		List<String> resAnalysis=rake.RAKEanalyzeNoStopword(l);
		String[] res=new String[resAnalysis.size()];
		return  resAnalysis.toArray(res);
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
	private boolean extractNGram(String node, String[] lemmas, boolean syny,double thr,WordEmbedding wordEmbedding) throws SimilarityException, LexicalSemanticResourceException, IOException {
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

	private boolean isSynonym(Set<String> requirementLemmas, Set<String> ontologyLemmas,double thr,WordEmbedding wordEmbedding) throws IOException {
		boolean isSynonym=true;
		for (String s: ontologyLemmas) {
			boolean synonymExists=false;
			for (String l:requirementLemmas) {
				if (wordEmbedding.computeSimilarity(s,l)>=thr) {
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
	public void matching(String keywords, String reqId, String requirement, boolean syny,double thr,WordEmbedding wordEmbedding) throws IOException, SimilarityException, LexicalSemanticResourceException {
		ArrayList<OntClass> classes = new ArrayList<>();
		String[] lemmas;
		for (int j = 0; j < ontClasses.size(); j++) {
		    lemmas = classesLemmas.get(j);
		    if (keywords.split(" ").length >= lemmas.length && extractNGram(keywords, lemmas, syny,thr,wordEmbedding)) {
				System.out.println("A MATCH WAS MADE BETWEEN:");
				System.out.println("REQUIREMENT KEYWORDS: "+keywords);
				System.out.println("ONTOLOGY NAME: "+lemmas.toString());

				classes.add(ontClasses.get(j));
			}
		}

		// Requirement instantiation within the ontology
		for (OntClass cls : classes) {
			System.out.println("A MATCH WAS MADE");
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
										dependencies.add(newDep);
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

}
