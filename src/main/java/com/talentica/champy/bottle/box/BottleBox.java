package com.talentica.champy.bottle.box;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.horizen.box.AbstractNoncedBox;
import com.horizen.box.BoxSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;
import com.talentica.champy.bottle.box.data.BottleBoxData;
import com.talentica.champy.bottle.box.data.BottleBoxDataSerializer;

import java.util.Arrays;

import static com.talentica.champy.bottle.box.AppBoxesIdEnum.BottleBoxId;

@JsonView(Views.Default.class)
@JsonIgnoreProperties({"bottleId", "value"})
public final class BottleBox extends AbstractNoncedBox<PublicKey25519Proposition, BottleBoxData, BottleBox> {

    public BottleBox(BottleBoxData boxData, long nonce) {
        super(boxData, nonce);
    }

    @Override
    public BoxSerializer serializer() {
        return BottleBoxSerializer.getSerializer();
    }

    @Override
    public byte boxTypeId() {
        return BottleBoxId.id();
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                Longs.toByteArray(nonce),
                BottleBoxDataSerializer.getSerializer().toBytes(boxData)
        );
    }

    public static BottleBox parseBytes(byte[] bytes){
        long nonce = Longs.fromByteArray(Arrays.copyOf(bytes, Longs.BYTES));
        BottleBoxData boxData = BottleBoxDataSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, Longs.BYTES, bytes.length));

        return new BottleBox(boxData, nonce);
    }

    public String getUuid() {return boxData.getUuid();}

    public String getManufacturer() {return boxData.getManufacturer();}

    public byte[] getBottleId() {
        return Bytes.concat(
                getUuid().getBytes(),
                getManufacturer().getBytes());
    }
}
