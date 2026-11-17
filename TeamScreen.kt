package com.example.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Fleet
import com.example.data.model.FleetMember
import com.example.data.model.StaffAccountRegistry
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadgePill
import com.example.ui.i18n.LanguageSelectorPillRow
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.LocalOnLanguageSelected
import com.example.ui.theme.HighwayAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KrishnaCrimson
import com.example.ui.theme.VerifiedTeal
import com.example.ui.viewmodel.UiState

@Composable
fun TeamScreen(
    fleet: Fleet?,
    currentUserId: String,
    currentRole: UserRole,
    activeSlotUsername: String,
    membersState: UiState<List<FleetMember>>,
    onUpdateMemberRole: (FleetMember, UserRole) -> Unit,
    onSyncAccess: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val onLanguageSelected = LocalOnLanguageSelected.current

    val memberList = (membersState as? UiState.Success)?.data.orEmpty()
    val claimedBySlot = remember(memberList) {
        memberList.filter { it.slotUsername.isNotBlank() }
            .associateBy { it.slotUsername }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Fleet & Role Overview
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fleet?.name ?: strings.appTitle, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = strings.activeMembersOwners(
                                    fleet?.memberUids?.size ?: 1,
                                    fleet?.ownerUids?.size ?: 1
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeSlotUsername.isNotBlank()) {
                                Text(
                                    text = activeSlotUsername,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VerifiedTeal
                                )
                            }
                        }
                        RoleBadgePill(role = currentRole)
                    }

                    Text(
                        text = if (currentRole == UserRole.OWNER) strings.ownerAuthorityDesc else strings.managerAccessDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Trilingual App Language Card (English / हिन्दी / ગુજરાતી)
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Translate, contentDescription = null, tint = KrishnaCrimson)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.appLanguageSettingsTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = strings.appLanguageSettingsDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguageSelectorPillRow(
                        selected = currentLanguage,
                        onSelect = onLanguageSelected,
                        darkSurface = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (currentRole == UserRole.OWNER) {
            // Owner-only 10-Account Audit Card (5 Owners + 5 Managers usernames & claimed status ONLY; zero passwords)
            item {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.authorizedSlotListTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = strings.authorizedSlotListDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        StaffAccountRegistry.authorizedSlots.forEach { slot ->
                            val claimedMember = claimedBySlot[slot.username]
                            val isOwnerSlot = slot.role == UserRole.OWNER
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("slot_audit_${slot.username}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isOwnerSlot) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Badge,
                                        contentDescription = null,
                                        tint = if (isOwnerSlot) HighwayAmber else VerifiedTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slot.username,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (claimedMember != null) {
                                                strings.slotStatusClaimedBy(claimedMember.displayName)
                                            } else {
                                                strings.slotStatusAvailable
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (claimedMember != null) VerifiedTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RoleBadgePill(role = slot.role)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onSyncAccess,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.syncHistoricalTrips)
                        }
                    }
                }
            }

            when (membersState) {
                is UiState.Success -> {
                    item {
                        Text(
                            strings.fleetOwnersAndManagers(membersState.data.size),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(membersState.data, key = { it.userId }) { member ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        member.displayName + if (member.userId == currentUserId) " ${strings.youTag}" else "",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (member.slotUsername.isNotBlank()) {
                                        Text(
                                            member.slotUsername,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VerifiedTeal
                                        )
                                    }
                                    if (member.email.isNotBlank()) {
                                        Text(
                                            member.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RoleBadgePill(role = member.userRole)
                                if (member.userId != currentUserId) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val targetRole = if (member.userRole == UserRole.OWNER) UserRole.MANAGER else UserRole.OWNER
                                    TextButton(onClick = { onUpdateMemberRole(member, targetRole) }) {
                                        Icon(
                                            Icons.Rounded.SwapHoriz,
                                            contentDescription = strings.roleLabel(targetRole.wireValue),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    item { Text(membersState.message, color = MaterialTheme.colorScheme.error) }
                }
                UiState.Loading -> Unit
            }
        } else {
            // Manager view: strict password privacy & anti-tamper immutability reminders
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = HighwayAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            strings.managerPasswordHiddenNotice,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = VerifiedTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            strings.managerLockedNoticeTeam,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
