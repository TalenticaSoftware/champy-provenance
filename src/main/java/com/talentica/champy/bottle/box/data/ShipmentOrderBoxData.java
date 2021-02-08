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

import static com.talentica.champy.bottle.box.data.AppBoxesDataIdsEnum.ShipmentOrderBoxDataId;

@JsonView(Views.Default.class)
public class ShipmentOrderBoxData extends AbstractNoncedBoxData<PublicKey25519Proposition, ShipmentOrderBox, ShipmentOrderBoxData> {
    // attributes defined for ShipmentOrder:
    private final String shipmentId;    //Unique shipment ID
    private final String sender;        //sender public key
    private final String receiver;      //Retailer - receiver public key
    private final String carrier;       //Carrier public key
    private final String shippingDate;    //Shipment date mm-dd-yyyy format
    private final ArrayList<String> bottleIds;  //List of bottle Ids shipped within this shipment
    private final long shipmentValue;           //Total shipment value

    public ShipmentOrderBoxData(PublicKey25519Proposition proposition, String shipmentId, String sender, String receiver, String carrier, String shippingDate, ArrayList<String> bottleIds, long shipmentValue) {
        super(proposition, 1);
        this.shipmentId = shipmentId;
        this.sender = sender;
        this.receiver = receiver;
        this.carrier = carrier;
        this.shippingDate = shippingDate;
        this.bottleIds = bottleIds;
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
                        sender.getBytes(),
                        receiver.getBytes()));
    }

    @Override
    public byte[] bytes() {
        byte [] bottleIdsBytes = new byte[] {};

        //Collect Bytes from all the bottle ids
        bottleIds.forEach( (String bottleId) -> Bytes.concat(bottleIdsBytes,
                Ints.toByteArray(bottleId.length()),
                bottleId.getBytes()));

        return Bytes.concat(
                proposition().bytes(),
                Ints.toByteArray(shipmentId.length()),
                shipmentId.getBytes(),
                Ints.toByteArray(sender.length()),
                sender.getBytes(),
                Ints.toByteArray(receiver.length()),
                receiver.getBytes(),
                Ints.toByteArray(carrier.length()),
                carrier.getBytes(),
                Ints.toByteArray(shippingDate.length()),
                shippingDate.getBytes(),
                Ints.toByteArray(bottleIds.size()),
                bottleIdsBytes,
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

        String sender = new String(Arrays.copyOfRange(bytes, offset, offset+size));
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

        //Number of bottleIds
        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        ArrayList<String> bottleIds = new ArrayList<>(size);
        for(int i=0; i<size; ++i){
            size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
            offset += Ints.BYTES;

            String bottleId = new String(Arrays.copyOfRange(bytes, offset, offset+size));
            offset += size;

            bottleIds.add(bottleId);
        }

        long shipmentValue = Longs.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Longs.BYTES));

        return new ShipmentOrderBoxData(proposition, shipmentId, sender, receiver, carrier, shipmentDate, bottleIds, shipmentValue );
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

    public String getSender() {
        return sender;
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

    public ArrayList<String> getBottleIds() {
        return bottleIds;
    }

    public long getShipmentValue() {
        return shipmentValue;
    }

    @Override
    public String toString() {
        return "ShipmentOrderBoxData{" +
                "shipmentId=" + shipmentId +
                ", proposition=" + proposition() +
                ", sender=" + sender +
                ", receiver=" + receiver +
                ", carrier=" + carrier +
                ", shippingDate=" + shippingDate +
                ", bottleIds=" + String.join(",", bottleIds) +
                ", shipmentValue=" + shipmentValue +
                '}';
    }
}
