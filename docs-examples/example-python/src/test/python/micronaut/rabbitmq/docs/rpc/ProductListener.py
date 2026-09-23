from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="RpcUppercaseSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    @Queue("product")
    def to_upper_case(self, data: str) -> str:  # <1>
        return data.upper()  # <2>
# end::clazz[]
