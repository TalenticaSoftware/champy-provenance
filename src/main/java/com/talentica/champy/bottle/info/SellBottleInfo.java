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
import com.talentica.champy.bottle.box.ShipmentOrderBox;
import com.talentica.champy.bottle.box.ShipmentOrderBoxSerializer;

import java.util.Arrays;

public class SellBottleInfo {
    private final BottleBox bottleBoxToOpen; // The Bottle Box to be sold
    private final Signature25519 bottleBoxSpendingProof;   // Proof to unlock bottle box
    private final long sellingPrice;

    public SellBottleInfo(BottleBox bottleBoxToOpen, Signature25519 bottleBoxSpendingProof, long sellingPrice) {
        this.bottleBoxToOpen = bottleBoxToOpen;
        this.bottleBoxSpendingProof = bottleBoxSpendingProof;
        this.sellingPrice = sellingPrice;
    }

    public BottleBox getBottleBoxToOpen() {
        return bottleBoxToOpen;
    }

    public Signature25519 getBottleBoxSpendingProof() {
        return bottleBoxSpendingProof;
    }

    public long getSellingPrice() {
        return sellingPrice;
    }

    // SellBottleInfo minimal bytes representation.
    public byte[] bytes() {
        byte[] bottleBoxToOpenBytes = bottleBoxToOpen.bytes();

        byte[] proofsBytes = Signature25519Serializer.getSerializer().toBytes(bottleBoxSpendingProof);

        return Bytes.concat(
                Ints.toByteArray(bottleBoxToOpenBytes.length),
                bottleBoxToOpenBytes,
                Ints.toByteArray(proofsBytes.length),
                proofsBytes,
                Longs.toByteArray(sellingPrice)
        );
    }

    // Define object deserialization similar to 'toBytes()' representation.
    public static SellBottleInfo parseBytes(byte[] bytes) {
        int offset = 0;

        int size = BytesUtils.getInt(bytes, offset);
        offset += 4;

        BottleBox bottleBox = BottleBoxSerializer.getSerializer().parseBytes(Arrays.
                copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = BytesUtils.getInt(bytes, offset);
        offset += 4;

        Signature25519 proof = Signature25519Serializer.getSerializer().parseBytes(
                Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        long price = BytesUtils.getLong(bytes, offset);

        return new SellBottleInfo( bottleBox, proof, price);
    }

}
