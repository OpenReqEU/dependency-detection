package com.gessi.dependency_detection.functionalities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Appender;
import org.apache.log4j.varia.NullAppender;
import org.apache.uima.UIMAException;
import org.eclipse.rdf4j.rio.RDFFormat;

import com.gessi.dependency_detection.components.Dependency;
import com.gessi.dependency_detection.components.DependencyType;
import com.gessi.dependency_detection.components.Node;
import com.gessi.dependency_detection.components.Status;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StmtIteratorImpl;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.XSD;

import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import dkpro.similarity.algorithms.api.SimilarityException;

import com.hp.hpl.jena.util.iterator.Filter;

public class OntologyHandler {

	private OntModel model;
	private String source;
	private String name;
	private ArrayList<OntClass> ontClasses;
	private ArrayList<String[]> classesWords;
	private ArrayList<String[]> classesLemmas;
//	private final double THRESHOLD = 0.9;
	private HashMap<String, ArrayList<String>> synonyms = new HashMap<>();
	private HashMap<String, ArrayList<String>> noSynonyms = new HashMap<>();

	/**
	 * Constructor
	 */
	public OntologyHandler() {
		org.apache.log4j.BasicConfigurator.configure(new NullAppender());
		// org.apache.log4j.BasicConfigurator.configure( new NullAppender());

		// OntModel base = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

	}

//    public void loadOnt(String source) throws IOException {
//	// create the base model
//	String SOURCE = "http://www.semanticweb.org/rborrull/ontologies/2018/5/untitled-ontology-4";
//	String NS = SOURCE + "#";
//	OntModel base = ModelFactory.createOntologyModel();
//	base.read("./src/assets/prova.owl", "RDF/XML");
//
//	// create a dummy paper for this example
//	OntClass wine = base.getOntClass(NS + "Bridge");
//	Individual iPaper = base.createIndividual(NS + "ARRRG", wine);
//
//	// list the asserted types
//	for (Iterator<Resource> i = iPaper.listRDFTypes(true); i.hasNext();) {
//	    System.out.println(iPaper.getURI() + " is asserted in class " + i.next());
//	}
//
//	OntModel inf = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, base);
//	iPaper = inf.getIndividual(NS + "ARRRG");
//	for (Iterator<Resource> i = iPaper.listRDFTypes(true); i.hasNext();) {
//	    System.out.println(iPaper.getURI() + " is inferred to be in class " + i.next());
//	}
//
//	// Dataset ds = TDBFactory.createDataset("./src/assets/dataset");
//	FileWriter fw = new FileWriter("./src/assets/mergetestds.xml");
//	// RDFDataMgr.write(fw, ds, RDFFormat.NQUADS);
//	base.write(fw, "TTL");
//    }

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
		String NS = source + "#";
		this.model = ModelFactory.createOntologyModel();
		this.model.read(path, "RDF/XML");

		// return model;
	}

	/**
	 * Read the subclasses from the ontology
	 * 
	 * @param rootClasses
	 * @param label
	 */
//	private void readSubclasses(ExtendedIterator<?> rootClasses, String label) {
//		while (rootClasses.hasNext()) {
//			// System.out.println(rootClasses.next().getClass());
//			OntClass thisClass = (OntClass) rootClasses.next();
//			// System.out.println(label + thisClass.toString());
//
//			// ExtendedIterator<?> instances = thisClass.listInstances();
//			// while (instances.hasNext()) {
//			// Individual thisInstance = (Individual) instances.next();
//			// System.out.println(" Found instance: " + thisInstance.toString());
//			// }
//
//			ExtendedIterator<?> subclasses = thisClass.listSubClasses();
//			if (subclasses.hasNext()) {
//				readSubclasses(subclasses, "\t");
//			}
//
//		}
//	}

