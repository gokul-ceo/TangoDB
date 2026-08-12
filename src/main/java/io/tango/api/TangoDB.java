package io.tango.api;

public interface TangoDB {

    void put(byte[] key, byte[] value);

    byte[] get(byte[] key);

    void delete(byte[] key);

    void close();

    static TangoDB open(TangoConfig config) {
        return new TangoDBImpl(config);
    }
}
