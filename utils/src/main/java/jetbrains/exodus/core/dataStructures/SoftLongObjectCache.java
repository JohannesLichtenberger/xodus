/**
 * Copyright 2010 - 2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.core.dataStructures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;

public class SoftLongObjectCache<V> {

    public static final int DEFAULT_SIZE = 4096;
    public static final int MIN_SIZE = 16;

    private final SoftReference<LongObjectCache<V>>[] chunks;
    private final int chuckSize;
    private long attempts;
    private long hits;

    public SoftLongObjectCache() {
        this(DEFAULT_SIZE);
    }

    public SoftLongObjectCache(int cacheSize) {
        if (cacheSize < MIN_SIZE) {
            cacheSize = MIN_SIZE;
        }
        //noinspection unchecked
        chunks = new SoftReference[SoftObjectCacheBase.computeNumberOfChunks(cacheSize)];
        chuckSize = cacheSize / chunks.length;
        clear();
    }

    public void clear() {
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = null;
        }
        attempts = 0L;
        hits = 0L;
    }

    public double hitRate() {
        return attempts == 0 ? 0 : ((double) hits) / ((double) attempts);
    }

    public V tryKey(final long key) {
        ++attempts;
        final LongObjectCache<V> chunk = getChunk(key, false);
        final V result = chunk == null ? null : chunk.tryKey(key);
        if (result != null) {
            ++hits;
        }
        return result;
    }

    public V getObject(final long key) {
        final LongObjectCache<V> chunk = getChunk(key, false);
        return chunk == null ? null : chunk.getObject(key);
    }

    public void cacheObject(final long key, @NotNull final V value) {
        final LongObjectCache<V> chunk = getChunk(key, true);
        assert chunk != null;
        chunk.cacheObject(key, value);
    }

    public void remove(final long key) {
        final LongObjectCache<V> chunk = getChunk(key, false);
        if (chunk != null) {
            chunk.remove(key);
        }
    }

    @Nullable
    private LongObjectCache<V> getChunk(final long key, final boolean create) {
        final int chunkIndex = (int) ((key & 0x7fffffffffffffffL) % chunks.length);
        final SoftReference<LongObjectCache<V>> ref = chunks[chunkIndex];
        LongObjectCache<V> result = ref == null ? null : ref.get();
        if (result == null && create) {
            result = new LongObjectCache<V>(chuckSize);
            chunks[chunkIndex] = new SoftReference<LongObjectCache<V>>(result);
        }
        return result;
    }
}