//	/**
//	 * 
//	 * @param title
//	 * @throws IOException
//	 */
//	public void createOnt(String title) throws IOException {
//		Date date = new Date();
//		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//
//		this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
//		this.source = "http://www.semanticweb.org/openreq/ontologies/" + localDate.getYear() + "/"
//				+ localDate.getMonthValue() + "/" + title;
//		this.name = title + ".owl";
//		// this.model.setNsPrefix(NS,
//		// "http://www.ontologies-owl.com/OntologyAnimals.rdf#");
//
//		// OntClass animales = this.model.createClass(NS + ":" + "Animales");
//		// OntClass vertebrados = this.model.createClass(NS + ":" + "Vertebrados");
//		// OntClass invertebrados = this.model.createClass(NS + ":" + "Invertebrados");
//		//
//		// vertebrados.setDisjointWith(invertebrados);
//		// animales.addSubClass(vertebrados);
//		// animales.addSubClass(invertebrados);
//		//
//		// Property p = this.model.createProperty(NS + ":" + "subType");
//		// this.model.add(animales, p, vertebrados);
//		// Statement s = this.model.createStatement(animales, p, vertebrados);
//		// this.model.add(animales, p, invertebrados);
//		// s = this.model.createStatement(animales, p, invertebrados);
//		//
//		// DatatypeProperty peso = this.model.createDatatypeProperty(NS + ":" + "Peso");
//		// peso.addDomain(animales);// Clase a la que pertenece
//		// peso.addRange(XSD.xint);// Tipo de la propiedad
//		// peso.convertToFunctionalProperty();// Para que solo acepte un valor.
//
//		// Individual leon = model.createIndividual(NS+":"+"Leon",vertebrados);
//		// leon.setPropertyValue(peso, model.createTypedLiteral(250));
//		//
//		// Individual leopardo =
//		// model.createIndividual(NS + ":" + "Leopardo", vertebrados);
//		// leopardo.setPropertyValue(peso, model.createTypedLiteral(200));
//		//
//		// Individual pulpo = model.createIndividual(NS+":"+"Pulpo",invertebrados);
//		// pulpo.setPropertyValue(peso, model.createTypedLiteral(10));
//		//
//		// Individual sepia = model.createIndividual(NS+":"+"Sepia",invertebrados);
//		// sepia.setPropertyValue(peso, model.createTypedLiteral(1));
//
//		// Almacenamos la ontología en un fichero OWL (Opcional)
//	}

//	public void createOntClass(String name) throws IOException {
//		OntClass newClass = model.createClass(source + "#" + name);
//	}

//	public void createObjectProperty(String name) throws IOException {
//		ObjectProperty newProperty = this.model.createObjectProperty(source + "#" + name);
//		// newProperty.addDomain();// Clase a la que pertenece
//		// newProperty.addRange(XSD.string);// Tipo de la propiedad
//	}

//	public void saveOntModel() throws IOException {
//		File file = new File("./src/assets/" + name);
//		// Hay que capturar las Excepciones
//		if (!file.exists()) {
//			file.createNewFile();
//		}
//		// model.write(new PrintWriter(file), "RDF/XML-ABBREV");
//
//		RDFWriter writer = model.getWriter("RDF/XML-ABBREV");
//		writer.setProperty("xmlbase", source);
//		writer.write(model, new PrintWriter(file), null);
//	}

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
						ontTerm = ontTerm + " " + words[i];
					} else {
						words[i] = words[i].toLowerCase();
						ontTerm = words[i];
					}
				}
