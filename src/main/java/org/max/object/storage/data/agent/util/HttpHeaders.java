package org.max.object.storage.data.agent.util;

public enum HttpHeaders {

    CONTENT_TYPE("Content-Type"),
    LOCATION("Location");

    private final String name;

    HttpHeaders(String name) {
        this.name = name;
    }

    public String headerName(){
        return name;
    }
}
