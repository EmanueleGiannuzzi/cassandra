/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db.rows;

import java.nio.ByteBuffer;

import org.apache.cassandra.db.ExpirationDateOverflowHandling;
import org.apache.cassandra.db.marshal.ByteBufferAccessor;
import org.apache.cassandra.db.marshal.ValueAccessor;
import org.apache.cassandra.schema.ColumnMetadata;
import org.apache.cassandra.db.marshal.ByteType;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.ObjectSizes;

import static java.lang.String.format;

public class BufferCell extends AbstractCell<ByteBuffer>
{
    private static final long EMPTY_SIZE = ObjectSizes.measure(new BufferCell(ColumnMetadata.regularColumn("", "", "", ByteType.instance), 0L, 0, 0, ByteBufferUtil.EMPTY_BYTE_BUFFER, null));

    private final long timestamp;
    private final int ttl;
    private final int localDeletionTime;

    private final ByteBuffer value;
    private final CellPath path;

    public BufferCell(ColumnMetadata column, long timestamp, int ttl, int localDeletionTime, ByteBuffer value, CellPath path)
    {
        super(column);
        assert !column.isPrimaryKeyColumn();
        assert column.isComplex() == (path != null) : format("Column %s.%s(%s: %s) isComplex: %b with cellpath: %s", column.ksName, column.cfName, column.name, column.type.toString(), column.isComplex(), path);
        this.timestamp = timestamp;
        this.ttl = ttl;
        this.localDeletionTime = localDeletionTime;
        this.value = value;
        this.path = path;
    }

    public static BufferCell live(ColumnMetadata column, long timestamp, ByteBuffer value)
    {
        return live(column, timestamp, value, null);
    }

    public static BufferCell live(ColumnMetadata column, long timestamp, ByteBuffer value, CellPath path)
    {
        return new BufferCell(column, timestamp, NO_TTL, NO_DELETION_TIME, value, path);
    }

    public static BufferCell expiring(ColumnMetadata column, long timestamp, int ttl, int nowInSec, ByteBuffer value)
    {
        return expiring(column, timestamp, ttl, nowInSec, value, null);
    }

    public static BufferCell expiring(ColumnMetadata column, long timestamp, int ttl, int nowInSec, ByteBuffer value, CellPath path)
    {
        assert ttl != NO_TTL;
        return new BufferCell(column, timestamp, ttl, ExpirationDateOverflowHandling.computeLocalExpirationTime(nowInSec, ttl), value, path);
    }

    public static BufferCell tombstone(ColumnMetadata column, long timestamp, int nowInSec)
    {
        return tombstone(column, timestamp, nowInSec, null);
    }

    public static BufferCell tombstone(ColumnMetadata column, long timestamp, int nowInSec, CellPath path)
    {
        return new BufferCell(column, timestamp, NO_TTL, nowInSec, ByteBufferUtil.EMPTY_BYTE_BUFFER, path);
    }

    public long timestamp()
    {
        return timestamp;
    }

    public int ttl()
    {
        return ttl;
    }

    public int localDeletionTime()
    {
        return localDeletionTime;
    }

    public ByteBuffer value()
    {
        return value;
    }

    public ValueAccessor<ByteBuffer> accessor()
    {
        return ByteBufferAccessor.instance;
    }

    public CellPath path()
    {
        return path;
    }

    public Cell<?> withUpdatedColumn(ColumnMetadata newColumn)
    {
        return new BufferCell(newColumn, timestamp, ttl, localDeletionTime, value, path);
    }

    public Cell<?> withUpdatedValue(ByteBuffer newValue)
    {
        return new BufferCell(column, timestamp, ttl, localDeletionTime, newValue, path);
    }

    public Cell<?> withUpdatedTimestampAndLocalDeletionTime(long newTimestamp, int newLocalDeletionTime)
    {
        return new BufferCell(column, newTimestamp, ttl, newLocalDeletionTime, value, path);
    }

    public Cell<?> withSkippedValue()
    {
        return withUpdatedValue(ByteBufferUtil.EMPTY_BYTE_BUFFER);
    }

    @Override
    public long unsharedHeapSize()
    {
        return EMPTY_SIZE + ObjectSizes.sizeOnHeapOf(value) + (path == null ? 0 : path.unsharedHeapSize());
    }

    @Override
    public long unsharedHeapSizeExcludingData()
    {
        return EMPTY_SIZE + ObjectSizes.sizeOnHeapExcludingData(value) + (path == null ? 0 : path.unsharedHeapSizeExcludingData());
    }
}
