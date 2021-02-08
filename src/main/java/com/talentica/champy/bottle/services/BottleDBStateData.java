package com.talentica.champy.bottle.services;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

import java.util.Arrays;

// This state will be stored for every bottle
public class BottleDBStateData {
    private final String uuid;
    private final BottleStateEnum state;
    private final String createTransactionId;

    public BottleDBStateData(String uuid, BottleStateEnum state, String createTransactionId) {
        this.uuid = uuid;
        this.state = state;
        this.createTransactionId = createTransactionId;
    }

    // Define serialization of State Object
    public byte[] bytes() {
        return Bytes.concat(
                Ints.toByteArray(uuid.length()),
                uuid.getBytes(),
                new byte [] {state.state()},
                Ints.toByteArray(createTransactionId.length()),
                createTransactionId.getBytes()
        );
    }

    // Define deserialization of state object
    public static BottleDBStateData parseBytes(byte[] bytes) {
        int offset = 0;

        int size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String uuid = new String(Arrays.copyOfRange(bytes, offset, offset + size));
        offset += size;

        BottleStateEnum state =  BottleStateEnum.get(bytes[offset]);
        offset += 1;

        size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
        offset += Ints.BYTES;

        String createTransactionId = new String(Arrays.copyOfRange(bytes, offset, offset + size));

        return new BottleDBStateData(uuid, state, createTransactionId);
    }
}