//				String[] lemmas = extractLemmas(words, analizer);
				String[] lemmas = extractLemmas(ontTerm, analizer);

				ontClasses.add(thisClass);
				classesWords.add(words);
				classesLemmas.add(lemmas);
				for (int i = 0; i < lemmas.length; i++) {
//					if(!lemmas[i].equals("O")) {
					synonyms.put(lemmas[i], new ArrayList<String>());
					noSynonyms.put(lemmas[i], new ArrayList<String>());
//					}else {
//						synonyms.put(words[i], new ArrayList<String>());
//						noSynonyms.put(words[i], new ArrayList<String>());
//					}

				}
			}
		}
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
	private boolean isSynonym(String reqTerm, String ontLemma, NLPAnalyser analizer, double thr)
			throws SimilarityException, LexicalSemanticResourceException {
		if (!ontLemma.matches("\\d+|\\W+")) {
//			Instant start = Instant.now();
			if (!synonyms.get(ontLemma).contains(reqTerm) && !noSynonyms.get(ontLemma).contains(reqTerm)) {
				if (analizer.semanticSimilarity(reqTerm, ontLemma) >= thr) {
//					System.out.println("Similar1: :" + reqTerm + ", " + ontLemma);
					synonyms.get(ontLemma).add(reqTerm);
//					Instant end = Instant.now();
//					Duration timeElapsed = Duration.between(start, end);
//					System.out.println("Time taken: "+ timeElapsed.toMillis() +" millis. ("+reqTerm+", "+ontLemma+")");

					return true;
				} else {
					noSynonyms.get(ontLemma).add(reqTerm);
				}
			} else if (synonyms.get(ontLemma).contains(reqTerm)) {
				return true;
			} else if (noSynonyms.get(ontLemma).contains(reqTerm)) {
				return false;
			}
//			Instant end = Instant.now();
//			Duration timeElapsed = Duration.between(start, end);
//			System.out.println("Time taken: "+ timeElapsed.toMillis() +" millis. ("+reqTerm+", "+ontLemma+")");
		}
		return false;
	}

	/**
	 * Check if the req. term match with the term of the ontology
	 * 
	 * @param term
	 * @param lemma
	 * @param ontWord
	 * @param ontLemma
	 * @param analizer
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean isSameTerm(String term, String lemma, String ontWord, String ontLemma, NLPAnalyser analizer)
			throws SimilarityException, LexicalSemanticResourceException {

		if (term.toLowerCase().equals(ontWord))
			return true;
		if (lemma.equals(ontWord))
			return true;
		if (lemma.equals(ontLemma))
			return true;
		if (term.toLowerCase().equals(ontLemma))
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
			NLPAnalyser analizer, boolean syny, double thr)
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
						&& isSameTerm(ngramTerm.get(i), ngramLemma.get(i), words[j], lemmas[j], analizer)) {
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
								&& isSynonym(ngramLemma.get(i), lemmas[j], analizer, thr)) {
							find = true;
							idxOntLemmaAnalized.add(j);
						}
						j++;
					}
				} else
					find = true;
			}
		}
		return find;
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
			boolean syny, double thr) throws SimilarityException, LexicalSemanticResourceException {
		boolean find = false;
		for (int j = idx; j < termsNode.length && !find; j++) {
			ngramTerm.push(termsNode[j]);
			ngramLemma.push(lemmasNode[j]);
			if (level < n) {
				find = findPotentialNgram(j + 1, level + 1, n, termsNode, lemmasNode, ngramTerm, ngramLemma, words,
						lemmas, analizer, syny, thr);
			}
			if (level == n) {
				if (isSameNgram(ngramTerm, ngramLemma, words, lemmas, analizer, syny, thr)) {
					return true;
				}
			}
			ngramTerm.pop();
			ngramLemma.pop();
		}
		return find;
	}

	/**
	 * Find if the set of words contains a correct n-gram that match with the
	 * ontology.
	 * 
	 * @param node
	 * @param words
	 * @param lemmas
	 * @param analizer
	 * @param syny
	 * @param thr
	 * @return
	 * @throws SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	private boolean extractNGram(Node node, String[] words, String[] lemmas, NLPAnalyser analizer, boolean syny,
			double thr) throws SimilarityException, LexicalSemanticResourceException {
		String[] termsNode = node.getTerm().split(" ");
		String[] lemmasNode = node.getLemma().split(" ");
		int n = words.length;
//		for (int i = 0; i < n; i++) {
		Stack<String> ngramTerm = new Stack<>();
		Stack<String> ngramLemma = new Stack<>();
//			for (int j = 0; j < termsNode.length -n +1; j++) {
//				ngramTerm.add(termsNode[j]);
//				ngramLemma.add(LemmasNode[j]);
//			}		
//			if (isSameNgram(ngramTerm, ngramLemma, words, lemmas, analizer)) {
//				return true;
//			}

		if (findPotentialNgram(0, 1, n, termsNode, lemmasNode, ngramTerm, ngramLemma, words, lemmas, analizer, syny,
				thr)) {
			return true;
		}

//		}
		return false;
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
	public void matching(ArrayList<Node> topNodes, String reqId, String requirement, NLPAnalyser analizer, boolean syny,
			double thr) throws IOException, SimilarityException, LexicalSemanticResourceException {
		ArrayList<OntClass> classes = new ArrayList<>();
		String[] words;
		String[] lemmas;
		for (int i = 0; i < topNodes.size(); i++) {
			for (int j = 0; j < ontClasses.size(); j++) {
				words = classesWords.get(j);
				lemmas = classesLemmas.get(j);
				if (topNodes.get(i).getTerm().split(" ").length >= words.length)
					if (extractNGram(topNodes.get(i), words, lemmas, analizer, syny, thr)) {
						classes.add(ontClasses.get(j));
					}

			}
		}

		// Requirement instantiation within the ontology
		for (OntClass cls : classes) {
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

//	private String[] extractLemmas(String[] words, NLPAnalyser analizer) throws IOException {
////		NLPAnalyser analizer = new NLPAnalyser();
//		String[] lemmas = analizer.POSLemmatization(words);
//		return lemmas;
//	}

	/**
	 * Extract lemmas from ontology classes
	 * @param words
	 * @param analizer
	 * @return
	 * @throws IOException
	 * @throws UIMAException
	 */
	private String[] extractLemmas(String words, NLPAnalyser analizer) throws IOException, UIMAException {
//		NLPAnalyser analizer = new NLPAnalyser();
		String ontLemma = analizer.lemmatization(words);
		String[] lemmas = ontLemma.split(" ");
		return lemmas;
	}

	/**
	 * Analyze the ontology and extract dependncies
	 * @return
	 */
	public ArrayList<Dependency> ontConflictDetection() {
		ArrayList<Dependency> dependencies = new ArrayList<>();
		ExtendedIterator<?> rootClasses = this.model.listClasses();
		while (rootClasses.hasNext()) {
			OntClass thisClass = (OntClass) rootClasses.next();
			if (thisClass.getLocalName() != null) {
				// System.out.println(thisClass.getLocalName());
				ArrayList<ArrayList<Object>> depClasses = new ArrayList<>();
				for (Iterator<OntClass> supers = thisClass.listSuperClasses(); supers.hasNext();) {
					depClasses.add(displayType(supers.next()));
				}

				/* Create dependencies */
				ArrayList<String> from = new ArrayList<>();
				ArrayList<String> to = new ArrayList<>();
				if (!depClasses.isEmpty()) {
					from = findIndividuals(thisClass);
					for (ArrayList<Object> dep : depClasses) {
						if (!dep.isEmpty()) {
							// System.out.println("\t: "+dep.get(0)+" with: " + ((OntClass)
							// dep.get(1)).getLocalName());
							to = findIndividuals((OntClass) dep.get(1));
							for (String f : from) {
								for (String t : to) {
									if (!f.equals(t)) {
//										Dependency newDep = new Dependency(f, t, Status.PROPOSED,
//												DependencyType.valueOf((String) dep.get(0).toString().toUpperCase()));
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
	private ArrayList<String> findIndividuals(OntClass thisClass) {

		ArrayList<String> individualIds = new ArrayList<>();
		ExtendedIterator<?> instances = thisClass.listInstances();
		while (instances.hasNext()) {
			Individual thisInstance = (Individual) instances.next();

			DatatypeProperty className = this.model.getDatatypeProperty(this.source + "#class");
			String namemore = thisInstance.getPropertyValue(className).toString();
			String name = namemore.split("\\^\\^")[0];
			if (name.equals(thisClass.getLocalName())) {
				DatatypeProperty id = this.model.getDatatypeProperty(this.source + "#id");
				individualIds.add(thisInstance.getPropertyValue(id).toString().split("\\^\\^")[0]);
			}
			// System.out.println(" Found instance: " + thisInstance.toString());
			// System.out.println(" \t" +
			// thisInstance.getPropertyValue(req).toString().split("\\^\\^")[0]);
		}
		return individualIds;
	}

	private ArrayList<Object> displayType(OntClass supClass) {
		if (supClass.isRestriction()) {
			return displayRestriction(supClass.asRestriction());
		}
		return new ArrayList<>();
	}

	private ArrayList<Object> displayRestriction(Restriction supRest) {
		if (supRest.isAllValuesFromRestriction()) {
			return displayRestriction(supRest.getOnProperty(), supRest.asAllValuesFromRestriction().getAllValuesFrom());
		} else if (supRest.isSomeValuesFromRestriction()) {
			return displayRestriction(supRest.getOnProperty(),
					supRest.asSomeValuesFromRestriction().getSomeValuesFrom());
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("serial")
	private ArrayList<Object> displayRestriction(OntProperty property, Resource constraint) {
		// String out = String.format("%s %s %s", qualifier, renderURI(property),
		// renderConstraint(constraint));
		// System.out.println("\t: " + out);
		return new ArrayList<Object>() {
			{
				add(property.getLocalName());
				add(constraint);
			}
		};
	}

	// private Object renderConstraint(Resource constraint) {
	// if (constraint.canAs(UnionClass.class)) {
	// UnionClass uc = constraint.as(UnionClass.class);
	// // this would be so much easier in ruby ...
	// String r = "union{ ";
	// for (Iterator<? extends OntClass> i = uc.listOperands(); i.hasNext();) {
	// r = r + " " + renderURI(i.next());
	// }
	// return r + "}";
	// } else {
	// return renderURI(constraint);
	// }
	// }
	//
	// private Object renderURI(Resource onP) {
	// String qName = onP.getModel().qnameFor(onP.getURI());
	// return qName == null ? onP.getLocalName() : qName;
	// }

}
