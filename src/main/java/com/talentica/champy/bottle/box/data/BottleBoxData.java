package com.talentica.champy.bottle.box.data;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.box.data.AbstractNoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import com.talentica.champy.bottle.box.BottleBox;

import scorex.crypto.hash.Blake2b256;

import java.util.Arrays;

import static com.talentica.champy.bottle.box.data.AppBoxesDataIdsEnum.BottleBoxDataId;

@JsonView(Views.Default.class)
public class BottleBoxData extends AbstractNoncedBoxData<PublicKey25519Proposition, BottleBox, BottleBoxData> {
    // attributes defined for Bottle:
    private final String uuid;
    private final String manufacturer;
    private final int year;

    public BottleBoxData(PublicKey25519Proposition proposition, String uuid, String manufacturer, int year) {
        //Zen equivalent value is set to 1
        super(proposition, 1);
        this.uuid = uuid;
        this.manufacturer = manufacturer;
        this.year = year;
    }

    public String getId(){return uuid;}

    public String getManufacturer(){return manufacturer;}

    public int getYear(){return year;}

    @Override
    public BottleBox getBox(long nonce) {
        return new BottleBox(this, nonce);
    }

    @Override
    public byte[] customFieldsHash() {
        return Blake2b256.hash(
                Bytes.concat(
                        uuid.getBytes(),
                        manufacturer.getBytes(),
                        Ints.toByteArray(year)));
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                proposition().bytes(),
                Ints.toByteArray(uuid.getBytes().length),
                uuid.getBytes(),
                Ints.toByteArray(manufacturer.length()),
                manufacturer.getBytes(),
                Ints.toByteArray(year)
        );
    }

    public static BottleBoxData parseBytes(byte[] bytes){
        int offset = 0;
        PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer()
                .parseBytes(Arrays.copyOf(bytes, PublicKey25519Proposition.getLength()));
        offset += PublicKey25519Proposition.getLength();

        int size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String id = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));
        offset += Ints.BYTES;

        String manufacturer = new String(Arrays.copyOfRange(bytes, offset, offset+size));
        offset += size;

        int year = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset+Ints.BYTES));

        return new BottleBoxData(proposition, id, manufacturer, year);
    }

    @Override
    public NoncedBoxDataSerializer serializer() {
        return BottleBoxDataSerializer.getSerializer();
    }

    @Override
    public byte boxDataTypeId() {
        return BottleBoxDataId.id();
    }

    @Override
    public String toString() {
        return "BottleBoxData{" +
                "uuid=" + uuid +
                ", proposition=" + proposition() +
                ", manufacturer=" + manufacturer +
                ", year=" + year +
                '}';
    }
}
