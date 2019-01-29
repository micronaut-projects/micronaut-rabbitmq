package io.micronaut.configuration.rabbitmq.serialization;

import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.serialize.exceptions.SerializationException;

import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

@Singleton
public class JavaLangRabbitMessageSerDes<T> implements RabbitMessageSerDes<T> {

    @Override
    public byte[] serialize(T data) {
        return findSerDes(data.getClass()).serialize(data);
    }

    @Override
    public T deserialize(byte[] data, Class<T> type) {
        return (T) findSerDes(type).deserialize(data, type);
    }

    @Override
    public boolean supports(Class<T> type) {
        return findSerDes(type) != null;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    protected RabbitMessageSerDes findSerDes(Class type) {
        if (String.class.isAssignableFrom(type)) {
            return getStringSerDes();
        }

        if (Short.class.isAssignableFrom(type)) {
            return getShortSerDes();
        }

        if (Integer.class.isAssignableFrom(type)) {
            return getIntegerSerDes();
        }

        if (Long.class.isAssignableFrom(type)) {
            return getLongSerDes();
        }

        if (Float.class.isAssignableFrom(type)) {
            return getFloatSerDes();
        }

        if (Double.class.isAssignableFrom(type)) {
            return getDoubleSerDes();
        }

        if (byte[].class.isAssignableFrom(type)) {
            return getByteArraySerDes();
        }

        if (ByteBuffer.class.isAssignableFrom(type)) {
            return getByteBufferSerDes();
        }

        if (UUID.class.isAssignableFrom(type)) {
            return getUUIDSerDes();
        }

        return null;
    }

    protected RabbitMessageSerDes<String> getStringSerDes() {
        return new StringSerDes();
    }

    static class StringSerDes implements RabbitMessageSerDes<String> {

        private static final Charset encoding = Charset.forName("UTF8");

        @Override
        public String deserialize(byte[] data, Class<String> type) {
            if (data == null) {
                return null;
            } else {
                return new String(data, encoding);
            }
        }

        @Override
        public byte[] serialize(String data) {
            if (data == null) {
                return null;
            } else {
                return data.getBytes(encoding);
            }
        }

        @Override
        public boolean supports(Class<String> type) {
            return type == String.class;
        }
    }

    protected RabbitMessageSerDes<Short> getShortSerDes() {
        return new ShortSerDes();
    }

    static class ShortSerDes implements RabbitMessageSerDes<Short> {

        @Override
        public Short deserialize(byte[] data, Class<Short> type) {
            if (data == null) {
                return null;
            } else if (data.length != 2) {
                throw new SerializationException("Incorrect message body size to deserialize to a Short");
            } else {
                short value = 0;
                for (byte b : data) {
                    value <<= 8;
                    value |= b & 0xFF;
                }
                return value;
            }
        }

        @Override
        public byte[] serialize(Short data) {
            if (data == null) {
                return null;
            } else {
                return new byte[] {
                        (byte) (data >>> 8),
                        data.byteValue()
                };
            }
        }

        @Override
        public boolean supports(Class<Short> type) {
            return type == Short.class;
        }
    }

    protected RabbitMessageSerDes<Integer> getIntegerSerDes() {
        return new IntegerSerDes();
    }

    static class IntegerSerDes implements RabbitMessageSerDes<Integer> {

        @Override
        public Integer deserialize(byte[] data, Class<Integer> type) {
            if (data == null) {
                return null;
            } else if (data.length != 4) {
                throw new SerializationException("Incorrect message body size to deserialize to an Integer");
            } else {
                int value = 0;
                for (byte b : data) {
                    value <<= 8;
                    value |= b & 0xFF;
                }
                return value;
            }
        }

        @Override
        public byte[] serialize(Integer data) {
            if (data == null) {
                return null;
            } else {
                return new byte[] {
                        (byte) (data >>> 24),
                        (byte) (data >>> 16),
                        (byte) (data >>> 8),
                        data.byteValue()
                };
            }
        }

        @Override
        public boolean supports(Class<Integer> type) {
            return type == Integer.class;
        }
    }

    protected RabbitMessageSerDes<Long> getLongSerDes() {
        return new LongSerDes();
    }

    static class LongSerDes implements RabbitMessageSerDes<Long> {

        @Override
        public Long deserialize(byte[] data, Class<Long> type) {
            if (data == null) {
                return null;
            } else if (data.length != 8) {
                throw new SerializationException("Incorrect message body size to deserialize to a Long");
            } else {
                long value = 0;
                for (byte b : data) {
                    value <<= 8;
                    value |= b & 0xFF;
                }
                return value;
            }
        }

        @Override
        public byte[] serialize(Long data) {
            if (data == null) {
                return null;
            } else {
                return new byte[]{
                        (byte) (data >>> 56),
                        (byte) (data >>> 48),
                        (byte) (data >>> 40),
                        (byte) (data >>> 32),
                        (byte) (data >>> 24),
                        (byte) (data >>> 16),
                        (byte) (data >>> 8),
                        data.byteValue()
                };
            }
        }

        @Override
        public boolean supports(Class<Long> type) {
            return type == Long.class;
        }
    }

    protected RabbitMessageSerDes<Float> getFloatSerDes() {
        return new FloatSerDes();
    }

    static class FloatSerDes implements RabbitMessageSerDes<Float> {

        @Override
        public Float deserialize(byte[] data, Class<Float> type) {
            if (data == null) {
                return null;
            } else if (data.length != 4) {
                throw new SerializationException("Incorrect message body size to deserialize to a Float");
            } else {
                int value = 0;
                for (byte b : data) {
                    value <<= 8;
                    value |= b & 0xFF;
                }
                return Float.intBitsToFloat(value);
            }
        }

        @Override
        public byte[] serialize(Float data) {
            if (data == null) {
                return null;
            } else {
                long bits = Float.floatToRawIntBits(data);
                return new byte[] {
                        (byte) (bits >>> 24),
                        (byte) (bits >>> 16),
                        (byte) (bits >>> 8),
                        (byte) bits
                };
            }
        }

        @Override
        public boolean supports(Class<Float> type) {
            return type == Float.class;
        }
    }

    protected RabbitMessageSerDes<Double> getDoubleSerDes() {
        return new DoubleSerDes();
    }

    static class DoubleSerDes implements RabbitMessageSerDes<Double> {

        @Override
        public Double deserialize(byte[] data, Class<Double> type) {
            if (data == null) {
                return null;
            } else if (data.length != 8) {
                throw new SerializationException("Incorrect message body size to deserialize to a Double");
            } else {
                long value = 0;
                for (byte b : data) {
                    value <<= 8;
                    value |= b & 0xFF;
                }
                return Double.longBitsToDouble(value);
            }
        }

        @Override
        public byte[] serialize(Double data) {
            if (data == null) {
                return null;
            } else {
                long bits = Double.doubleToLongBits(data);
                return new byte[] {
                        (byte) (bits >>> 56),
                        (byte) (bits >>> 48),
                        (byte) (bits >>> 40),
                        (byte) (bits >>> 32),
                        (byte) (bits >>> 24),
                        (byte) (bits >>> 16),
                        (byte) (bits >>> 8),
                        (byte) bits
                };
            }
        }

        @Override
        public boolean supports(Class<Double> type) {
            return type == Double.class;
        }
    }

    protected RabbitMessageSerDes<byte[]> getByteArraySerDes() {
        return new ByteArraySerDes();
    }

    static class ByteArraySerDes implements RabbitMessageSerDes<byte[]> {

        @Override
        public byte[] deserialize(byte[] data, Class<byte[]> type) {
            return data;
        }

        @Override
        public byte[] serialize(byte[] data) {
            return data;
        }

        @Override
        public boolean supports(Class<byte[]> type) {
            return type == byte[].class;
        }
    }

    protected RabbitMessageSerDes<ByteBuffer> getByteBufferSerDes() {
        return new ByteBufferSerDes();
    }

    static class ByteBufferSerDes implements RabbitMessageSerDes<ByteBuffer> {

        @Override
        public ByteBuffer deserialize(byte[] data, Class<ByteBuffer> type) {
            if (data == null) {
                return null;
            } else {
                return ByteBuffer.wrap(data);
            }
        }

        @Override
        public byte[] serialize(ByteBuffer data) {
            if (data == null) {
                return null;
            } else {
                data.rewind();

                if (data.hasArray()) {
                    byte[] arr = data.array();
                    if (data.arrayOffset() == 0 && arr.length == data.remaining()) {
                        return arr;
                    }
                }

                byte[] ret = new byte[data.remaining()];
                data.get(ret, 0, ret.length);
                data.rewind();
                return ret;
            }
        }

        @Override
        public boolean supports(Class<ByteBuffer> type) {
            return type == ByteBuffer.class;
        }
    }

    protected RabbitMessageSerDes<UUID> getUUIDSerDes() {
        return new UUIDSerDes();
    }

    static class UUIDSerDes implements RabbitMessageSerDes<UUID> {

        StringSerDes stringSerDes = new StringSerDes();

        @Override
        public UUID deserialize(byte[] data, Class<UUID> type) {
            String uuid = stringSerDes.deserialize(data, String.class);
            if (uuid == null) {
                return null;
            } else {
                return UUID.fromString(uuid);
            }
        }

        @Override
        public byte[] serialize(UUID data) {
            if (data == null) {
                return null;
            } else {
                return stringSerDes.serialize(data.toString());
            }
        }

        @Override
        public boolean supports(Class<UUID> type) {
            return type == UUID.class;
        }
    }
}
