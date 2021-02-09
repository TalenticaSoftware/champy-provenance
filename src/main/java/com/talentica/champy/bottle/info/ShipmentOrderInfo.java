package com.talentica.champy.bottle.info;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.proof.Signature25519;
import com.horizen.proof.Signature25519Serializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.box.BottleBox;
import com.talentica.champy.bottle.box.BottleBoxSerializer;
import com.talentica.champy.bottle.box.data.ShipmentOrderBoxData;

import java.util.ArrayList;
import java.util.Arrays;

// CreateShipmentOrderInfo contains data required to construct CreateShipmentOrderTransaction specific inputs and outputs
public class ShipmentOrderInfo {
    public ArrayList<BottleBox> getBottleBoxesToOpen() {
        return bottleBoxesToOpen;
    }

    public ArrayList<Signature25519> getProofs() {
        return proofs;
    }

    public PublicKey25519Proposition getCarrierProposition() {
        return carrierProposition;
    }

    private final ArrayList<BottleBox> bottleBoxesToOpen;       // List of BottleBoxes to be shipped
    private final ArrayList<Signature25519> proofs;             // Proofs to unlock the boxes above
    private final PublicKey25519Proposition carrierProposition; // Shipment carrier - who's going to own the shipment till delivered.

    private final String shipmentId;                    //Unique shipment ID
    private final String manufacturer;                  //manufacturer public key
    private final String receiver;                      //Retailer - receiver public key
    private final String carrier;                       //Carrier public key
    private final String shippingDate;                  //Shipment date mm-dd-yyyy format
    private final long shipmentValue;                   //Total shipment value

    public ShipmentOrderInfo(ArrayList<BottleBox> bottleBoxesToOpen, ArrayList<Signature25519> proofs, PublicKey25519Proposition carrierProposition, String shipmentId, String manufacturer, String receiver, String carrier, String shippingDate, long shipmentValue) {
        this.bottleBoxesToOpen = bottleBoxesToOpen;
        this.proofs = proofs;
        this.carrierProposition = carrierProposition;
        this.shipmentId = shipmentId;
        this.manufacturer = manufacturer;
        this.receiver = receiver;
        this.carrier = carrier;
        this.shippingDate = shippingDate;
        this.shipmentValue = shipmentValue;
    }

    public ShipmentOrderBoxData getShipmentOrderBoxData() {
        ArrayList<String> bottleBoxUuids = new ArrayList<>(bottleBoxesToOpen.size());
        for(BottleBox bottleBox : bottleBoxesToOpen){
            bottleBoxUuids.add(bottleBox.getUuid());
        }
        return new ShipmentOrderBoxData(carrierProposition, shipmentId, manufacturer,
                receiver, carrier, shippingDate, bottleBoxUuids, shipmentValue);
    }

    // ShipmentOrderInfo minimal bytes representation.
    public byte[] bytes() {
        byte[] bottleBoxesToOpenBytes = new byte[]{};
        for (BottleBox bottleBox : bottleBoxesToOpen) {
            byte[] boxToOpenBytes = BottleBoxSerializer.getSerializer().toBytes(bottleBox);
            bottleBoxesToOpenBytes = Bytes.concat(bottleBoxesToOpenBytes, Ints.toByteArray(boxToOpenBytes.length),
                    boxToOpenBytes);
        }

        byte[] proofsBytes = new byte[]{};
        for(Signature25519 proof: proofs){
            byte[] proofBytes = Signature25519Serializer.getSerializer().toBytes(proof);
            proofsBytes = Bytes.concat(proofsBytes, Ints.toByteArray(proofBytes.length), proofBytes);
        }

        byte[] propositionBytes = PublicKey25519PropositionSerializer.getSerializer().toBytes(carrierProposition);

        return Bytes.concat(
                Ints.toByteArray(bottleBoxesToOpen.size()),
                bottleBoxesToOpenBytes,
                Ints.toByteArray(proofs.size()),
                proofsBytes,
                Ints.toByteArray(propositionBytes.length),
                propositionBytes,
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
                Longs.toByteArray(shipmentValue)
        );
    }

    // Define object deserialization similar to 'toBytes()' representation.
    public static ShipmentOrderInfo parseBytes(byte[] bytes) {
        int offset = 0;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        ArrayList<BottleBox> bottleBoxes = new ArrayList<>(batchSize);

        for (int i = 0; i < batchSize; ++i) {
            int boxSize = BytesUtils.getInt(bytes, offset);
            offset += 4;

            BottleBox bottleBox = BottleBoxSerializer.getSerializer().parseBytes(
                    Arrays.copyOfRange(bytes, offset, offset + boxSize));
            offset += boxSize;

            bottleBoxes.add(bottleBox);
        }

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        ArrayList<Signature25519> proofs = new ArrayList<>(batchSize);

        for (int i = 0; i < batchSize; ++i) {
            int proofSize = BytesUtils.getInt(bytes, offset);
            offset += 4;

            Signature25519 proof = Signature25519Serializer.getSerializer().parseBytes(
                    Arrays.copyOfRange(bytes, offset, offset + proofSize));
            offset += proofSize;

            proofs.add(proof);
        }

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        PublicKey25519Proposition carrierProposition = PublicKey25519PropositionSerializer.getSerializer()
                .parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        String shipmentId = new String(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        String manufacturer = new String(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        String receiver = new String(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        String carrier = new String(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        String shipmentDate = new String(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        long shipmentValue = BytesUtils.getLong(bytes, offset);

        return new ShipmentOrderInfo(bottleBoxes, proofs, carrierProposition, shipmentId,
                manufacturer, receiver, carrier, shipmentDate, shipmentValue);
    }
}