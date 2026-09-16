# tag::imports[]
from micronaut.core.bind.annotation import Bindable
# end::imports[]


# tag::clazz[]
@Bindable  # <1>
def DeliveryTag(target):
    return target
# end::clazz[]
