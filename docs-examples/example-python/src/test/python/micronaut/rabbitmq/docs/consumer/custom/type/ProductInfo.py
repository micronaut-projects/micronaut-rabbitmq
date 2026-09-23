# tag::clazz[]
from dataclasses import dataclass

from java.lang import Long


@dataclass
class ProductInfo:
    size: str | None  # <1>
    count: Long  # <2>
    sealed: bool  # <3>
# end::clazz[]
