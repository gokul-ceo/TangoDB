---
sidebar_position: 1
title: Quickstart
---

# Quickstart Guide

Get up and running with TangoDB in modern Java.

---

## Prerequisites

TangoDB leverages Java's Foreign Function & Memory (FFM) API (`java.lang.foreign`), which requires **JDK 22** or higher (JDK 24 recommended).

Ensure your environment satisfies:
* **Java Development Kit (JDK)**: JDK 22+ (JDK 24 recommended)
* **JVM Flags**: `--enable-native-access=ALL-UNNAMED` or specific module authorization when using FFM API native memory access.

```bash
java --version
# OpenJDK 22+ or 24+
```

---

## Installation / Build Setup

To use TangoDB in your Java project, include the artifact or build the JAR using Apache Maven:

```bash
# Clone the repository
git clone https://github.com/gokul-ceo/TangoDB.git
cd TangoDB

# Build the project
mvn clean package -DskipTests
```

### Maven Dependency

```xml
<dependency>
    <groupId>io.tango</groupId>
    <artifactId>tangodb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## Basic Usage Example

Here is a complete example demonstrating how to initialize `TangoDB`, perform concurrent key-value mutations, retrieve entries, delete entries, and gracefully release off-heap resources.

```java
package io.tango.example;

import io.tango.api.TangoConfig;
import io.tango.api.TangoDB;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class BasicExample {

    public static void main(String[] args) {
        // 1. Configure TangoDB parameters
        TangoConfig config = TangoConfig.builder()
                .sstableDirectory(Path.of("data"))
                .memTableSize(64L * 1024 * 1024) // 64 MiB MemTable
                .arenaBlockSize(64L * 1024)      // 64 KiB Arena Block
                .flushQueueSize(8)
                .build();

        // 2. Open database instance
        try (TangoDB db = TangoDB.open(config)) {
            
            byte[] key = "user:1001:profile".getBytes(StandardCharsets.UTF_8);
            byte[] value = "{\"name\": \"Alice\", \"role\": \"Admin\"}".getBytes(StandardCharsets.UTF_8);

            // 3. PUT - Insert or update key-value pair
            db.put(key, value);
            System.out.println("Inserted key successfully.");

            // 4. GET - Retrieve value by key
            byte[] retrievedValue = db.get(key);
            if (retrievedValue != null) {
                System.out.println("Retrieved: " + new String(retrievedValue, StandardCharsets.UTF_8));
            }

            // 5. DELETE - Write tombstone record
            db.delete(key);
            System.out.println("Deleted key.");

            // Verify deletion
            byte[] postDelete = db.get(key);
            System.out.println("Post-delete GET result: " + postDelete); // null
        }
    }
}
```

---

## Running with JVM Native Access

When executing your application, ensure you add the native access flag so the FFM API can allocate off-heap memory without warnings:

```bash
java --enable-native-access=ALL-UNNAMED -jar your-app.jar
```
