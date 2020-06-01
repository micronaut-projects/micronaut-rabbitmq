/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.rabbitmq.connect;

import com.rabbitmq.client.Address;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Converts a CharSequence to an {@link Address}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class AddressTypeConverter implements TypeConverter<CharSequence, Address> {

    @Override
    public Optional<Address> convert(CharSequence object, Class<Address> targetType, ConversionContext context) {
        String address = object.toString();
        return Optional.of(Address.parseAddress(address));
    }
}
