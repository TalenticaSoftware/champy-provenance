package com.talentica.champy.bottle.box;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.horizen.box.AbstractNoncedBox;
import com.horizen.box.BoxSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;
import com.talentica.champy.bottle.box.data.ShipmentOrderBoxData;
import com.talentica.champy.bottle.box.data.ShipmentOrderBoxDataSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.talentica.champy.bottle.box.AppBoxesIdEnum.ShipmentOrderBoxId;

@JsonView(Views.Default.class)
@JsonIgnoreProperties({"shipmentId", "value"})
public final class ShipmentOrderBox extends AbstractNoncedBox<PublicKey25519Proposition, ShipmentOrderBoxData, ShipmentOrderBox> {
    public ShipmentOrderBox(ShipmentOrderBoxData boxData, long nonce) {
        super(boxData, nonce);
    }

    public static ShipmentOrderBox parseBytes(byte[] bytes) {
        long nonce = Longs.fromByteArray(Arrays.copyOf(bytes, Longs.BYTES));
        ShipmentOrderBoxData boxData = ShipmentOrderBoxDataSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, Longs.BYTES, bytes.length));

        return new ShipmentOrderBox(boxData, nonce);
    }

    public String getShipmentId() {return boxData.getShipmentId();}

    public String getManufacturer() {
        return boxData.getManufacturer();
    }

    public String getReceiver() {
        return boxData.getReceiver();
    }

    public String getCarrier() {
        return boxData.getCarrier();
    }

    public List<String> getBottleBoxUuids() {
        return boxData.getBottleBoxUuids();
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                Longs.toByteArray(nonce),
                ShipmentOrderBoxDataSerializer.getSerializer().toBytes(boxData)
        );
    }

    @Override
    public BoxSerializer serializer() {
        return ShipmentOrderBoxSerializer.getSerializer();
    }

    @Override
    public byte boxTypeId() {
        return ShipmentOrderBoxId.id();
    }
}