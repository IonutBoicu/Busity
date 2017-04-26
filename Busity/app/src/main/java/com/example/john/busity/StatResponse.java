package com.example.john.busity;

/**
 * Created by John on 4/25/2017.
 */

public class StatResponse {
    private String lineNo;
    private String dayOfWeek;
    private int hour;
    private String stationName;
    private int waitingTime;

    public StatResponse() {}

    public StatResponse(String lineNo, String dayOfWeek, int hour, String stationName, int waitingTime) {
        this.lineNo = lineNo;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.stationName = stationName;
        this.waitingTime = waitingTime;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public String getStationName() {
        return stationName;
    }

    public int getWaitingTime() {
        return waitingTime;
    }
}
