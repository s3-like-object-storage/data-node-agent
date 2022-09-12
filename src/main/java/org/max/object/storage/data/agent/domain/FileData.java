
package org.max.object.storage.data.agent.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class FileData {

    private String id;

    private String data;

    public FileData(String id, String data) {
        this.id = id;
        this.data = data;
    }

    public FileData() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setData(String data) {
        this.data = data;
    }

    @JsonInclude(Include.NON_NULL)
    public String getData() {
        return this.data;
    }
}
