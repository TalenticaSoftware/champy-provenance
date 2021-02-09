package com.talentica.champy.bottle.api.request;

// '.../bottleApi/createBottle' HTTP Post request body representing class.
public class CreateBottleBoxRequest {
    public String uuid;
    public String manufacturer;
    public String proposition;
    public long fee;

    public void setId(String uuid){ this.uuid = uuid;}

    public void setManufacturer(String manufacturer) {this.manufacturer = manufacturer;}

    public void setProposition(String proposition) {this.proposition = proposition;}

    public void setFee(long fee) {this.fee = fee;}
}
