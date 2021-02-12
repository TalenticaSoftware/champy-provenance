package com.talentica.champy.bottle.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.BoxUnlocker;
import com.horizen.box.data.RegularBoxData;
import com.horizen.proof.Proof;
import com.horizen.proof.Signature25519;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.info.SellBottleInfo;
import scorex.core.NodeViewModifier$;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.talentica.champy.bottle.transaction.AppTransactionIdsEnum.SellBottleTransactionId;

// SellBottleTransaction is nested from AbstractRegularTransaction so support regular coins transmission as well.
// SellBottleTransaction is designed to sell a Bottle from retailer to consumer.
// As outputs it contains possible RegularBoxes(to pay fee and make change).
// As unlockers it contains RegularBoxes and BottleBox to open.
public class SellBottleTransaction extends AbstractRegularTransaction {

    private final SellBottleInfo sellBottleInfo;

    public SellBottleTransaction(List<byte[]> inputRegularBoxIds,
                                 List<Signature25519> inputRegularBoxProofs,
                                 List<RegularBoxData> outputRegularBoxesData,
                                 SellBottleInfo sellBottleInfo,
                                 long fee,
                                 long timestamp) {
        super(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, fee, timestamp);
        this.sellBottleInfo = sellBottleInfo;
    }

    // Override unlockers to contains regularBoxes from the parent class appended with BottleBox entry.
    @Override
    public List<BoxUnlocker<Proposition>> unlockers() {
        // Get Regular unlockers from base class.
        List<BoxUnlocker<Proposition>> unlockers = super.unlockers();
        BoxUnlocker<Proposition> unlocker = new BoxUnlocker<Proposition>() {
            @Override
            public byte[] closedBoxId() {
                return sellBottleInfo.getBottleBoxToOpen().id();
            }
            @Override
            public Proof boxKey() {
                return sellBottleInfo.getBottleBoxSpendingProof();
            }
        };
        // Append with the ShipmentOrderBox unlocker entry.
        unlockers.add(unlocker);
        return unlockers;
    }

    @Override
    public byte transactionTypeId() {
        return SellBottleTransactionId.id();
    }

    // Define object serialization, that should serialize both parent class entries and BottleBox as well
    @Override
    public byte[] bytes() {
        ByteArrayOutputStream inputsIdsStream = new ByteArrayOutputStream();
        for(byte[] id: inputRegularBoxIds)
            inputsIdsStream.write(id, 0, id.length);

        byte[] inputRegularBoxIdsBytes = inputsIdsStream.toByteArray();

        byte[] inputRegularBoxProofsBytes = regularBoxProofsSerializer.toBytes(inputRegularBoxProofs);

        byte[] outputRegularBoxesDataBytes = regularBoxDataListSerializer.toBytes(outputRegularBoxesData);

        byte[] sellBottleInfoBytes = sellBottleInfo.bytes();

        return Bytes.concat(
                Longs.toByteArray(fee()),                               // 8 bytes
                Longs.toByteArray(timestamp()),                         // 8 bytes
                Ints.toByteArray(inputRegularBoxIdsBytes.length),       // 4 bytes
                inputRegularBoxIdsBytes,                                // depends on previous value (>=4 bytes)
                Ints.toByteArray(inputRegularBoxProofsBytes.length),    // 4 bytes
                inputRegularBoxProofsBytes,                             // depends on previous value (>=4 bytes)
                Ints.toByteArray(outputRegularBoxesDataBytes.length),   // 4 bytes
                outputRegularBoxesDataBytes,                            // depends on previous value (>=4 bytes)
                Ints.toByteArray(sellBottleInfoBytes.length),      // 4 bytes
                sellBottleInfoBytes                                // sell bottle info
        );
    }


    public static SellBottleTransaction parseBytes(byte[] bytes) {
        int offset = 0;

        long fee = BytesUtils.getLong(bytes, offset);
        offset += 8;

        long timestamp = BytesUtils.getLong(bytes, offset);
        offset += 8;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        ArrayList<byte[]> inputRegularBoxIds = new ArrayList<>();
        int idLength = NodeViewModifier$.MODULE$.ModifierIdSize();
        while(batchSize > 0) {
            inputRegularBoxIds.add(Arrays.copyOfRange(bytes, offset, offset + idLength));
            offset += idLength;
            batchSize -= idLength;
        }

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        List<Signature25519> inputRegularBoxProofs = regularBoxProofsSerializer.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        List<RegularBoxData> outputRegularBoxesData = regularBoxDataListSerializer.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        SellBottleInfo sellBottleInfo = SellBottleInfo.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new SellBottleTransaction(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, sellBottleInfo, fee, timestamp);
    }

    @Override
    public TransactionSerializer serializer() {
        return SellBottleTransactionSerializer.getSerializer();
    }
}
