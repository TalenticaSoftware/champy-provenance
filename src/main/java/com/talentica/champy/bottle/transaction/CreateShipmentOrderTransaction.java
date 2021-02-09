package com.talentica.champy.bottle.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.BoxUnlocker;
import com.horizen.box.NoncedBox;
import com.horizen.box.data.RegularBoxData;
import com.horizen.proof.Proof;
import com.horizen.proof.Signature25519;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.box.ShipmentOrderBox;
import com.talentica.champy.bottle.info.ShipmentOrderInfo;
import scorex.core.NodeViewModifier$;
import scorex.core.serialization.BytesSerializable;
import scorex.core.serialization.ScorexSerializer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.talentica.champy.bottle.transaction.AppTransactionIdsEnum.CreateShipmentOrderTransactionId;

// CreateShipmentOrderTransaction is nested from AbstractRegularTransaction so support regular coins transmission as well.
// CreateShipmentOrderTransaction is designed to create a ShipmentOrder for a group of BottleBoxes owned by the manufacturer.
// As outputs it contains possible RegularBoxes(to pay fee and make change) and new ShipmentOderBox entry.
// As unlockers it contains RegularBoxes and BottleBoxes to open.
public class CreateShipmentOrderTransaction extends AbstractRegularTransaction {
    private final ShipmentOrderInfo shipmentOrderInfo;
    private List<NoncedBox<Proposition>> newBoxes;

    public CreateShipmentOrderTransaction(  List<byte[]> inputRegularBoxIds,
                                            List<Signature25519> inputRegularBoxProofs,
                                            List<RegularBoxData> outputRegularBoxesData,
                                            ShipmentOrderInfo shipmentOrderInfo,
                                            long fee,
                                            long timestamp) {
        super(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, fee, timestamp);
        this.shipmentOrderInfo = shipmentOrderInfo;
    }

    // Specify the unique custom transaction id.
    @Override
    public byte transactionTypeId() {
        return CreateShipmentOrderTransactionId.id();
    }

    // Override unlockers to contains regularBoxes from the parent class appended with BottleBox entry to be opened.
    @Override
    public List<BoxUnlocker<Proposition>> unlockers() {
        // Get Regular unlockers from base class.
        List<BoxUnlocker<Proposition>> unlockers = super.unlockers();

        for(int i = 0; i < shipmentOrderInfo.getBottleBoxesToOpen().size(); ++i) {
            int finalI = i;
            BoxUnlocker<Proposition> unlocker = new BoxUnlocker<Proposition>() {
                @Override
                public byte[] closedBoxId() {
                    return shipmentOrderInfo.getBottleBoxesToOpen().get(finalI).id();
                }
                @Override
                public Proof boxKey() {
                    return shipmentOrderInfo.getProofs().get(finalI);
                }
            };
            // Append with the CarBox unlocker entry.
            unlockers.add(unlocker);
        }
        return unlockers;
    }

    // Override newBoxes to contain regularBoxes from the parent class appended with ShipmentOrderBox.
    // The nonce calculation algorithm for ShipmentOrderBox is the same as in parent class.
    @Override
    public List<NoncedBox<Proposition>> newBoxes() {
        if(newBoxes == null) {
            newBoxes = new ArrayList<>(super.newBoxes());
            long nonce = getNewBoxNonce(shipmentOrderInfo.getCarrierProposition(), newBoxes.size());
            newBoxes.add((NoncedBox) new ShipmentOrderBox(shipmentOrderInfo.getSellOrderBoxData(), nonce));
        }
        return Collections.unmodifiableList(newBoxes);
    }

    // Define object serialization, that should serialize both parent class entries and CarSellOrderInfo as well
    @Override
    public byte[] bytes() {
        ByteArrayOutputStream inputsIdsStream = new ByteArrayOutputStream();
        for(byte[] id: inputRegularBoxIds)
            inputsIdsStream.write(id, 0, id.length);

        byte[] inputRegularBoxIdsBytes = inputsIdsStream.toByteArray();

        byte[] inputRegularBoxProofsBytes = regularBoxProofsSerializer.toBytes(inputRegularBoxProofs);

        byte[] outputRegularBoxesDataBytes = regularBoxDataListSerializer.toBytes(outputRegularBoxesData);

        byte[] shipmentOrderInfoBytes = shipmentOrderInfo.bytes();

        return Bytes.concat(
                Longs.toByteArray(fee()),                               // 8 bytes
                Longs.toByteArray(timestamp()),                         // 8 bytes
                Ints.toByteArray(inputRegularBoxIdsBytes.length),       // 4 bytes
                inputRegularBoxIdsBytes,                                // depends on previous value (>=4 bytes)
                Ints.toByteArray(inputRegularBoxProofsBytes.length),    // 4 bytes
                inputRegularBoxProofsBytes,                             // depends on previous value (>=4 bytes)
                Ints.toByteArray(outputRegularBoxesDataBytes.length),   // 4 bytes
                outputRegularBoxesDataBytes,                            // depends on previous value (>=4 bytes)
                Ints.toByteArray(shipmentOrderInfoBytes.length),         // 4 bytes
                shipmentOrderInfoBytes                                   // depends on previous value (>=4 bytes)
        );
    }

    // Define object deserialization similar to 'toBytes()' representation.
    public static CreateShipmentOrderTransaction parseBytes(byte[] bytes) {
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

        ShipmentOrderInfo shipmentOrderInfo = ShipmentOrderInfo.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new CreateShipmentOrderTransaction(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, shipmentOrderInfo, fee, timestamp);
    }

        @Override
    public TransactionSerializer serializer() {
        return CreateShipmentOrderTransactionSerializer.getSerializer();
    }
}
