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
