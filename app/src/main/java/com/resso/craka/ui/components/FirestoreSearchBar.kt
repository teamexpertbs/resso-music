package com.resso.craka.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTextSecondary

enum class SearchFilterType(val label: String) {
    ALL("All"),
    TITLE("By Title"),
    ARTIST("By Artist")
}

/**
 * Material 3 Search Bar component that allows users to filter songs
 * by title or artist, integrating directly with the Firebase Firestore Repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirestoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterType: SearchFilterType,
    onFilterTypeChange: (SearchFilterType) -> Unit,
    isSearching: Boolean,
    onSearchTriggered: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search songs by title or artist in Firestore..."
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_search_bar_container"),
        shape = RoundedCornerShape(20.dp),
        color = RessoSurface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header Badge showing Firestore Integration Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FIRESTORE LIVE SYNC",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Firestore Connected",
                        tint = RessoSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Project: sleepok",
                        color = RessoTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Material 3 Search Text Field
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = when (filterType) {
                            SearchFilterType.ALL -> "Search songs or artists..."
                            SearchFilterType.TITLE -> "Filter by song title..."
                            SearchFilterType.ARTIST -> "Filter by artist name..."
                        },
                        color = RessoTextSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Firestore",
                        tint = if (query.isNotBlank()) RessoPrimary else RessoTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                color = RessoPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = onClearQuery,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = RessoTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RessoPrimary,
                    unfocusedBorderColor = RessoCardBg,
                    focusedContainerColor = RessoCardBg.copy(alpha = 0.8f),
                    unfocusedContainerColor = RessoCardBg.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = RessoPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchTriggered(query) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firestore_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips: All, By Title, By Artist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter By",
                    tint = RessoTextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                SearchFilterType.values().forEach { type ->
                    val isSelected = filterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterTypeChange(type) },
                        label = {
                            Text(
                                text = type.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            when (type) {
                                SearchFilterType.ALL -> Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                SearchFilterType.TITLE -> Icon(
                                    imageVector = Icons.Default.Title,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                SearchFilterType.ARTIST -> Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = RessoCardBg,
                            selectedContainerColor = RessoPrimary,
                            labelColor = RessoTextSecondary,
                            selectedLabelColor = Color.White,
                            iconColor = RessoTextSecondary,
                            selectedLeadingIconColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) RessoPrimary else RessoCardBg,
                            selectedBorderColor = RessoPrimary,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_chip_${type.name.lowercase()}")
                    )
                }
            }
        }
    }
}
