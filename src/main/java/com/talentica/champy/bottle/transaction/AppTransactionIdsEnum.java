package com.talentica.champy.bottle.transaction;

public enum AppTransactionIdsEnum {
    CreateBottleTransactionId((byte)1),
    CreateShipmentOrderTransactionId((byte)2),
    DeliverShipmentOrderTransactionId((byte)3),
    SellBottleTransactionId((byte)4);

    private final byte id;

    AppTransactionIdsEnum(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }
}
