package com.dividetask.homeschoolteacher.reading

data class Animal(
    val letter: Char,
    val emoji: String,
    val name: String,
)

object Animals {
    val all: List<Animal> = listOf(
        Animal('A', "🐜", "Ant"),
        Animal('B', "🐻", "Bear"),
        Animal('C', "🐱", "Cat"),
        Animal('D', "🐶", "Dog"),
        Animal('E', "🐘", "Elephant"),
        Animal('F', "🦊", "Fox"),
        Animal('G', "🦒", "Giraffe"),
        Animal('H', "🐎", "Horse"),
        Animal('I', "🦎", "Iguana"),
        Animal('J', "🐆", "Jaguar"),
        Animal('K', "🦘", "Kangaroo"),
        Animal('L', "🦁", "Lion"),
        Animal('M', "🐒", "Monkey"),
        Animal('O', "🦉", "Owl"),
        Animal('P', "🐧", "Penguin"),
        Animal('R', "🐰", "Rabbit"),
        Animal('S', "🐍", "Snake"),
        Animal('T', "🐯", "Tiger"),
        Animal('U', "🦄", "Unicorn"),
        Animal('W', "🐺", "Wolf"),
        Animal('Y', "🐃", "Yak"),
        Animal('Z', "🦓", "Zebra"),
    )
}
