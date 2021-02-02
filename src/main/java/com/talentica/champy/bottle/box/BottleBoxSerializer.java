package com.talentica.champy.bottle.box;

import com.horizen.box.BoxSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class BottleBoxSerializer implements BoxSerializer<BottleBox> {
    private static final BottleBoxSerializer serializer = new BottleBoxSerializer();

    private BottleBoxSerializer() {
        super();
    }

    public static BottleBoxSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(BottleBox box, Writer writer) {
        writer.putBytes(box.bytes());
    }

    @Override
    public BottleBox parse(Reader reader) {
        return BottleBox.parseBytes(reader.getBytes(reader.remaining()));
    }
}
