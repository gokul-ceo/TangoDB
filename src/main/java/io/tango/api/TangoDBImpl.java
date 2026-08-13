package io.tango.api;

import io.tango.engine.StorageEngine;

/**
 * Package-private implementation of the {@link TangoDB} interface delegating storage operations to {@link StorageEngine}.
 *
 * @author Gokul G
 * @version 0.1.1
 */
final class TangoDBImpl implements TangoDB {

    private final StorageEngine engine;

    /**
     * Constructs a new {@code TangoDBImpl} wrapping a {@link StorageEngine} initialized with the given config.
     *
     * @param config non-null {@link TangoConfig} instance
     */
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
