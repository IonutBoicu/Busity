package com.example.john.busity;

/**
 * Created by John on 4/24/2017.
 */

public class LineResponse {
    private String lineNo;
    private String type;

    public LineResponse() {

    }

    public LineResponse(String lineNo, String type) {
        this.lineNo = lineNo;
        this.type = type;
    }

    public String getLineNo() {
        return lineNo;
    }

    public String getType() {
        return type;
    }


}