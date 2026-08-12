package io.tango.api;

import io.tango.engine.StorageEngine;

final class TangoDBImpl  implements TangoDB{

    private final StorageEngine engine;

    TangoDBImpl(TangoConfig config) {
        this.engine = new StorageEngine(config);
    }

    @Override
    public void put(byte[] key, byte[] value) {
        engine.put(key, value);
    }

    @Override
    public byte[] get(byte[] key) {
        return engine.get(key);
    }

    @Override
    public void delete(byte[] key) {
        engine.delete(key);
    }

    @Override
    public void close() {
        engine.close();
    }
}
