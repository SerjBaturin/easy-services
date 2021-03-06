package org.zenframework.easyservices.serialize;

import org.zenframework.easyservices.descriptor.MethodDescriptor;

public interface SerializerFactory {

    boolean isTextBased();

    boolean isTypeSafe();

    String getFormat();

    Serializer getSerializer(Class<?>[] paramTypes, Class<?> returnType, MethodDescriptor methodDescriptor, boolean outParametersMode);

    Serializer getTypeSafeSerializer();

}
