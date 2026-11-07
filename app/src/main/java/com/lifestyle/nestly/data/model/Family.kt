package com.lifestyle.nestly.data.model

import java.time.LocalDate

data class FamilyEntity(
    val id: String,
    val name: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val version: Int = 1,
)

data class CaregiverEntity(
    val id: String,
    val familyId: String,
    val name: String,
    /** Supabase Auth user id, null for name-only caregivers who don't use the app. */
    val userId: String? = null,
    val role: String = "MEMBER",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val version: Int = 1,
)

/**
 * A family member may soft-delete a name-only caregiver, themselves, or, when they
 * are an OWNER, another signed-in member. The last OWNER stays so the family
 * always has someone who can manage membership.
 */
fun canRemoveCaregiver(actor: CaregiverEntity?, target: CaregiverEntity, family: List<CaregiverEntity>): Boolean {
    if (actor == null) return false
    if (target.role == "OWNER" && family.count { it.role == "OWNER" } <= 1) return false
    val isSelf = actor.id == target.id
    val nameOnly = target.userId == null
    return nameOnly || isSelf || actor.role == "OWNER"
}

fun caregiverRemovalDeniedMessage(target: CaregiverEntity, family: List<CaregiverEntity>): String =
    if (target.role == "OWNER" && family.count { it.role == "OWNER" } <= 1) {
        "A family needs to keep one owner."
    } else {
        "Only the family owner can remove another signed-in caregiver."
    }

data class ProfileEntity(
    val id: String,
    val familyId: String,
    val name: String,
    val birthDate: LocalDate?,
    val adjustedBirthDate: LocalDate? = null,
    val sex: Sex?,
    val profileType: ProfileType = ProfileType.CHILD,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val version: Int = 1,
)
