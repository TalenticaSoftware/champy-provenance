package com.talentica.champy.bottle.info;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.proof.Signature25519;
import com.horizen.proof.Signature25519Serializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.box.ShipmentOrderBox;
import com.talentica.champy.bottle.box.ShipmentOrderBoxSerializer;
import com.talentica.champy.bottle.box.data.BottleBoxData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShipmentDeliveryInfo {
    private final ShipmentOrderBox  shipmentOrderBoxToOpen; // The Shipment Order Box to be delivered
    private final Signature25519 shipmentOrderSpendingProof;                  // Proof to unlock shipment order box
    private final PublicKey25519Proposition retailerProposition; // Public key of the retailer-shop owner

    public ShipmentDeliveryInfo(ShipmentOrderBox shipmentOrderBoxToOpen, Signature25519 shipmentOrderSpendingProof,
                                PublicKey25519Proposition retailerProposition) {
        this.shipmentOrderBoxToOpen = shipmentOrderBoxToOpen;
        this.shipmentOrderSpendingProof = shipmentOrderSpendingProof;
        this.retailerProposition = retailerProposition;
    }

    public ShipmentOrderBox getShipmentOrderBoxToOpen() {
        return shipmentOrderBoxToOpen;
    }

    public Signature25519 getShipmentOrderSpendingProof() {
        return shipmentOrderSpendingProof;
    }

    public PublicKey25519Proposition getRetailerProposition() {
        return retailerProposition;
    }

    public ArrayList<BottleBoxData> getDeliveredBottleBoxesData(){
        ArrayList<String> uuids = shipmentOrderBoxToOpen.getBottleBoxUuids();
        List<PublicKey25519Proposition> propositions = Collections.nCopies(uuids.size(), retailerProposition);
        String manufacturer = shipmentOrderBoxToOpen.getManufacturer();
        ArrayList<BottleBoxData> newBottleBoxesData = new ArrayList<>(uuids.size());
        int idx = 0;
        for(String bottleUuid : uuids){
            BottleBoxData boxData = new BottleBoxData(propositions.get(idx++), bottleUuid, manufacturer);
            newBottleBoxesData.add(boxData);
        }
        return  newBottleBoxesData;
    }

    // ShipmentDeliveryInfo minimal bytes representation.
    public byte[] bytes() {
        byte[] shipmentOrderBoxToOpenBytes = shipmentOrderBoxToOpen.bytes();

        byte[] proofsBytes = Signature25519Serializer.getSerializer().toBytes(shipmentOrderSpendingProof);

        byte[] propositionBytes = PublicKey25519PropositionSerializer.getSerializer().toBytes(retailerProposition);

        return Bytes.concat(
                Ints.toByteArray(shipmentOrderBoxToOpenBytes.length),
                shipmentOrderBoxToOpenBytes,
                Ints.toByteArray(proofsBytes.length),
                proofsBytes,
                Ints.toByteArray(propositionBytes.length),
                propositionBytes
        );
    }

    // Define object deserialization similar to 'toBytes()' representation.
    public static ShipmentDeliveryInfo parseBytes(byte[] bytes) {
        int offset = 0;

        int size = BytesUtils.getInt(bytes, offset);
        offset += 4;

        ShipmentOrderBox shipmentOrderBox = ShipmentOrderBoxSerializer.getSerializer().parseBytes(Arrays.
                copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = BytesUtils.getInt(bytes, offset);
        offset += 4;

        Signature25519 proof = Signature25519Serializer.getSerializer().parseBytes(
                Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = BytesUtils.getInt(bytes, offset);
        offset += 4;

        PublicKey25519Proposition retailerProposition = PublicKey25519PropositionSerializer.getSerializer()
                .parseBytes(Arrays.copyOfRange(bytes, offset, offset + size));

        return new ShipmentDeliveryInfo(shipmentOrderBox, proof, retailerProposition);
    }
}
