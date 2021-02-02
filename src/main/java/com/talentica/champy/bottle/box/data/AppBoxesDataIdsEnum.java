package com.talentica.champy.bottle.box.data;

public enum  AppBoxesDataIdsEnum {
    BottleBoxDataId((byte)1),
    ShipmentOrderDataId((byte)1);

    private final byte id;

    AppBoxesDataIdsEnum(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }
}
