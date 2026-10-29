# Java Vector Search

A simple, educational Java implementation of vector similarity search.

> ⚠️ **Not intended for production use.** This project prioritizes learning and clarity over performance and optimization.

## What it does

Stores vectors with an ID and metadata, then retrieves the **K nearest matches** for a query vector. Supports cosine distance and Euclidean distance.

## How it works

Vectors are initially stored in memory. When the in-memory store reaches capacity, its contents are flushed to a memory-mapped file on disk using a temporary file and atomic rename.

Search runs across all stores in parallel using Java virtual threads. Each store returns its local top-K results, which are then merged to produce the final top-K matches.

Duplicate IDs are rejected. On restart, existing segment files are reloaded, and their vector dimensions are validated against the engine's configured dimensions.

## Project layout

```text
java-vector-search/
└── src/main/java/
    └── vdb/
        ├── Engine.java          # insert, search, close
        ├── model/
        │   ├── Doc.java         # ID, vector, and metadata
        │   └── Result.java      # ID, distance, and metadata
        ├── metric/
        │   ├── Metric.java      # distance metric interface
        │   ├── Cosine.java      # 1 - cosine similarity
        │   └── Euclidean.java   # Euclidean (L2) distance
        └── store/
            ├── Store.java       # common store interface
            ├── MemStore.java    # in-memory store with read/write lock
            ├── DiskStore.java   # read-only memory-mapped file store
            └── Flusher.java     # writes .seg files using temp + rename
```

## Requirements

- JDK 21 or later
- Maven 3.6 or later

## Build and test

Run the test suite with:

```bash
mvn clean test
```

## File format

Each `.seg` file uses a binary format:

```text
[dims:int][count:int]

For each document:
[idLen:int]
[id:bytes]
[vector:float × dims]
[metaLen:int]
[meta:bytes]
```

The header stores the vector dimensions and document count. Each document contains its ID, vector values, and metadata.

---

**Educational project** — provided as-is, without warranty. Built to explore vector similarity search, in-memory storage, disk-backed segments, and parallel search in Java.
