package com.gessi.dependency_detection.functionalities;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class TextPreprocessing {

    Set<String> exclusions = null;

    /**
     * Preprocesses the text by removing stopwords and characters that hold no semantic meaning, or would worsen the semantic analysis
     * @param  text The string to preprocess
     * @return  The preprocessed text
     */
    public String text_preprocess(String text) throws IOException {
        String trueRes = "";
        if (text != null) {
            text = text.replaceAll("(\\{.*?})", " code ");
            text = text.replaceAll("[$,;\\\"/:|!?=()><_{}'+%[0-9]]", " ");
            text = text.replaceAll("] \\[", "][");

            if (exclusions == null) {
                BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/ExcludedWords.txt"));
                String word = null;
                exclusions = new HashSet<>();

                while ((word = reader.readLine()) != null) {
                    exclusions.add(word);
                }
                reader.close();
            }
            for (String l : text.split(" ")) {
                if (!(l.toLowerCase().equals("null") && !l.equals("null") && !l.equals("Null")) && !l.toUpperCase().equals(l)) l = l.toLowerCase();
                if (l != null && !exclusions.contains(l) && l.length() > 1) {
                    String[] aux=l.split("\\.");
                    if (!(aux.length>1 && (aux[1]==null|| aux[0]==null || aux[0].equals("")&&aux[1].equals("") || aux[0].equals(" ")|| aux[1].equals(" ")))) {
                        if (aux.length > 1) {
                            String repeatingWord = aux[0];
                            l = aux[0] + " " + aux[0];
                            for (int i = 1; i < aux.length; ++i) {
                                repeatingWord = repeatingWord + "." + aux[i];
                                l = l + "." + aux[i];
                                if (i != (aux.length - 1)) l = l + " " + repeatingWord;
                            }
                        }
                    }
                    else l=l.replace(".","");
                    trueRes = trueRes.concat(l + " ");
                }
            }
        }
        return trueRes;

    }

}
