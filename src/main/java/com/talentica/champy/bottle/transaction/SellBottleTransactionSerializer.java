package com.talentica.champy.bottle.transaction;

import com.horizen.transaction.TransactionSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class SellBottleTransactionSerializer implements TransactionSerializer<SellBottleTransaction> {

    private static SellBottleTransactionSerializer serializer = new SellBottleTransactionSerializer();

    private SellBottleTransactionSerializer() {
        super();
    }

    public static SellBottleTransactionSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(SellBottleTransaction transaction, Writer writer) {
        writer.putBytes(transaction.bytes());
    }

    @Override
    public SellBottleTransaction parse(Reader reader) {
        return SellBottleTransaction.parseBytes(reader.getBytes(reader.remaining()));
    }
}
