package com.example.gpsen;

public class Employee {
    private String UID;
    private String fullName;
    private String organization;
    private String heartRate;
    private String bodyTemperature;
    private String SPO2Level;

    public Employee(String UID, String fullName, String organization, String heartRate, String bodyTemperature, String SPO2Level) {
        this.UID = UID;
        this.fullName = fullName;
        this.organization = organization;
        this.heartRate = heartRate;
        this.bodyTemperature = bodyTemperature;
        this.SPO2Level = SPO2Level;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
    }

    public String getBodyTemperature() {
        return bodyTemperature;
    }

    public void setBodyTemperature(String bodyTemperature) {
        this.bodyTemperature = bodyTemperature;
    }

    public String getSPO2Level() {
        return SPO2Level;
    }

    public void setSPO2Level(String SPO2Level) {
        this.SPO2Level = SPO2Level;
    }
}
