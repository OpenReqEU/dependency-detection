package com.gessi.dependency_detection.functionalities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gessi.dependency_detection.components.*;

/**
 * 
 * Information extraction class, used to analyze the dependency tree.
 *
 */
public class DependencyTreeIE {
	private Node root;
	// lists of second level rule
	private List<String> depNNLevel2 = Arrays.asList("nsubj", "nsubjpass", "nn", "appos", "conj", "hmod", "abbrev");
	private List<String> depOtherLevel2 = Arrays.asList("num", "pobj", "dobj", "iobj"/* ,"acomp" */);
	// list of first level rule
	private List<String> depLevel1 = Arrays.asList("prep", "agent", "amod", "advcl", "xcomp", "acomp", "ccomp", "root",
			"oprd", "possessive", "partmod", "pcomp");

	/**
	 * Constructor
	 */
	public DependencyTreeIE() {
		super();
	}

	/**
	 * Constructor
	 */
	public DependencyTreeIE(Node root) {
		super();
		this.root = root;
	}

	public Node getRoot() {
		return root;
	}

	/**
	 * Information Extaction The initial state is the root of the tree
	 * 
	 * @return
	 */
	public ArrayList<Node> applyIE() {
		ArrayList<Node> topNodes = analyzeSon(root);
		if (root.getPosTag().matches("\\w*NN\\w*")) {
			topNodes.add(root);
		}
		return topNodes;
	}

	/**
	 * Analyse the branches of a tree node. Depending of their dependency relations,
	 * the word will be stored as a candidate or not (topNodes).
	 */
	private ArrayList<Node> analyzeSon(Node node) {
		ArrayList<Node> topNodes = new ArrayList<>();
		for (Node son : node.getSonNodes()) {
			if ((depNNLevel2.contains(son.getDependencyType()) && son.getPosTag().matches("\\w*NN\\w*"))
					|| (depOtherLevel2
							.contains(son.getDependencyType())/* && son.getPosTag().matches("\\w*CD\\w*") */)) {

				ArrayList<Node> sonPred = analyzeSon(son);

				// check if the node has ngrams
				Node sonGram = findNGrams(son, sonPred);

				topNodes.addAll(sonPred);
				if (sonGram != null) {
					topNodes.add(sonGram);
				} else {
					topNodes.add(son);
				}

			} else if (depLevel1.contains(son.getDependencyType())) {
				topNodes.addAll(analyzeSon(son));
			}
		}
		return topNodes;
	}

	/**
	 * The node term will be updated with the terms of its direct children.
	 * Those children will be removed from the topNodes as they are taken into
	 * account in a set of words (ngram)
	 * 
	 * @param node
	 * @param sonPred
	 * @return Node
	 */
	private Node findNGrams(Node node, ArrayList<Node> sonPred) {
		String ngram = node.getTerm();
		String lemmaNgram = node.getLemma();
		boolean directSon = false;
		Node sonGram = null;
		ArrayList<Integer> idxToRemove = new ArrayList<>();

		for (int i = 0; i < sonPred.size(); i++) {
			if (sonPred.get(i).getParentId() == node.getId()/* && sonPred.get(i).getId()!=-1 */) {
				ngram = ngram + " " + sonPred.get(i).getTerm();
				if (!sonPred.get(i).getLemma().matches("\\d+"))
					lemmaNgram = lemmaNgram + " " + sonPred.get(i).getLemma();
				else
					lemmaNgram = lemmaNgram + " " + sonPred.get(i).getTerm();
				idxToRemove.add(i);
				directSon = true;
			} 
		}
		for (int i = idxToRemove.size() - 1; i >= 0; i--) {
			sonPred.remove((int) (idxToRemove.get(i)));
		}if (directSon) {
			sonGram = new Node(-1, node.getParentId(), node.getPosTag(), node.getDependencyType(), ngram, lemmaNgram,
					node.getDependency());
		}
		return sonGram;
	}
}
