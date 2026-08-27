"""The animal pictures the picture-style worksheets draw.

Mirrors ``shared/src/main/java/com/dividetask/homeschoolteacher/reading/Animal.kt``
so a printed sheet uses the same cast as the Android lesson it backs. If
an animal is added there, add it here too and re-subset the emoji font
(see ``fonts/SOURCE.md``).
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Animal:
    letter: str
    emoji: str
    name: str


ALL = (
    Animal("A", "\U0001F41C", "Ant"),
    Animal("B", "\U0001F43B", "Bear"),
    Animal("C", "\U0001F431", "Cat"),
    Animal("D", "\U0001F436", "Dog"),
    Animal("E", "\U0001F418", "Elephant"),
    Animal("F", "\U0001F98A", "Fox"),
    Animal("G", "\U0001F992", "Giraffe"),
    Animal("H", "\U0001F40E", "Horse"),
    Animal("I", "\U0001F98E", "Iguana"),
    Animal("J", "\U0001F406", "Jaguar"),
    Animal("K", "\U0001F998", "Kangaroo"),
    Animal("L", "\U0001F981", "Lion"),
    Animal("M", "\U0001F412", "Monkey"),
    Animal("O", "\U0001F989", "Owl"),
    Animal("P", "\U0001F427", "Penguin"),
    Animal("R", "\U0001F430", "Rabbit"),
    Animal("S", "\U0001F40D", "Snake"),
    Animal("T", "\U0001F42F", "Tiger"),
    Animal("U", "\U0001F984", "Unicorn"),
    Animal("W", "\U0001F43A", "Wolf"),
    Animal("Y", "\U0001F403", "Yak"),
    Animal("Z", "\U0001F993", "Zebra"),
)


def subset_unicodes() -> str:
    """The ``--unicodes`` argument for re-subsetting the emoji font."""
    return ",".join("U+%04X" % ord(a.emoji) for a in ALL)
