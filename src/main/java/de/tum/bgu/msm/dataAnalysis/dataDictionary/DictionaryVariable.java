package de.tum.bgu.msm.dataAnalysis.dataDictionary;

import java.util.HashMap;

/**
 * Created by Joe on 25/07/2016.
 */
public class DictionaryVariable {
    private String name;
    private String type;
    private int start;
    private int end;

    private HashMap<Integer, String> answers;

    public DictionaryVariable(String name, String type, int start, int end, HashMap<Integer, String> answers) {
        this.name = name;
        this.type = type;
        this.start = start;
        this.end = end;
        this.answers = answers;
    }

    public String getName() {
        return name;
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
