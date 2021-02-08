package com.talentica.champy.bottle.services;

public enum BottleStateEnum {
    CREATED((byte)1),
    SHIPPED((byte)2),
    DELIVERED((byte)3),
    SOLD((byte)4);

    private final byte state;

    BottleStateEnum(byte state) {
        this.state = state;
    }
    public byte state() {
        return state;
    }

    public static BottleStateEnum get(byte b){
        return BottleStateEnum.values()[b-1];
    }
}
