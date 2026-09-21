# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `docs-examples/example-python`
that are present but disabled, or that deviate from the Java example because the direct port does
not compile or does not behave like the Java example yet. It is the bug-fixing task list for
The Python compiler (`micronaut-inject-python` / `micronaut-context-python`); every row references a
`TODO(python)` comment in the sources or a workaround described below.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow), which needs a container
runtime for the RabbitMQ test container.

## Reconciliation

- Last generated active `@Disabled` count: 0.
- Last generated command: `rg -n "@Disabled\(" docs-examples/example-python/src/test/python`.
- Last full-suite command: `./gradlew :micronaut-docs-examples:micronaut-example-python:test -Ppython-ci`.
- Last full-suite result (micronaut-core 5.2.3, micronaut-build 8.1.2): build successful, 15 tests executed, 0 skipped, 0 failures.

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  Standard Micronaut and RabbitMQ annotations are imported from their Java package
  (`micronaut.rabbitmq.annotation`, `micronaut.messaging.annotation`, `micronaut.core.bind.annotation`, ...).
- `@RabbitClient` interfaces are abstract classes (`ABC`) whose abstract methods have `...` bodies;
  `@RabbitListener` beans are plain classes with `@Queue` methods. Parameter annotations use
  `Annotated[str, MessageHeader("...")]`, `Annotated[Long, DeliveryTag]`, etc.
- Python has no method overloading: the overloaded `send` methods of the Java clients and the
  overloaded `receive` methods of the Java listeners get distinct names (`send_with_headers`,
  `send_with_header_map`, `send_to`, `send_mandatory`, `send_with_properties`, `receive_headers`,
  `receive_cat`/`receive_snake`, `send_animal`).
- Parameters bound by name (`appId`, `contentType`) keep the Java (camelCase) name because the
  `BasicProperties` name is inferred from the argument name; other names are snake_case.
- Methods that implement a Java interface keep the Java (camelCase) name (`onCreated`,
  `onApplicationEvent`, `initialize`, `handleReturn`, `getAnnotationType`, `bind`, `argumentType`,
  `deserialize`, `serialize`, `supports`).
- Do not add Java-style getters or setters to Python docs models. Prefer `@dataclass` models with
  idiomatic Python attributes (`ProductInfo`, `Cat`, `Snake`).
- Python `int` is a Java `int`; the 64-bit `count` attributes and header/delivery tag arguments are
  declared as `java.lang.Long`. Message bodies are `bytes` (`byte[]`); a received body is converted
  with `bytes(data).decode()`.
- A Python test class is a `@MicronautTest(environments=["rabbitmq"])` with injected `@RabbitClient`
  and listener beans (`product_client: Annotated[ProductClient, Inject]`). The connection properties of
  the shared RabbitMQ test container are supplied by the Java
  `io.micronaut.rabbitmq.docs.RabbitMQTestConfigurer` (`@ContextConfigurer`) of this project
  (`src/test/java`), see below. Conditions are awaited with the `micronaut.rabbitmq.docs.Await` helper
  instead of Awaitility.
- Java classes are imported (`from java.lang import Long`, `from reactor.core.publisher import Mono`,
  `from micronaut.core.bind.ArgumentBinder import BindingResult`), logging uses the Python `logging`
  module (`LOG = logging.getLogger(__name__)`); `java.type(...)` is only used where a Python-defined
  annotation must be passed to Java as a runtime `java.lang.Class` (see "java.type usages" below).
- The `ChannelInitializer` subclasses of the Java examples (`ChannelPoolListener`, `MyReturnListener`)
  implement the `ChannelPoolInitializer` interface (see "Workarounds Kept In Snippets").

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Workarounds Kept In Snippets

