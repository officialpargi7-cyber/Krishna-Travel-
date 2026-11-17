package com.example.data.model

import java.security.MessageDigest
import java.util.Locale

data class StaffSlotSpec(
    val slotIndex: Int,
    val username: String,
    val digestHex: String,
    val role: UserRole
)

data class VerifiedStaffSlot(
    val spec: StaffSlotSpec,
    val digestHex: String
)

object StaffAccountRegistry {
    const val SHARED_FLEET_ID = "fleet_krishna_travels_hq"
    const val SHARED_FLEET_NAME = "Krishna Travels"
    const val ACCESS_REGISTRY_DOC = "krishna_hq"
    private const val SALT_PREFIX = "KT_BUS_2026:"

    // 10 Exclusive Authorized Accounts: 5 Owners + 5 Managers (salted SHA-256 digests only; no plaintext passwords stored in app)
    val authorizedSlots: List<StaffSlotSpec> = listOf(
        // 5 Owners
        StaffSlotSpec(1, "krishna_owner1", "eb176276dc9843e85a38394ffe8bd49fff56dcab686e0a78a7bc1a7a3ecf242f", UserRole.OWNER),
        StaffSlotSpec(2, "krishna_owner2", "4e7fefa0dbc087a2614ec20573a74175527e8fe39e04387fb16d12f70d072d0f", UserRole.OWNER),
        StaffSlotSpec(3, "krishna_owner3", "4554e8796d371dfac957850cdee52c114d0f062674f2e79a564f99a42474831a", UserRole.OWNER),
        StaffSlotSpec(4, "krishna_owner4", "1d22212b7bbb96800932136f03342869aa9414ca4f6eac96d54f5a6f779708c6", UserRole.OWNER),
        StaffSlotSpec(5, "krishna_owner5", "c091ca928f644accc24017f1eb9a957a6491aed2af37a5982da19b93a77a649f", UserRole.OWNER),
        // 5 Managers
        StaffSlotSpec(1, "krishna_mgr1", "3235c408a42c1f27ddd0a00bc1c8d7c916a77bf07c30ced88568369187c85954", UserRole.MANAGER),
        StaffSlotSpec(2, "krishna_mgr2", "4e1b6f6e6ccf0123c9cf3f95518cfab8b7d25a88e4ece470767bdca7e3f97a9f", UserRole.MANAGER),
        StaffSlotSpec(3, "krishna_mgr3", "6a8ebe5856f4c823188cb969e08be020b8272c9f5d8a00ec7e4e6ef989a7b0c4", UserRole.MANAGER),
        StaffSlotSpec(4, "krishna_mgr4", "5b4182e766fa24ad4e0f7ecd8c0736bb2b667d0430a6fdee2ea59517ee80eb23", UserRole.MANAGER),
        StaffSlotSpec(5, "krishna_mgr5", "b1ff6c53228fbb8ebae266e13aa384e831d8518f1a25e9a6efb1366a0be26a50", UserRole.MANAGER)
    )

    fun computeDigest(username: String, password: String): String {
        val normalizedUser = username.trim().lowercase(Locale.ROOT)
        val input = "$SALT_PREFIX$normalizedUser:${password.trim()}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyCredentials(username: String, password: String): VerifiedStaffSlot? {
        val normalizedUser = username.trim().lowercase(Locale.ROOT)
        if (normalizedUser.isBlank() || password.isBlank()) return null
        val computed = computeDigest(normalizedUser, password)
        val match = authorizedSlots.firstOrNull {
            it.username == normalizedUser && it.digestHex == computed
        } ?: return null
        return VerifiedStaffSlot(spec = match, digestHex = computed)
    }
}
