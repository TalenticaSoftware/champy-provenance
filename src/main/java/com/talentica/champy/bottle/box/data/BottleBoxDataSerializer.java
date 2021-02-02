package com.talentica.champy.bottle.box.data;

import akka.util.ByteString;
import com.horizen.box.data.NoncedBoxDataSerializer;
import scala.util.Try;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class BottleBoxDataSerializer implements NoncedBoxDataSerializer<BottleBoxData> {

    private static final BottleBoxDataSerializer serializer = new BottleBoxDataSerializer();

    private BottleBoxDataSerializer() {
        super();
    }

    public static BottleBoxDataSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(BottleBoxData bottleBoxData, Writer writer) {
        writer.putBytes(bottleBoxData.bytes());
    }

    @Override
    public BottleBoxData parse(Reader reader) {
        return BottleBoxData.parseBytes(reader.getBytes(reader.remaining()));
    }
}
