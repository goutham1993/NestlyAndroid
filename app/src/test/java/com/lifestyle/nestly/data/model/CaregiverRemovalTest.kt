package com.lifestyle.nestly.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaregiverRemovalTest {
    private val owner = caregiver("owner", role = "OWNER", userId = "user-owner")
    private val member = caregiver("member", role = "MEMBER", userId = "user-member")
    private val otherOwner = caregiver("owner-2", role = "OWNER", userId = "user-owner-2")
    private val grandma = caregiver("grandma", role = "MEMBER", userId = null)

    @Test
    fun memberCanRemoveNameOnlyCaregiver() {
        val family = listOf(owner, member, grandma)
        assertTrue(canRemoveCaregiver(member, grandma, family))
    }

    @Test
    fun memberCannotRemoveAnotherSignedInCaregiver() {
        val partner = caregiver("partner", role = "MEMBER", userId = "user-partner")
        val family = listOf(owner, member, partner)
        assertFalse(canRemoveCaregiver(member, partner, family))
        assertFalse(canRemoveCaregiver(member, owner, family))
        assertEquals(
            "Only the family owner can remove another signed-in caregiver.",
            caregiverRemovalDeniedMessage(partner, family),
        )
    }

    @Test
    fun memberCanLeave() {
        val family = listOf(owner, member)
        assertTrue(canRemoveCaregiver(member, member, family))
    }

    @Test
    fun soleOwnerCannotLeave() {
        val family = listOf(owner, member, grandma)
        assertFalse(canRemoveCaregiver(owner, owner, family))
        assertEquals(
            "A family needs to keep one owner.",
            caregiverRemovalDeniedMessage(owner, family),
        )
    }

    @Test
    fun ownerCanRemoveMembersAndLeaveWhenAnotherOwnerRemains() {
        val family = listOf(owner, otherOwner, member, grandma)
        assertTrue(canRemoveCaregiver(owner, member, family))
        assertTrue(canRemoveCaregiver(owner, grandma, family))
        assertTrue(canRemoveCaregiver(owner, otherOwner, family))
        assertTrue(canRemoveCaregiver(owner, owner, family))
    }

    @Test
    fun signedOutActorCannotRemoveAnyone() {
        val family = listOf(owner, grandma)
        assertFalse(canRemoveCaregiver(null, grandma, family))
        assertFalse(canRemoveCaregiver(null, owner, family))
    }

    private fun caregiver(id: String, role: String, userId: String?) = CaregiverEntity(
        id = id,
        familyId = "family",
        name = id,
        userId = userId,
        role = role,
        createdAt = 0,
        updatedAt = 0,
    )
}
