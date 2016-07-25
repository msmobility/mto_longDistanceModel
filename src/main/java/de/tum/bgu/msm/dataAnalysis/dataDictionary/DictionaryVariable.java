package de.tum.bgu.msm.dataAnalysis.dataDictionary;

/**
 * Created by Joe on 25/07/2016.
 */
public class DictionaryVariable {
    private String name;
    private String question;
    private int start;
    private int end;

    public DictionaryVariable(String name, String question, int start, int end) {
        this.name = name;
        this.question = question;
        this.start = start;
        this.end = end;
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
}
