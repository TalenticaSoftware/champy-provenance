package com.talentica.champy.bottle.transaction;

import com.horizen.transaction.TransactionSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class DeliverShipmentOrderTransactionSerializer implements TransactionSerializer<DeliverShipmentOrderTransaction> {

    private static DeliverShipmentOrderTransactionSerializer serializer = new DeliverShipmentOrderTransactionSerializer();

    private DeliverShipmentOrderTransactionSerializer() {
        super();
    }

    public static DeliverShipmentOrderTransactionSerializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(DeliverShipmentOrderTransaction transaction, Writer writer) {
        writer.putBytes(transaction.bytes());
    }

    @Override
    public DeliverShipmentOrderTransaction parse(Reader reader) {
        return DeliverShipmentOrderTransaction.parseBytes(reader.getBytes(reader.remaining()));
    }
}
