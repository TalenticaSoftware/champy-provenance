package com.talentica.champy.bottle.box.data;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.data.AbstractNoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import com.talentica.champy.bottle.box.ShipmentOrderBox;

import scorex.crypto.hash.Blake2b256;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.talentica.champy.bottle.box.data.AppBoxesDataIdsEnum.ShipmentOrderBoxDataId;

@JsonView(Views.Default.class)
public class ShipmentOrderBoxData extends AbstractNoncedBoxData<PublicKey25519Proposition, ShipmentOrderBox, ShipmentOrderBoxData> {
    // attributes defined for ShipmentOrder:
    private final String shipmentId;    //Unique shipment ID
    private final String manufacturer;        //manufacturer public key
    private final String receiver;      //Retailer - receiver public key
    private final String carrier;       //Carrier public key
    private final String shippingDate;    //Shipment date mm-dd-yyyy format
    private final List<String> bottleBoxUuids;  //List of bottle Ids shipped within this shipment
    private final long shipmentValue;           //Total shipment value

    public ShipmentOrderBoxData(PublicKey25519Proposition proposition, String shipmentId, String manufacturer, String receiver, String carrier, String shippingDate, List<String> bottleBoxUuids, long shipmentValue) {
        super(proposition, 1);
        this.shipmentId = shipmentId;
        this.manufacturer = manufacturer;
        this.receiver = receiver;
        this.carrier = carrier;
        this.shippingDate = shippingDate;
        this.bottleBoxUuids = bottleBoxUuids;
        this.shipmentValue = shipmentValue;
    }

    @Override
    public ShipmentOrderBox getBox(long nonce) {
        return new ShipmentOrderBox(this, nonce);
    }

    @Override
    public byte[] customFieldsHash() {
        return Blake2b256.hash(
                Bytes.concat(
                        shipmentId.getBytes(),
                        manufacturer.getBytes(),
                        receiver.getBytes()));
    }

    @Override
    public byte[] bytes() {
        byte [] bottleUuidsBytes = new byte[] {};

        //Collect Bytes from all the bottle ids
        for (String bottleUuid : bottleBoxUuids) {
            bottleUuidsBytes = Bytes.concat(bottleUuidsBytes,
                    Ints.toByteArray(bottleUuid.length()),
                    bottleUuid.getBytes());
        }

        return Bytes.concat(
                proposition().bytes(),
                Ints.toByteArray(shipmentId.length()),
                shipmentId.getBytes(),
                Ints.toByteArray(manufacturer.length()),
                manufacturer.getBytes(),
                Ints.toByteArray(receiver.length()),
                receiver.getBytes(),
                Ints.toByteArray(carrier.length()),
                carrier.getBytes(),
                Ints.toByteArray(shippingDate.length()),
                shippingDate.getBytes(),
                Ints.toByteArray(bottleBoxUuids.size()),
                bottleUuidsBytes,
                Longs.toByteArray(shipmentValue)
        );
    }

    public static ShipmentOrderBoxData parseBytes(byte[] bytes){
        int offset = 0;
        PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer()
                .parseBytes(Arrays.copyOf(bytes, PublicKey25519Proposition.getLength()));
        offset += PublicKey25519Proposition.getLength();

        int size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String shipmentId = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        String manufacturer = new String(Arrays.copyOfRange(bytes, offset, offset+size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        String receiver = new String(Arrays.copyOfRange(bytes, offset, offset+size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        String carrier = new String(Arrays.copyOfRange(bytes, offset, offset+size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        String shipmentDate = new String(Arrays.copyOfRange(bytes, offset, offset+size));
        offset += size;

        //Number of bottleBoxIds
        final int nBottles = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        List<String> bottleBoxIds = new ArrayList<>(size);
        for(int i=0; i<nBottles; ++i){
            size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
            offset += Ints.BYTES;

            String bottleBoxId = new String(Arrays.copyOfRange(bytes, offset, offset+size));
            offset += size;

            bottleBoxIds.add(bottleBoxId);
        }

        long shipmentValue = Longs.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Longs.BYTES));

        return new ShipmentOrderBoxData(proposition, shipmentId, manufacturer, receiver, carrier, shipmentDate, bottleBoxIds, shipmentValue );
    }

    @Override
    public NoncedBoxDataSerializer serializer() {
        return ShipmentOrderBoxDataSerializer.getSerializer();
    }

    @Override
    public byte boxDataTypeId() {
        return ShipmentOrderBoxDataId.id();
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getShippingDate() {
        return shippingDate;
    }

    public List<String> getBottleBoxUuids() {
        return bottleBoxUuids;
    }

    public long getShipmentValue() {
        return shipmentValue;
    }

    @Override
    public String toString() {
        return "ShipmentOrderBoxData{" +
                "shipmentId=" + shipmentId +
                ", proposition=" + proposition() +
                ", manufacturer=" + manufacturer +
                ", receiver=" + receiver +
                ", carrier=" + carrier +
                ", shippingDate=" + shippingDate +
                ", bottleBoxIds=" + String.join(",", bottleBoxUuids) +
                ", shipmentValue=" + shipmentValue +
                '}';
    }
}
