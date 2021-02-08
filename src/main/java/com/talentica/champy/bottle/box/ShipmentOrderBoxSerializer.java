package com.talentica.champy.bottle.box;

import com.horizen.box.BoxSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class ShipmentOrderBoxSerializer implements BoxSerializer<ShipmentOrderBox> {
    private static final ShipmentOrderBoxSerializer serializer = new ShipmentOrderBoxSerializer();

    private ShipmentOrderBoxSerializer() {
        super();
    }

    public static ShipmentOrderBoxSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(ShipmentOrderBox box, Writer writer) {
        writer.putBytes(box.bytes());
    }

    @Override
    public ShipmentOrderBox parse(Reader reader) {
        return ShipmentOrderBox.parseBytes(reader.getBytes(reader.remaining()));
    }
}

