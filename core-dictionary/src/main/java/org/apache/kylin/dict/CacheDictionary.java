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

package org.apache.kylin.dict;

import org.apache.kylin.common.util.Dictionary;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xiefan on 16-12-30.
 */
abstract public class CacheDictionary<T> extends Dictionary<T> {
    private static final long serialVersionUID = 1L;

    transient protected boolean enableValueCache = false;

    transient private SoftReference<Map> valueToIdCache;

    transient private SoftReference<Object[]> idToValueCache;

    transient protected int baseId;

    transient protected BytesConverter<T> bytesConvert;

    public CacheDictionary() {

    }

    //value --> id
    @Override
    final protected int getIdFromValueImpl(T value, int roundingFlag) {
        if (enableValueCache && roundingFlag == 0) {
            Map cache = valueToIdCache.get(); // SoftReference to skip cache gracefully when short of memory
            if (cache != null) {
                Integer id = null;
                id = (Integer) cache.get(value);
                if (id != null)
                    return id.intValue();
                byte[] valueBytes = bytesConvert.convertToBytes(value);
                id = getIdFromValueBytes(valueBytes, 0, valueBytes.length, roundingFlag);
                cache.put(value, id);
                return id;
            }
        }
        byte[] valueBytes = bytesConvert.convertToBytes(value);
        return getIdFromValueBytes(valueBytes, 0, valueBytes.length, roundingFlag);
    }

    //id --> value
    @Override
    final protected T getValueFromIdImpl(int id) {
        if (enableValueCache) {
            Object[] cache = idToValueCache.get();
            if (cache != null) {
                int seq = calcSeqNoFromId(id);
                if (cache[seq] != null)
                    return (T) cache[seq];
                byte[] valueBytes = getValueBytesFromIdImpl(id);
                T value = bytesConvert.convertFromBytes(valueBytes, 0, valueBytes.length);
                cache[seq] = value;
                return value;
            }
        }
        byte[] valueBytes = getValueBytesFromIdImpl(id);
        return bytesConvert.convertFromBytes(valueBytes, 0, valueBytes.length);
    }

    final protected int calcSeqNoFromId(int id) {
        int seq = id - baseId;
        if (seq < 0 || seq >= getSize()) {
            throw new IllegalArgumentException("Not a valid ID: " + id);
        }
        return seq;
    }

    final public void enableCache() {
        this.enableValueCache = true;
        if (this.valueToIdCache == null)
            this.valueToIdCache = new SoftReference<Map>(new ConcurrentHashMap());
        if (this.idToValueCache == null)
            this.idToValueCache = new SoftReference<Object[]>(new Object[getSize()]);
    }

    final public void disableCache() {
        this.enableValueCache = false;
        this.valueToIdCache = null;
        this.idToValueCache = null;
    }
}
