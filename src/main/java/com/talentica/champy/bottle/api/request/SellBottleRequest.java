package com.talentica.champy.bottle.api.request;

public class SellBottleRequest {
    public void setBottleBoxId(String bottleBoxId) {
        this.bottleBoxId = bottleBoxId;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public void setSellingPrice(long sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String bottleBoxId;  // hex representation of bottle box id
    public long sellingPrice;   // bottle selling price
    public long fee;
}
