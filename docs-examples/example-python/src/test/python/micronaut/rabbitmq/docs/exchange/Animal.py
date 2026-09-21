from dataclasses import dataclass


@dataclass
class Animal:
    """The base type of the animals published to the ``animals`` exchange."""

    name: str
