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
import com.talentica.champy.bottle.box.BottleBox;
import com.talentica.champy.bottle.box.data.BottleBoxData;
import com.talentica.champy.bottle.info.ShipmentDeliveryInfo;
import scorex.core.NodeViewModifier$;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.talentica.champy.bottle.transaction.AppTransactionIdsEnum.DeliverShipmentOrderTransactionId;

// DeliverShipmentOrderTransaction is nested from AbstractRegularTransaction so support regular coins transmission as well.
// DeliverShipmentOrderTransaction is designed to deliver a ShipmentOrder from carrier to retailer.
// As outputs it contains possible RegularBoxes(to pay fee and make change) and new BottleBoxes entry.
// As unlockers it contains RegularBoxes and ShipmentOrderBox to open.
public class DeliverShipmentOrderTransaction extends AbstractRegularTransaction {
    private final ShipmentDeliveryInfo shipmentDeliveryInfo;
    private List<NoncedBox<Proposition>> newBoxes;

    public DeliverShipmentOrderTransaction(List<byte[]> inputRegularBoxIds,
                                           List<Signature25519> inputRegularBoxProofs,
                                           List<RegularBoxData> outputRegularBoxesData,
                                           ShipmentDeliveryInfo shipmentDeliveryInfo,
                                           long fee,
                                           long timestamp) {
        super(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, fee, timestamp);
        this.shipmentDeliveryInfo = shipmentDeliveryInfo;
    }

    // Override unlockers to contains regularBoxes from the parent class appended with ShipmentOrderBox entry.
    @Override
    public List<BoxUnlocker<Proposition>> unlockers() {
        // Get Regular unlockers from base class.
        List<BoxUnlocker<Proposition>> unlockers = super.unlockers();
        BoxUnlocker<Proposition> unlocker = new BoxUnlocker<Proposition>() {
            @Override
            public byte[] closedBoxId() {
                return shipmentDeliveryInfo.getShipmentOrderBoxToOpen().id();
            }
            @Override
            public Proof boxKey() {
                return shipmentDeliveryInfo.getShipmentOrderSpendingProof();
            }
        };
        // Append with the ShipmentOrderBox unlocker entry.
        unlockers.add(unlocker);
        return unlockers;
    }

    // Override newBoxes to contains regularBoxes from the parent class appended with BottleBox.
    // The nonce calculation algorithm for Boxes is the same as in parent class.
    @Override
    public List<NoncedBox<Proposition>> newBoxes() {
        if(newBoxes == null) {
            // Get new boxes from base class.
            newBoxes = new ArrayList<>(super.newBoxes());

            // Set BottleBox with retailer as owner.
            List<BottleBoxData> newBottleBoxesData = shipmentDeliveryInfo.getDeliveredBottleBoxesData();
            for(BottleBoxData boxData : newBottleBoxesData){
                long nonce = getNewBoxNonce(boxData.proposition(), newBoxes.size());
                newBoxes.add((NoncedBox) new BottleBox(boxData, nonce));
            }
        }
        return Collections.unmodifiableList(newBoxes);

    }
    @Override
    public byte transactionTypeId() {
        return DeliverShipmentOrderTransactionId.id();
    }

    @Override
    public TransactionSerializer serializer() {
        return DeliverShipmentOrderTransactionSerializer.getSerializer();
    }

    // Define object serialization, that should serialize both parent class entries and shipment delivery order box as well
    @Override
    public byte[] bytes() {
        ByteArrayOutputStream inputsIdsStream = new ByteArrayOutputStream();
        for(byte[] id: inputRegularBoxIds)
            inputsIdsStream.write(id, 0, id.length);

        byte[] inputRegularBoxIdsBytes = inputsIdsStream.toByteArray();

        byte[] inputRegularBoxProofsBytes = regularBoxProofsSerializer.toBytes(inputRegularBoxProofs);

        byte[] outputRegularBoxesDataBytes = regularBoxDataListSerializer.toBytes(outputRegularBoxesData);

        byte[] shipmentDeliveryInfoBytes = shipmentDeliveryInfo.bytes();

        return Bytes.concat(
                Longs.toByteArray(fee()),                               // 8 bytes
                Longs.toByteArray(timestamp()),                         // 8 bytes
                Ints.toByteArray(inputRegularBoxIdsBytes.length),       // 4 bytes
                inputRegularBoxIdsBytes,                                // depends on previous value (>=4 bytes)
                Ints.toByteArray(inputRegularBoxProofsBytes.length),    // 4 bytes
                inputRegularBoxProofsBytes,                             // depends on previous value (>=4 bytes)
                Ints.toByteArray(outputRegularBoxesDataBytes.length),   // 4 bytes
                outputRegularBoxesDataBytes,                            // depends on previous value (>=4 bytes)
                Ints.toByteArray(shipmentDeliveryInfoBytes.length),      // 4 bytes
                shipmentDeliveryInfoBytes                                // Shipment delivery info, output boxes can be constructed from this
        );
    }

    // Define object deserialization similar to 'toBytes()' representation.
    public static DeliverShipmentOrderTransaction parseBytes(byte[] bytes) {
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

        ShipmentDeliveryInfo shipmentDeliveryInfo = ShipmentDeliveryInfo.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new DeliverShipmentOrderTransaction(inputRegularBoxIds, inputRegularBoxProofs, outputRegularBoxesData, shipmentDeliveryInfo, fee, timestamp);
    }

}
