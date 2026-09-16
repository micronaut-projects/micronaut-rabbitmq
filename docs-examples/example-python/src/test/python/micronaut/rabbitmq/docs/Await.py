import time
from collections.abc import Callable


class Await:
    """Polls a condition until it holds, like Awaitility's ``await().atMost(...).until(...)`` in the Java examples."""

    @staticmethod
    def until(condition: Callable[[], bool], timeout: float = 60.0) -> None:
        deadline = time.monotonic() + timeout
        while not condition():
            assert time.monotonic() < deadline, "The condition was not fulfilled within the timeout"
            time.sleep(0.1)
