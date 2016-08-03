package de.tum.bgu.msm.dataAnalysis.dataDictionary;

import java.util.HashMap;

/**
 * Created by Joe on 25/07/2016.
 */
public class DictionaryVariable {
    private String name;
    private String question;
    private int start;
    private int end;

    private HashMap<Integer, String> answers;

    public DictionaryVariable(String name, String question, int start, int end, HashMap answers) {
        this.name = name;
        this.question = question;
        this.start = start;
        this.end = end;
        this.answers = answers;
    }

    public String getName() {
        return name;
    }

    public String getQuestion() {
        return question;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String decodeAnswer(int code) {
        //get the decoded answer, or just return the code if not found
        return answers.getOrDefault(code, String.valueOf(code));
    }
}
