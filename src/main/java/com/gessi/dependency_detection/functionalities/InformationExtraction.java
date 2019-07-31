package com.gessi.dependency_detection.functionalities;

import java.util.*;

import opennlp.tools.parser.Parse;

/**
 * NOT USED!
 * Old Information Extraction class! 
 * Not for a dependency parser (ClearNLP), but for a simple parser (OpenNLP).
 *
 */
public class InformationExtraction {

    public InformationExtraction() {
	super();
    }

    public InformationExtraction(Parse parse) {
	super();
	applyInfoExtraction(parse);
    }

    public List<String> applyInfoExtraction(Parse parse) {
	ArrayList<String> infoExtracted = new ArrayList<>();
	String np = "";
	String obj = "";
	ArrayList<Parse> verb = null;
	if (parse.getType().equals("S")) {
		
	    if (parse.getChildren()[0].getType().equals("S")) {
		do {
		    parse = parse.getChildren()[0];
		} while (parse.getChildren()[0].getType().equals("S"));
	    }

	    Parse[] children = parse.getChildren();
	    for (Parse child : children) {
		if (child.getType().equals("NP")) {
		    np = extractSubject(child);
		    infoExtracted.add(np);
		} else if (child.getType().equals("VP")) {
		    Map<Integer, ArrayList<Parse>> vp = extractPredicate(child.getChildren(), 0);
		    int key = findMax(vp.keySet());
		    verb = vp.get(key);
		    String vps = "";
		    for (Parse p : verb) {
			vps = vps.concat(p.getCoveredText().replaceAll(",|;|:", "") +"_"+ p.getType() + ",");

			vps = vps.concat(extractVerbAtributes(verb.get(0).getParent().getChildren()));
			obj = obj.concat(extractObject(verb.get(0).getParent().getChildren()));
		    }
		    infoExtracted.add(vps);
		    infoExtracted.add(obj);
		}
	    }
	}
	return infoExtracted;
    }

    private Integer findMax(Set<Integer> values) {
	int max = -999;
	for (int d : values) {
	    if (d > max) max = d;
	}

	return max;
    }

    public String extractAtributes(Parse[] parses) {
	for (Parse parse : parses) {
	    if (parse.getType().matches("\\w*RB\\w*")) {
		parse.getParent();
	    }
	}

	return null;
    }

    public String extractSubject(Parse parse) {
	List<String> tags = null;
	if (parse.getType().equals("PP")) {
	    tags = Arrays.asList("V", "NN");

	} else if (parse.getType().equals("NP")) {
	    tags = Arrays.asList("NN");
	}

	/* Find first noun (and its siblings if it has them) */
	Parse node = null;
	ArrayList<Parse> siblings = null;
	String subject = "";
	for (String tag : tags) {
	    node = extractFirstTag(parse, tag);
	    if (node != null) {

			siblings = extractSibling(node, tag);
			for (Parse s : siblings) {
				subject = subject.concat(s.getCoveredText().replaceAll(",|;|:", "") +"_"+ s.getType() + ",");
			}

			/* Find the PP NN attributes of the noun */
			Parse parent = null;
			if (node.getType().matches("\\w*V\\w*")) {
				parent = findParent(node, "VP");
				if (parent.getType().equals("S")) parent = findParent(node, "NP");
				siblings = extractSibling(parent, "PP");
			} else if (node.getType().matches("\\w*NN\\w*")) {
				parent = findParent(node, "NP");
				siblings = extractSibling(parent, "PP");
			}

			for (Parse child : siblings) {
				if (child.getType().equals("PP") || (child.getType().equals("NP") && !child.equals(node))) subject = subject.concat(extractSubject(child) + ",");
			}
	    }
	}

	return subject;
    }

    public Parse findParent(Parse parse, String tag) {

	while (!parse.getType().equals("S") && !parse.getType().equals(tag)) {
	    parse = parse.getParent();
	}
	return parse;
    }

    public Boolean isVerbPhrase(Parse parse) {
	Parse source = parse.getParent();
	while (!source.getType().equals("S")) {
	    if (!source.getType().matches("\\w*V\\w*")) {
		return false;
	    }
	    source = source.getParent();
	}
	return true;
    }

