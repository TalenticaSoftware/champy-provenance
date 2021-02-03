package com.talentica.champy.bottle.transaction;

import com.horizen.transaction.TransactionSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class CreateBottleTransactionSerializer implements TransactionSerializer<CreateBottleTransaction> {

    private static final CreateBottleTransactionSerializer serializer = new CreateBottleTransactionSerializer();

    private CreateBottleTransactionSerializer() {
        super();
    }

    public static CreateBottleTransactionSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(CreateBottleTransaction transaction, Writer writer) {
        writer.putBytes(transaction.bytes());
    }

    @Override
    public CreateBottleTransaction parse(Reader reader) {
        return CreateBottleTransaction.parseBytes(reader.getBytes(reader.remaining()));
    }
}
