# tag::clazz[]
from com.rabbitmq.client import Channel
from jakarta.inject import Singleton
from micronaut.rabbitmq.connect import ChannelPoolInitializer


@Singleton  # <1>
class ChannelPoolListener(ChannelPoolInitializer):  # <2>

    def initialize(self, channel: Channel, name: str) -> None:  # <3>
        # docs/quickstart
        args = {"x-max-priority": 100}
        channel.queueDeclare("product", False, False, False, args)  # <4>

        # docs/exchange
        channel.exchangeDeclare("animals", "headers", False)
        channel.queueDeclare("snakes", False, False, False, None)
        channel.queueDeclare("cats", False, False, False, None)
        cat_args = {"x-match": "all", "animalType": "Cat"}
        channel.queueBind("cats", "animals", "", cat_args)

        snake_args = {"x-match": "all", "animalType": "Snake"}
        channel.queueBind("snakes", "animals", "", snake_args)
# end::clazz[]
