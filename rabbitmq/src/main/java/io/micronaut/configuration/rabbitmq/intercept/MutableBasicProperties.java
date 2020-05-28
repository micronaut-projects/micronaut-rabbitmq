/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.configuration.rabbitmq.intercept;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;

import java.util.Date;
import java.util.Map;

/**
 * A class that stores the basic properties in a mutable state.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class MutableBasicProperties implements BasicProperties {

    private String contentType;
    private String contentEncoding;
    private Map<String, Object> headers;
    private Integer deliveryMode;
    private Integer priority;
    private String correlationId;
    private String replyTo;
    private String expiration;
    private String messageId;
    private Date timestamp;
    private String type;
    private String userId;
    private String appId;
    private String clusterId;

    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType The content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * Sets the content encoding.
     *
     * @param contentEncoding The content encoding
     */
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Sets the headers.
     *
     * @param headers The headers
     */
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    @Override
    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * Sets the delivery mode.
     *
     * @param deliveryMode The delivery mode
     */
    public void setDeliveryMode(Integer deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    /**
     * Sets the priority.
     *
     * @param priority The priority
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Sets the correlation id.
     *
     * @param correlationId The correlation id
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the reply to.
     *
     * @param replyTo The reply to
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public String getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration.
     *
     * @param expiration The expiration
     */
    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the message id.
     *
     * @param messageId The message id
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type The type
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user id.
     *
     * @param userId The user id
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    /**
     * Sets the app id.
     *
     * @param appId The app id
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return The cluster id
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * Sets the cluster id.
     *
     * @param clusterId The cluster id
     */
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * Converts the mutable properties to the RabbitMQ properties.
     *
     * @return The RabbitMQ properties
     */
    public AMQP.BasicProperties toBasicProperties() {
        return new AMQP.BasicProperties.Builder()
                .contentType(contentType)
                .contentEncoding(contentEncoding)
                .headers(headers)
                .deliveryMode(deliveryMode)
                .priority(priority)
                .correlationId(correlationId)
                .replyTo(replyTo)
                .expiration(expiration)
                .messageId(messageId)
                .timestamp(timestamp)
                .type(type)
                .userId(userId)
                .appId(appId)
                .clusterId(clusterId)
                .build();
    }
}
