package com.talentica.champy.bottle.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.NoncedBox;
import com.horizen.box.data.RegularBoxData;
import com.horizen.proof.Signature25519;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.box.BottleBox;
import com.talentica.champy.bottle.box.data.BottleBoxData;
import com.talentica.champy.bottle.box.data.BottleBoxDataSerializer;
import scorex.core.NodeViewModifier$;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.talentica.champy.bottle.transaction.AppTransactionIdsEnum.CreateBottleTransactionId;
public class CreateBottleTransaction extends AbstractRegularTransaction {

    private final BottleBoxData outputBottleBoxData;
    private List<NoncedBox<Proposition>> newBoxes;

    public CreateBottleTransaction(List<byte[]> inputRegularBoxIds,
                                   List<Signature25519> inputRegularBoxProofs,
                                   List<RegularBoxData> outputRegularBoxesData,
                                   BottleBoxData outputBottleBoxData,
                                   long fee,
                                   long timestamp) {
        super(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, fee, timestamp);
        this.outputBottleBoxData = outputBottleBoxData;
    }

    @Override
    public byte transactionTypeId() {
        return CreateBottleTransactionId.id();
    }

    @Override
    public synchronized List<NoncedBox<Proposition>> newBoxes() {
        if(newBoxes == null) {
            newBoxes = new ArrayList<>(super.newBoxes());
            long nonce = getNewBoxNonce(outputBottleBoxData.proposition(), newBoxes.size());
            newBoxes.add((NoncedBox) new BottleBox(outputBottleBoxData, nonce));
        }
        return Collections.unmodifiableList(newBoxes);
    }

    // Define object serialization, that should serialize both parent class entries and BottleBoxData as well
    @Override
    public byte[] bytes() {
        ByteArrayOutputStream inputsIdsStream = new ByteArrayOutputStream();
        for(byte[] id: inputRegularBoxIds)
            inputsIdsStream.write(id, 0, id.length);

        byte[] inputRegularBoxIdsBytes = inputsIdsStream.toByteArray();

        byte[] inputRegularBoxProofsBytes = regularBoxProofsSerializer.toBytes(inputRegularBoxProofs);

        byte[] outputRegularBoxesDataBytes = regularBoxDataListSerializer.toBytes(outputRegularBoxesData);

        byte[] outputBottleBoxDataBytes = outputBottleBoxData.bytes();

        return Bytes.concat(
                Longs.toByteArray(fee()),                               // 8 bytes
                Longs.toByteArray(timestamp()),                         // 8 bytes
                Ints.toByteArray(inputRegularBoxIdsBytes.length),       // 4 bytes
                inputRegularBoxIdsBytes,                                // depends on previous value (>=4 bytes)
                Ints.toByteArray(inputRegularBoxProofsBytes.length),    // 4 bytes
                inputRegularBoxProofsBytes,                             // depends on previous value (>=4 bytes)
                Ints.toByteArray(outputRegularBoxesDataBytes.length),   // 4 bytes
                outputRegularBoxesDataBytes,                            // depends on previous value (>=4 bytes)
                Ints.toByteArray(outputBottleBoxDataBytes.length),         // 4 bytes
                outputBottleBoxDataBytes
        );
    }

    // Define object deserialization similar to 'toBytes()' representation.
    public static CreateBottleTransaction parseBytes(byte[] bytes) {
        int offset = 0;

        long fee = BytesUtils.getLong(bytes, offset);
        offset += 8;

        long timestamp = BytesUtils.getLong(bytes, offset);
        offset += 8;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        List<byte[]> inputRegularBoxIds = new ArrayList<>();
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

        BottleBoxData outputBottleBoxData = BottleBoxDataSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        return new CreateBottleTransaction(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, outputBottleBoxData, fee, timestamp);
    }

    @Override
    public TransactionSerializer serializer() {
        return CreateBottleTransactionSerializer.getSerializer();
    }
}