    public Parse extractFirstTag(Parse parse, String tag) {
	Parse noun = null;
	Parse[] children = parse.getChildren();
	int i = 0;
	while (i < children.length && noun == null) {
	    if (children[i].getChildCount() != 0 && !children[i].getChildren()[0].getType().equals("TK")) {
		noun = extractFirstTag(children[i], tag);
	    } else if (children[i].getType().matches("\\w*" + tag + "\\w*")) {
		noun = children[i];
	    }
	    i++;
	}
	return noun;
    }

    public List<Parse> extractChildrenTag(Parse parse, String tag) {
	ArrayList<Parse> noun = new ArrayList<>();
	Parse[] children = parse.getChildren();

	for (Parse child : children) {
	    if (child.getChildCount() != 0 && !child.getChildren()[0].getType().equals("TK")) {
		noun.addAll(extractChildrenTag(child, tag));
	    } else if (child.getType().matches("\\w*" + tag + "\\w*")) {
		noun.add(child);
	    }
	}

	return noun;
    }

    public Map<Integer, ArrayList<Parse>> extractPredicate(Parse[] parse, int lebel) {
	HashMap<Integer, ArrayList<Parse>> verbs = new HashMap<>();
	int i = 0;
	while (i < parse.length) {

	    if (parse[i].getChildCount() != 0 && !parse[i].getChildren()[0].getType().equals("TK")
		    && !parse[i].getType().equals("SBAR")) {
		verbs.putAll(extractPredicate(parse[i].getChildren(), lebel + 1));
	    } else if (parse[i].getType().matches("\\w*V\\w*") && isVerbPhrase(parse[i])) {
		if (!verbs.containsKey(lebel)) {
		    ArrayList<Parse> p = new ArrayList<>();
		    p.add(parse[i]);
		    verbs.put(lebel, p);
		} else {
		    ArrayList<Parse> p = verbs.get(lebel);
		    p.add(parse[i]);
		    verbs.put(lebel, p);
		}

	    }
	    i++;
	}

	return verbs;
    }

    public String extractObject(Parse[] parse) {

	String obj = "";
	for (Parse child : parse) {
	    if (child.getType().equals("NP") || child.getType().equals("PP")) {
		obj = obj.concat(extractSubject(child));
	    } else {
		Parse node = extractFirstTag(child, "JJ");
		if (node != null)
		    obj = obj.concat(node.getCoveredText().replaceAll(",|;|:", "") +"_"+ node.getType() + ",");
	    }
	}

	return obj;
    }
    
    public String extractVerbAtributes(Parse[] parse) {

	String obj = "";
	for (Parse child : parse) {
	    if (child.getType().equals("ADVP")) {
		Parse node = extractFirstTag(child, "IN");
		if (node != null)
		    obj = obj.concat(node.getCoveredText().replaceAll(",|;|:", "") +"_"+ node.getType() + ",");
		node = extractFirstTag(child, "RB");
		if (node != null)
		    obj = obj.concat(node.getCoveredText().replaceAll(",|;|:", "") +"_"+ node.getType() + ",");
	    }
	}

	return obj;
    }

    private ArrayList<Parse> extractSibling(Parse parse, String tag) {
	ArrayList<Parse> siblings = new ArrayList<>();
	for (Parse child : parse.getParent().getChildren()) {
	    if (child.getType().matches("\\w*" + tag + "\\w*") || child.getType().matches("\\w*CD\\w*") || child.getType().matches("\\w*JJ\\w*")) {
		siblings.add(child);
	    }
	}
	return siblings;
    }

    private ArrayList<Parse> extractBrother(Parse[] parse) {
	ArrayList<Parse> siblings = new ArrayList<>();
	for (Parse words : parse) {
	    if (words.getType().equals("NP") || words.getType().equals("PP") || words.getType().equals("ADJP")) {
		siblings.add(words);
	    } else if (words.getChildCount() != 0 && !words.getChildren()[0].getType().equals("TK")) {
		siblings.addAll(extractBrother(words.getChildren()));
	    }
	}

	return siblings;
    }
}