| Target | Reason |
| --- | --- |
| `io.micronaut.rabbitmq.docs.RabbitMQTestConfigurer` (Java, `src/test/java`) | `TestPropertyProvider.getProperties()` is called by Micronaut Test before the application context, and with it the GraalPy runtime, exists, so a Python test class cannot provide the container's connection properties; the `@ContextConfigurer` adding the `rabbitmq.uri`/`username`/`password` property source (and the `rabbitmq.servers.product-cluster.*` properties of the `ConnectionSpec` in the `rabbitmq-cluster` environment) in `configure(ApplicationContext)` is written in Java. |
| `io.micronaut.rabbitmq.docs.ChannelPoolListener`, `io.micronaut.rabbitmq.docs.parameters.MyReturnListener` | Extending the Java class `ChannelInitializer` (core 5.2.3 supports Java class bases) fails to compile because the overridden `initialize(Channel, String)` declares `throws IOException` and the generated base-method dispatcher does not handle it: `Pyronaut processing failed: unreported exception java.io.IOException; must be caught or declared to be thrown` at `super.initialize(...)` in the generated `micronautInvokeJavaBaseMethod` of `ChannelPoolListener.java`. The Python classes implement the `ChannelPoolInitializer` interface instead. `TODO(python)`. |
| `io.micronaut.rabbitmq.docs.publisher.acknowledge.PublisherAcknowledgeSpec` | A class defined inside a method cannot extend an imported Java interface (`Subscriber`) with core 5.2.3: instantiating it fails with `TypeError: invalid instantiation of foreign object` (the runtime module keeps the host interface as the base; with the generated import modules of 5.2.2 the local class worked). The subscriber of the Java example's anonymous class is a module-level class taking the counters. `TODO(python)`. |
| `io.micronaut.rabbitmq.docs.consumer.custom.annotation.DeliveryTagAnnotationBinder`, `io.micronaut.rabbitmq.docs.consumer.custom.type.ProductInfoTypeBinder` | `bind` declares its `BindingResult[...]` return type explicitly (like the Java `@Override`) so the returned lambda is converted to the functional interface. |
| `io.micronaut.rabbitmq.docs.parameters.MandatoryProductClient`, `MyReturnListener`, `io.micronaut.rabbitmq.docs.event.MyStartingEventListener`, `MyStartedEventListener` | Like in the Java, Kotlin and Groovy examples these snippets are compiled but not exercised by a test (`spec.name` `MandatorySpec` / `RabbitListenerEventsSpec` have no test class). |

## Intentionally Unsupported Snippet Targets

None.

## java.type usages

Every remaining `java.type(...)` call carries a `# TODO(python)` comment naming the reason.

| Location | Reason |
| --- | --- |
| `consumer/custom/annotation/DeliveryTagAnnotationBinder.py` (`DeliveryTagClass`) | `RabbitAnnotatedArgumentBinder.getAnnotationType()` returns the annotation type to Java as a runtime `java.lang.Class`; returning the Python-defined annotation function still fails with core 5.2.3: `Cannot convert '<function DeliveryTag at 0x...>'(language: Python, type: function) to Java type 'java.lang.Class': Unsupported target type.` (raised while `RabbitMQConsumerAdvice` is instantiated). Python *classes* passed as `Class` arguments work. |

## Verified with micronaut-core 5.2.3 (workarounds removed)

- `PythonRuntimeInitializer` (Java) removed: the GraalPy runtime is created on demand for the Python beans the
  `processOnStartup` consumer advice instantiates.
- `RabbitAnnotatedArgumentBinder[DeliveryTag]` generic binder generates `BindingResult<Object> bind(...)`.
- `ProductInfoSerDes.serialize(...) -> bytes | None`.
- `Animal` is a `@dataclass` base, `Cat(Animal)`/`Snake(Animal)` only declare their own fields, and
  `AnimalClient.send(..., animal: Animal)` keeps the runtime type of the passed `Cat`/`Snake` (JSON serialization
  uses the subclass introspection).
- The deserialized `Cat`/`Snake` bodies reach the Python listener as Python objects (`isinstance(cat, Cat)` in
  `CustomExchangeSpec`, no `java.type` needed).
