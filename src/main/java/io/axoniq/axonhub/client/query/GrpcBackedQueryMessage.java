/*
 * Copyright (c) 2018. AxonIQ
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.axoniq.axonhub.client.query;

import io.axoniq.axonhub.QueryRequest;
import io.axoniq.platform.MetaDataValue;
import io.axoniq.platform.SerializedObject;
import org.axonframework.messaging.MetaData;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.responsetypes.ResponseType;
import org.axonframework.serialization.LazyDeserializingObject;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around GRPC QueryRequest to implement the QueryMessage interface
 *
 * @author Marc Gathier
 */
public class GrpcBackedQueryMessage<Q, R> implements QueryMessage<Q, R> {

    private final QueryRequest query;
    private final Serializer messageSerializer;
    private LazyDeserializingObject<Q> serializedPayload;
    private LazyDeserializingObject<ResponseType<R>> serializedResponseType;
    private MetaData metaData;

    public GrpcBackedQueryMessage(QueryRequest query, Serializer messageSerializer, Serializer genericSerializer) {
        this.query = query;
        this.messageSerializer = messageSerializer;
        this.serializedPayload = new LazyDeserializingObject<>(fromGrpcSerializedObject(query.getPayload()), messageSerializer);
        this.serializedResponseType = new LazyDeserializingObject<>(fromGrpcSerializedObject(query.getResponseType()), genericSerializer);
    }

    private org.axonframework.serialization.SerializedObject<byte[]> fromGrpcSerializedObject(SerializedObject payload) {
        return new SimpleSerializedObject<>(payload.getData().toByteArray(),
                                            byte[].class,
                                            payload.getType(),
                                            payload.getRevision());
    }

    @Override
    public String getQueryName() {
        return query.getQuery();
    }

    @Override
    public ResponseType<R> getResponseType() {
        return serializedResponseType.getObject();
    }

    @Override
    public String getIdentifier() {
        return query.getMessageIdentifier();
    }

    @Override
    public MetaData getMetaData() {
        if (metaData == null) {
            metaData = deserializeMetaData(query.getMetaDataMap());
        }
        return metaData;
    }

    @Override
    public Q getPayload() {
        return serializedPayload.getObject();
    }

    @Override
    public Class<Q> getPayloadType() {
        return serializedPayload.getType();
    }

    @Override
    public QueryMessage<Q, R> withMetaData(Map<String, ?> var1) {
        return null;
    }

    @Override
    public QueryMessage<Q, R> andMetaData(Map<String, ?> var1) {
        return null;
    }

    private MetaData deserializeMetaData(Map<String, MetaDataValue> metaDataMap) {
        if (metaDataMap.isEmpty()) {
            return MetaData.emptyInstance();
        }
        Map<String, Object> metaData = new HashMap<>(metaDataMap.size());
        metaDataMap.forEach((k, v) -> metaData.put(k, convertFromMetaDataValue(v)));
        return MetaData.from(metaData);
    }

    private Object convertFromMetaDataValue(MetaDataValue value) {
        switch (value.getDataCase()) {
            case TEXT_VALUE:
                return value.getTextValue();
            case BYTES_VALUE:
                return messageSerializer.deserialize(fromGrpcSerializedObject(value.getBytesValue()));
            case DOUBLE_VALUE:
                return value.getDoubleValue();
            case NUMBER_VALUE:
                return value.getNumberValue();
            case BOOLEAN_VALUE:
                return value.getBooleanValue();
            case DATA_NOT_SET:
                return null;
        }
        return null;
    }

}
