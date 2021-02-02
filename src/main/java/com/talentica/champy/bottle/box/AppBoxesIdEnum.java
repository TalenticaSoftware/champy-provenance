package com.talentica.champy.bottle.box;

public enum AppBoxesIdEnum {
    BottleBoxId((byte)1),
    ShipmentOrderId((byte)2);

    private final byte id;

    AppBoxesIdEnum(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }
}
