package com.talentica.champy.bottle.box.data;

import com.horizen.box.data.NoncedBoxDataSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class ShipmentOrderBoxDataSerializer implements NoncedBoxDataSerializer<ShipmentOrderBoxData> {

    private static final ShipmentOrderBoxDataSerializer serializer = new ShipmentOrderBoxDataSerializer();

    private ShipmentOrderBoxDataSerializer() {
        super();
    }

    public static ShipmentOrderBoxDataSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(ShipmentOrderBoxData shipmentOrderBoxData, Writer writer) {
        writer.putBytes(shipmentOrderBoxData.bytes());
    }

    @Override
    public ShipmentOrderBoxData parse(Reader reader) {
        return ShipmentOrderBoxData.parseBytes(reader.getBytes(reader.remaining()));
    }
}
