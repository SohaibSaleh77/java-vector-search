# super simple Vector DB for similarity search


Not suitable for real use. Not optimized. Built for learning.

## What it does

Insert vectors with an ID and metadata. Query with a vector and get back the
K closest matches. Cosine and Euclidean distances are supported.

## How it works

Inserts go into an in-memory store. When it fills up, the contents are written
to a memory-mapped file on disk via a temp file and atomic rename. Search runs
across all stores in parallel using virtual threads, collects each store's
top-K, then merges them into a final top-K.

Duplicate IDs are rejected. On restart, existing segment files are reloaded
and their dimensions are validated against the engine's configured dimensions.

## Project layout

    vdb/
      Engine.java           insert, search, close
      model/
        Doc.java            id + vector + metadata
        Result.java         id + distance + metadata
      metric/
        Metric.java         distance interface
        Cosine.java         1 - cosine similarity
        Euclidean.java      L2 distance
      store/
        Store.java          common interface
        MemStore.java       in-RAM store with read/write lock
        DiskStore.java      memory-mapped file store (read-only)
        Flusher.java        writes docs to a .seg file (temp + rename)

## Build

    mvn clean test

Requires JDK 21+ and Maven 3.6+.

## File format

Each .seg file is binary:

    [dims:int][count:int]
    per doc: [idLen:int][id:bytes][vector:float x dims][metaLen:int][meta:bytes]
