//package com.codeabovelab.dm.platform.util;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.codeabovelab.dm.common.utils.ArrayUtils;
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.data.redis.serializer.SerializationException;
//import org.springframework.util.Assert;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Serializer which allow us to (de)serialize any java type to redis without specify type.
// */
//public class RedisJacksonSerializer implements RedisSerializer<Object> {
//
//    private static final byte[] EMPTY_ARRAY = new byte[0];
//    private final ObjectMapper objectMapper;
//    private final TypeMapper typeMapper;
//
//    public RedisJacksonSerializer(ObjectMapper objectMapper, TypeMapper mapper) {
//        this.objectMapper = objectMapper;
//        this.typeMapper = mapper;
//    }
//
//    @Override
//    public byte[] serialize(Object o) throws SerializationException {
//        if (o == null) {
//            return EMPTY_ARRAY;
//        }
//        try {
//            Class<?> clazz = o.getClass();
//            byte[] type = typeMapper.serialize(clazz);
//            if(type.length > 0xFF) {
//                throw new RuntimeException("Too long serialized type for " + clazz.getName() + " from " + typeMapper);
//            }
//            byte[] value = this.objectMapper.writeValueAsBytes(o);
//            byte[] data = new byte[type.length + value.length + 1];
//            data[0] = (byte) type.length;
//            System.arraycopy(type, 0, data, 1, type.length);
//            System.arraycopy(value, 0, data, type.length + 1, value.length);
//            return data;
//        } catch (Exception ex) {
//            throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
//        }
//    }
//
//    @Override
//    public Object deserialize(byte[] bytes) throws SerializationException {
//        if (ArrayUtils.isEmpty(bytes)) {
//            return null;
//        }
//        try {
//            int typeLen = (int)bytes[0];
//            int valueStart = typeLen + 1;
//            Class<?> type = this.typeMapper.deserialize(Arrays.copyOfRange(bytes, 1, valueStart));
//            return this.objectMapper.readValue(bytes, valueStart, bytes.length - valueStart, type);
//        } catch (Exception ex) {
//            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
//        }
//    }
//
//    /**
//     * Bidirectional mapper between javatype and its binary identifier
//     */
//    public interface TypeMapper {
//        byte[] serialize(Class<?> type) throws Exception;
//        Class<?> deserialize(byte[] type) throws Exception;
//    }
//
//
//    /**
//     * Type mapper which save className as string.
//     */
//    public static class TypeAsStringMapper implements TypeMapper {
//
//        private final ClassLoader classLoader;
//
//        public TypeAsStringMapper(ClassLoader classLoader) {
//            this.classLoader = classLoader;
//        }
//
//        public TypeAsStringMapper() {
//            this(null);
//        }
//
//        @Override
//        public byte[] serialize(Class<?> type) {
//            return type.getName().getBytes(StandardCharsets.ISO_8859_1);
//        }
//
//        @Override
//        public Class<?> deserialize(byte[] type) throws Exception {
//            final String name = new String(type, StandardCharsets.ISO_8859_1);
//            Assert.hasText(name, "null or empty class name");
//            if(classLoader != null) {
//                return classLoader.loadClass(name);
//            }
//            return Class.forName(name);
//        }
//    }
//
//    /**
//     * Type mapper which use sequence class index.
//     */
//    public static class TypeAsIndexMapper implements TypeMapper {
//
//        private final Map<Class<?>, Integer> indexByType;
//        private final Class<?> typeByIndex[];
//
//        public TypeAsIndexMapper(Class<?> ... types) {
//            Assert.notEmpty(types, "types is empty or null");
//            if(types.length > 0xFF) {
//                throw new IllegalArgumentException("We do not support more than 265 types.");
//            }
//            this.indexByType = new HashMap<>();
//            this.typeByIndex = types.clone();
//            for(int i = 0; i < types.length; ++i) {
//                this.indexByType.put(types[i], i);
//            }
//        }
//
//        @Override
//        public byte[] serialize(Class<?> type) {
//            Integer index = this.indexByType.get(type);
//            if(index == null) {
//                throw new RuntimeException("Unsupported type: " + type);
//            }
//            return new byte[]{index.byteValue()};
//        }
//
//        @Override
//        public Class<?> deserialize(byte[] type) throws Exception {
//            int index = type[0] & 0xff;
//            if(index < 0 || index > typeByIndex.length) {
//                throw new RuntimeException("Invalid type index: " + index + ", expect from 0 to " + typeByIndex.length);
//            }
//            return typeByIndex[index];
//        }
//    }
//}
