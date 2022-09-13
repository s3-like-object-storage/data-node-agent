package org.max.object.storage.data.agent.api;

public class ErrorData {

    private String code;

    private String description;

    public ErrorData() {
        this("undefined", "no description");
    }

    public ErrorData(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
