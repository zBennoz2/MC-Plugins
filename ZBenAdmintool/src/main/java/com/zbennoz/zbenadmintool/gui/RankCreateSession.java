package com.zbennoz.zbenadmintool.gui;

public class RankCreateSession {

    public enum Step {
        NAME,
        COLOR,
        PRIORITY
    }

    private Step step = Step.NAME;
    private String name;
    private String color;
    private Integer priority;

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
