package com.talentica.champy.bottle.api.request;

// '.../bottleApi/createBottle' HTTP Post request body representing class.
public class CreateBottleBoxRequest {
    public String id;
    public String manufacturer;
    public int year;
    public String proposition;
    public long fee;

    public void setId(String id){ this.id = id;}

    public void setManufacturer(String manufacturer) {this.manufacturer = manufacturer;}

    public void setYear(int year) {this.year = year;}

    public void setProposition(String proposition) {this.proposition = proposition;}

    public void setFee(long fee) {this.fee = fee;}
}
