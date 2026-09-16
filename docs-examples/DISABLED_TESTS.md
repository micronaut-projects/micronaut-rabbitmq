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
- Last full-suite result: build successful, 15 tests executed, 0 skipped, 0 failures.

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
  module (`LOG = logging.getLogger(__name__)`); `java.type(...)` is only used where a Python class
  must be passed to Java as a runtime `java.lang.Class` (see "java.type usages" below).
- A Python class cannot extend a Java class: the `ChannelInitializer` subclasses of the Java examples
  (`ChannelPoolListener`, `MyReturnListener`) implement the `ChannelPoolInitializer` interface.

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Workarounds Kept In Snippets

| Target | Reason |
| --- | --- |
| `io.micronaut.rabbitmq.docs.RabbitMQTestConfigurer` (Java, `src/test/java`) | `TestPropertyProvider.getProperties()` is called by Micronaut Test before the application context, and with it the GraalPy runtime, exists, so a Python test class cannot provide the container's connection properties; the `@ContextConfigurer` adding the `rabbitmq.uri`/`username`/`password` property source (and the `rabbitmq.servers.product-cluster.*` properties of the `ConnectionSpec` in the `rabbitmq-cluster` environment) in `configure(ApplicationContext)` is written in Java. |
| `io.micronaut.rabbitmq.docs.PythonRuntimeInitializer` (Java, `src/test/java`) | `@Executable(processOnStartup = true)` processors such as the RabbitMQ consumer advice are created before the `@Context` beans, so the Python beans they depend on (argument binders, message SerDes, `@RabbitListener` beans) would be instantiated before the GraalPy runtime exists (`GraalPy context has not been initialized`); the initializer is a `TypeConverterRegistrar` injecting the GraalPy context, which is created before those processors. |
| `io.micronaut.rabbitmq.docs.consumer.custom.annotation.DeliveryTagAnnotationBinder` | Declaring the binder as `RabbitAnnotatedArgumentBinder[DeliveryTag]` generates `bind` with the return type `BindingResult<DeliveryTag>` instead of `BindingResult<Object>` (the annotation type argument is used as the value type), which does not compile; the Python class implements the raw `RabbitAnnotatedArgumentBinder`, and `bind` declares `-> BindingResult[object]` explicitly because a Python lambda returned from a method without a return annotation is not converted to the `BindingResult` functional interface (`PolyglotMapAndFunction cannot be cast to ArgumentBinder$BindingResult`). |
| `io.micronaut.rabbitmq.docs.consumer.custom.type.ProductInfoTypeBinder` | Same: `bind` declares `-> BindingResult[ProductInfo]` explicitly so the returned lambda is converted to the functional interface. |
| `io.micronaut.rabbitmq.docs.exchange.AnimalClient`, `Animal`, `Cat`, `Snake` | A Python object passed to Java through a parameter declared with a Python base class (`animal: Animal`) is wrapped by the base class stub and loses its runtime type, so the JSON SerDes finds no introspection for `Animal`; the client method is declared as `animal: Cat \| Snake` (generated as `Object`) so the `Cat`/`Snake` introspections are used. The generated stub of a dataclass extending a Python dataclass only declares the constructor parameters of the subclass (`Cat(lives)` does not compile against `Animal(name)`), so `Animal` is a plain base class and `Cat`/`Snake` declare `name` themselves. |
| `io.micronaut.rabbitmq.docs.exchange.CustomExchangeSpec` | The deserialized `Cat`/`Snake` bodies reach the Python listener as instances of the generated Java classes (foreign objects with public fields) rather than as Python objects, so the test filters them with `java.instanceof(...)` instead of `isinstance(...)`. |
| `io.micronaut.rabbitmq.docs.serdes.ProductInfoSerDes` | A `-> bytes \| None` return annotation generates `pythonResult.asByte()` (a single byte) instead of a `byte[]` conversion; `serialize` is declared `-> bytes` (a `None` result is still passed to Java as `null`). |
| `io.micronaut.rabbitmq.docs.parameters.MandatoryProductClient`, `MyReturnListener`, `io.micronaut.rabbitmq.docs.event.MyStartingEventListener`, `MyStartedEventListener` | Like in the Java, Kotlin and Groovy examples these snippets are compiled but not exercised by a test (`spec.name` `MandatorySpec` / `RabbitListenerEventsSpec` have no test class). |

## Intentionally Unsupported Snippet Targets

None.

## java.type usages

Every remaining `java.type(...)` call carries a `# TODO(python)` comment naming the reason.

| Location | Reason |
| --- | --- |
| `exchange/CustomExchangeSpec.py` (`CatClass`, `SnakeClass`) | The deserialized `Cat`/`Snake` bodies reach the Python listener as instances of the generated Java classes and are filtered with `java.instanceof(...)`, whose type argument must be a Java class; the imported Python classes fail with `instanceof second argument 'type' is not a Java class`. |
| `consumer/custom/annotation/DeliveryTagAnnotationBinder.py` (`DeliveryTagClass`) | `RabbitAnnotatedArgumentBinder.getAnnotationType()` returns the annotation type to Java as a runtime `java.lang.Class`; returning the imported Python annotation function fails with `Cannot convert '<function DeliveryTag>' (language: Python, type: function) to Java type 'java.lang.Class'`. |
