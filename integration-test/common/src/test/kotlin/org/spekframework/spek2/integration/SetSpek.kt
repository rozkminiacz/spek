package org.spekframework.spek2.integration

import org.spekframework.spek2.Spek
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object SetSpek: Spek({
    describe("A set") {
        val set by memoized { mutableSetOf<String>() }

        context("is empty") {
            it("should have a size of 0") {
                assertEquals(0, set.size)
            }

            it("should throw when first is invoked") {
                assertFailsWith(NoSuchElementException::class) {
                    set.first()
                }
            }
        }
    }
})
