package com.obsidianbackup.testing

import io.github.serpro69.kfaker.Faker
import java.util.UUID

/**
 * Test fixtures for creating test data objects.
 */
object TestFixtures {
    val faker = Faker()
    
    fun randomString(length: Int = 10): String {
        return (1..length)
            .map { ('a'..'z').random() }
            .joinToString("")
    }
    
    fun randomEmail(): String = faker.internet.email()
    
    fun randomUUID(): String = UUID.randomUUID().toString()
    
    fun randomInt(min: Int = 0, max: Int = 100): Int {
        return (min..max).random()
    }
    
    fun randomLong(min: Long = 0L, max: Long = 1000L): Long {
        return (min..max).random()
    }
    
    fun randomBoolean(): Boolean = listOf(true, false).random()
    
    fun randomFilePath(): String {
        val dirs = List(3) { faker.file.fileName() }
        return dirs.joinToString("/", prefix = "/")
    }
    
    fun randomFileName(): String {
        return "${faker.file.fileName()}.${faker.file.extension()}"
    }
}
