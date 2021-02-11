package com.talentica.champy.bottle.services;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.serialization.Views;

import java.util.Arrays;

// This state will be stored for every bottle
@JsonView(Views.Default.class)
public class BottleDBStateData {

    public BottleDBStateData(String uuid, BottleStateEnum state, String manufacturer, String carrier,
                             String retailer, String createBottleTransactionId) {
        this.uuid = uuid;
        this.state = state;
        this.manufacturer = manufacturer;
        this.carrier = carrier;
        this.retailer = retailer;
        this.createBottleTransactionId = createBottleTransactionId;
    }

    public BottleDBStateData(String uuid) {
        this.uuid = uuid;
        this.state = BottleStateEnum.CREATED;
        this.manufacturer = "";
        this.carrier = "";
        this.retailer = "";
        this.createBottleTransactionId = "";
    }

    public String getUuid() {
        return uuid;
    }

    public BottleStateEnum getState() {
        return state;
    }

    public void setState(BottleStateEnum state) {
        this.state = state;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    public String getCreateBottleTransactionId() {
        return createBottleTransactionId;
    }

    public void setCreateBottleTransactionId(String createBottleTransactionId) {
        this.createBottleTransactionId = createBottleTransactionId;
    }

    private final String uuid;
    private BottleStateEnum state;
    private String manufacturer;
    private String carrier;
    private String retailer;
    private String createBottleTransactionId;

    // Define serialization of State Object
    public byte[] bytes() {
        return Bytes.concat(
                Ints.toByteArray(uuid.length()),
                uuid.getBytes(),
                new byte [] {state.state()},
                Ints.toByteArray(manufacturer.length()),
                manufacturer.getBytes(),
                Ints.toByteArray(carrier.length()),
                carrier.getBytes(),
                Ints.toByteArray(retailer.length()),
                retailer.getBytes(),
                Ints.toByteArray(createBottleTransactionId.length()),
                createBottleTransactionId.getBytes()
        );
    }

    // Define deserialization of state object
    public static BottleDBStateData parseBytes(byte[] bytes) {
        int offset = 0;

        int size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String uuid = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        BottleStateEnum state =  BottleStateEnum.get(bytes[offset]);
        offset += 1;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String manufacturer = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String carrier = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String retailer = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String createTransactionId = new String(Arrays.copyOfRange(bytes, offset, offset + size));

        return new BottleDBStateData(uuid, state, manufacturer, carrier, retailer, createTransactionId);
    }
}
