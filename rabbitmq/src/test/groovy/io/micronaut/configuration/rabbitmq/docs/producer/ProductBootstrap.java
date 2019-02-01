package io.micronaut.configuration.rabbitmq.docs.producer;

// tag::imports[]
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
// end::imports[]

// tag::clazz[]
@Singleton
public class ProductBootstrap {

    @Inject protected ProductClient productClient;

    Completable sendProducts() {
        List<Completable> products = new ArrayList<>();
        products.add(productClient.send("product 1".getBytes()));
        products.add(productClient.send("product 2".getBytes()));
        products.add(productClient.send("product 3".getBytes()));

        return Completable.merge(products);
    }
}
// end::clazz[]