from dataclasses import dataclass

from micronaut.serde.annotation import Serdeable

from .Animal import Animal


@Serdeable
@dataclass
class Snake(Animal):
    venomous: bool
