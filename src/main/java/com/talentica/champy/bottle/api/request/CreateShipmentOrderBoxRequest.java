package com.talentica.champy.bottle.api.request;

import java.util.ArrayList;

public class CreateShipmentOrderBoxRequest {
    public String shipmentId;
    public String manufacturer;
    public String receiver;
    public String carrier;
    public String shippingDate;
    public ArrayList<String> bottleBoxIds;
    public long shipmentValue;
    public String carrierProposition;
    public long fee;

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public void setShippingDate(String shippingDate) {
        this.shippingDate = shippingDate;
    }

    public void setBottleBoxIds(ArrayList<String> bottleBoxIds) {
        this.bottleBoxIds = bottleBoxIds;
    }

    public void setShipmentValue(long shipmentValue) {
        this.shipmentValue = shipmentValue;
    }

    public void setCarrierProposition(String carrierProposition) {
        this.carrierProposition = carrierProposition;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }
}
