package io.tango.api;

/**
 * Main public entry point and API interface for the TangoDB storage engine.
 * <p>
 * TangoDB is an experimental high-performance embedded key-value storage engine
 * designed for high throughput, low latency, and efficient memory usage using
 * Java's Foreign Function &amp; Memory (FFM) API.
 * </p>
 *
 * <pre>{@code
 * TangoConfig config = TangoConfig.builder()
 *         .sstableDirectory(Path.of("data"))
 *         .build();
 *
 * try (TangoDB db = TangoDB.open(config)) {
 *     db.put("key1".getBytes(), "value1".getBytes());
 *     byte[] val = db.get("key1".getBytes());
 *     db.delete("key1".getBytes());
 * }
 * }</pre>
 *
 * @author Gokul G
 * @version 0.1.1
 */
public interface TangoDB extends AutoCloseable {

    /**
     * Inserts or updates a key-value record in the database.
     *
     * @param key   non-null byte array representing the record key
     * @param value non-null byte array representing the payload value
     */
    void put(byte[] key, byte[] value);

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key non-null byte array representing the record key
     * @return byte array containing the value if present; {@code null} if the key does not exist or was deleted
     */
    byte[] get(byte[] key);

    /**
     * Deletes the record associated with the specified key by writing a tombstone entry.
     *
     * @param key non-null byte array representing the record key to delete
     */
    void delete(byte[] key);

    /**
     * Gracefully closes the database instance, waiting for pending flushes and releasing native resources.
     */
    @Override
    void close();

    /**
     * Opens and initializes a new TangoDB database instance using the provided configuration.
     *
     * @param config non-null {@link TangoConfig} instance containing engine parameters
     * @return initialized {@link TangoDB} instance
     */
    static TangoDB open(TangoConfig config) {
        return new TangoDBImpl(config);
    }
}
