package com.talentica.champy.bottle.transaction;

import com.horizen.transaction.TransactionSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class CreateShipmentOrderTransactionSerializer implements TransactionSerializer<CreateShipmentOrderTransaction> {

    private static CreateShipmentOrderTransactionSerializer serializer = new CreateShipmentOrderTransactionSerializer();

    private CreateShipmentOrderTransactionSerializer() {
        super();
    }

    public static CreateShipmentOrderTransactionSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(CreateShipmentOrderTransaction transaction, Writer writer) {
        writer.putBytes(transaction.bytes());
    }

    @Override
    public CreateShipmentOrderTransaction parse(Reader reader) {
        return CreateShipmentOrderTransaction.parseBytes(reader.getBytes(reader.remaining()));
    }
}
