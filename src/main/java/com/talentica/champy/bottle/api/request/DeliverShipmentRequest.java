package com.talentica.champy.bottle.api.request;

public class DeliverShipmentRequest {

    public void setShipmentOrderId(String shipmentOrderId) {
        this.shipmentOrderId = shipmentOrderId;
    }

    public void setRetailerProposition(String retailerProposition) {
        this.retailerProposition = retailerProposition;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String shipmentOrderId;      // hex representation of shipment order box id
    public String retailerProposition;  // public key of shipment receiver - retailer
    public long fee;
}
