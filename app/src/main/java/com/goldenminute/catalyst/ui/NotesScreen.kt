package com.goldenminute.catalyst.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.NoteItem
import com.goldenminute.catalyst.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    viewModel: TodoViewModel,
    listState: LazyStaggeredGridState,
    isFullScreen: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = if (isFullScreen) 100.dp else 130.dp,
    isInMultiSelectMode: Boolean = false,
    selectedNoteIds: List<Int> = emptyList(),
    onToggleSelectNote: (NoteItem) -> Unit = {},
    onStartMultiSelect: (NoteItem) -> Unit = {},
    onNoteClick: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onAddNoteClick: () -> Unit = {},
    onEnterFullScreen: () -> Unit = {},
    onAddCategoryClick: () -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val noteCategories by viewModel.noteCategories.collectAsState()
    val selectedNoteCatId by viewModel.selectedNoteCategoryId.collectAsState()
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    val isGridView by viewModel.isNotesGridView.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Recent", "Short", "Long"

    // 0. Extract user-defined tags from all notes
    val extractedTags = remember(notes) {
        val hashtagRegex = Regex("#[\\w\\u0600-\\u06FF]+")
        val tags = mutableSetOf<String>()
        notes.forEach { note ->
            tags.addAll(hashtagRegex.findAll(note.title).map { it.value })
            tags.addAll(hashtagRegex.findAll(note.content).map { it.value })
        }
        tags.toList().sorted()
    }

    // 1. Tag Filtering Logic
    val filteredNotes = remember(notes, selectedFilter, selectedNoteCatId) {
        notes.filter { note ->
            val matchesCategory = selectedNoteCatId == null || note.categoryId == selectedNoteCatId
            val matchesFilter = when {
                selectedFilter == "Recent" -> note.createdAt >= System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                selectedFilter == "Short" -> {
                    val wordCount = if (note.content.isBlank()) 0 else note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    wordCount <= 30
                }
                selectedFilter == "Long" -> {
                    val wordCount = if (note.content.isBlank()) 0 else note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    wordCount > 30
                }
                selectedFilter.startsWith("#") -> {
                    note.title.contains(selectedFilter, ignoreCase = true) || 
                    note.content.contains(selectedFilter, ignoreCase = true)
                }
                else -> true
            }

            matchesCategory && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Redesigned Header Row: Beautiful dynamic title and count badge + Segmented Grid/List toggle + Multi-Select Actions
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(400))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateLight.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(R.string.my_journal),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            fontSize = 21.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.notes_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
                
                // Gorgeous Glass Badge for Entries count
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${filteredNotes.size}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isInMultiSelectMode) {
                    // Beautiful Dual-Segmented Pill Control
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SlateDark.copy(alpha = 0.5f))
                            .border(BorderStroke(1.dp, BorderColor.copy(alpha = 0.25f)), CircleShape)
                            .padding(4.dp)
                            .testTag("notes_view_mode_toggle")
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { viewModel.setNotesGridView(!isGridView) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Grid option
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isGridView) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.setNotesGridView(true) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("notes_grid_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid Layout",
                                tint = if (isGridView) NeonCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // List option
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (!isGridView) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.setNotesGridView(false) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("notes_list_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "List Layout",
                                tint = if (!isGridView) NeonCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Quick Filters Row (Smooth Scrolling & High-Fidelity Interactive Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = mutableListOf(
                "All" to stringResource(R.string.filter_all),
                "Recent" to stringResource(R.string.filter_recent),
                "Short" to stringResource(R.string.filter_short),
                "Long" to stringResource(R.string.filter_long)
            )
            
            extractedTags.forEach { tag ->
                filters.add(tag to tag)
            }

            filters.forEach { (filterType, label) ->
                val isSelected = selectedFilter == filterType
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                    label = "chip_press"
                )

                val bgGlow = if (isSelected) {
                    Brush.horizontalGradient(listOf(NeonCyan, NeonBlue))
                } else {
                    Brush.horizontalGradient(listOf(SlateDark.copy(alpha = 0.4f), SlateDark.copy(alpha = 0.4f)))
                }

                val icon = when {
                    filterType == "Recent" -> Icons.Default.AccessTime
                    filterType == "Short" -> Icons.AutoMirrored.Filled.ShortText
                    filterType == "Long" -> Icons.AutoMirrored.Filled.Subject
                    filterType.startsWith("#") -> Icons.Default.Tag
                    else -> Icons.Default.Book
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgGlow)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else BorderColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedFilter = filterType }
                        .padding(horizontal = 12.dp)
                        .testTag("note_filter_chip_$filterType"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) ObsidianBlack else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = if (isSelected) ObsidianBlack else TextSecondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        CategoryRowSelector(
            categories = noteCategories,
            selectedId = selectedNoteCatId,
            onSelectId = { viewModel.selectedNoteCategoryId.value = it },
            onAddCategoryClick = onAddCategoryClick,
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
            }
        }

        // Notes List / Empty State area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding.coerceAtLeast(80.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyNotesStateView(onAddClick = onAddNoteClick)
                }
            } else {
                SwipeToFullScreenContainer(
                    isAlreadyFullScreen = isFullScreen,
                    canScrollBackward = listState.canScrollBackward,
                    onEnterFullScreen = onEnterFullScreen,
                    indicatorTopPadding = 0.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                category = categoriesMap[note.categoryId],
                                isGridView = isGridView,
                                isSelected = selectedNoteIds.contains(note.id),
                                isInMultiSelectMode = isInMultiSelectMode,
                                modifier = Modifier.animateItem(),
                                onClick = { onNoteClick(note) },
                                onLongClick = { onStartMultiSelect(note) },
                                onDelete = { onDeleteNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Accent palette for beautiful custom colored cards (made public)
data class NotesCardColorPalette(
    val label: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
)

@Composable
fun getNotesPaletteList(): List<NotesCardColorPalette> {
    return listOf(
        NotesCardColorPalette(stringResource(R.string.palette_nebula_dream), Color(0xFF8B5CF6), Color(0xFFC084FC), Color(0xFF8B5CF6).copy(alpha = 0.18f)),
        NotesCardColorPalette(stringResource(R.string.palette_ocean_glow), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF06B6D4).copy(alpha = 0.18f)),
        NotesCardColorPalette(stringResource(R.string.palette_emerald_spark), Color(0xFF10B981), Color(0xFF059669), Color(0xFF10B981).copy(alpha = 0.18f)),
        NotesCardColorPalette(stringResource(R.string.palette_amber_aura), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFF59E0B).copy(alpha = 0.18f)),
        NotesCardColorPalette(stringResource(R.string.palette_cosmic_rose), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFFEC4899).copy(alpha = 0.18f))
    )
}

@Composable
fun resolveNotePalette(colorIndex: Int): NotesCardColorPalette {
    if (colorIndex < 0) {
        val baseColor = Color(colorIndex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorIndex, hsv)
        hsv[1] = (hsv[1] * 1.2f).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 0.8f).coerceIn(0f, 1f)
        val secondaryColor = Color(android.graphics.Color.HSVToColor(hsv))
        
        return NotesCardColorPalette(
            label = stringResource(com.goldenminute.catalyst.R.string.custom),
            primaryColor = baseColor,
            secondaryColor = secondaryColor,
            glowColor = baseColor.copy(alpha = 0.18f)
        )
    } else {
        val notesPalette = getNotesPaletteList()
        val index = (colorIndex.coerceAtLeast(0)) % notesPalette.size
        return notesPalette[index]
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteItem,
    category: com.goldenminute.catalyst.data.Category? = null,
    isGridView: Boolean = true,
    isSelected: Boolean = false,
    isInMultiSelectMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    // 1. Resolve Palette uniquely per note
    val resolvedPalette = resolveNotePalette(note.colorIndex)
    val palette = remember(resolvedPalette) { resolvedPalette }

    // 2. Format Date
    val isPersian = Locale.getDefault().language == "fa"
    val dateStr = remember(note.createdAt, isPersian) {
        if (isPersian) {
            JalaliCalendarHelper.formatJalali(note.createdAt, includeTime = true, includeYear = false)
        } else {
            val sdf = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
            sdf.format(Date(note.createdAt))
        }
    }

    // 3. Word count & Reading time
    val wordCount = remember(note.content) {
        if (note.content.isBlank()) 0 else note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val readTime = remember(wordCount) {
        (wordCount / 200 + 1).coerceAtLeast(1)
    }

    // 4. State Animations
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (isInMultiSelectMode && !isSelected) 0.55f else 1.0f,
        label = "card_alpha_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alphaAnim
            )
            .shadow(
                elevation = if (isSelected) 14.dp else 8.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false,
                ambientColor = if (isSelected) NeonCyan else palette.primaryColor,
                spotColor = if (isSelected) NeonCyan else palette.primaryColor
            )
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.2.dp else 1.2.dp,
                brush = Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(NeonCyan, NeonBlue)
                    } else {
                        listOf(
                            palette.primaryColor.copy(alpha = 0.6f),
                            BorderColor.copy(alpha = 0.2f)
                        )
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // High fidelity outer layout with left vertical indicator gradient
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.glowColor, Color.Transparent),
                        radius = 220f
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant vertical accent bar on the left edge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(5.dp)
                    .height(if (isGridView) 120.dp else 70.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.primaryColor, palette.secondaryColor)
                        )
                    )
            )

            // Inner content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(18.dp)
            ) {
                // Header section: Title, Date, Delete button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = note.title.ifEmpty { if (getSystemLocale().language == "fa") "بدون عنوان" else "Untitled" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = TextPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Date + Metadata row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                        }
                    }

                    if (isInMultiSelectMode) {
                        // Beautiful Glassmorphic Checkbox Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) NeonCyan.copy(alpha = 0.25f) else SlateLight.copy(alpha = 0.3f)
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        if (isSelected) NeonCyan else BorderColor.copy(alpha = 0.5f)
                                    ),
                                    CircleShape
                                )
                                .testTag("select_note_checkbox_${note.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Glassmorphic Delete button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SlateLight.copy(alpha = 0.6f))
                                .border(BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f)), CircleShape)
                                .clickable { onDelete() }
                                .testTag("delete_note_button_${note.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete note",
                                tint = PriorityHigh.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body content with adaptive max lines depending on Grid vs List
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        color = TextSecondary
                    ),
                    maxLines = if (isGridView) 4 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Footer: Tag Pill & Smart Statistics (FlowRow ensures full tag name is never truncated)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (category != null) {
                        val catColor = remember(category.colorHex) {
                            safeParseColor(category.colorHex)
                        }
                        val catDisplayName = when (category.name) {
                            "Work" -> stringResource(R.string.category_work)
                            "Personal" -> stringResource(R.string.category_personal)
                            "Shopping" -> stringResource(R.string.category_shopping)
                            "Health" -> stringResource(R.string.category_health)
                            "Study" -> stringResource(R.string.category_study)
                            "Finance" -> stringResource(R.string.category_finance)
                            else -> category.name
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(catColor.copy(alpha = 0.16f))
                                .border(BorderStroke(0.6.dp, catColor.copy(alpha = 0.35f)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = catDisplayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = catColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Smart statistics footer chip (Word count and read duration)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.primaryColor.copy(alpha = 0.08f))
                            .border(BorderStroke(0.5.dp, palette.primaryColor.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = palette.secondaryColor,
                            modifier = Modifier.size(11.dp)
                        )
                        val isPersian = Locale.getDefault().language == "fa"
                        val wcStr = if (isPersian) JalaliCalendarHelper.toPersianDigits(wordCount.toString()) else wordCount.toString()
                        val rtStr = if (isPersian) JalaliCalendarHelper.toPersianDigits(readTime.toString()) else readTime.toString()
                        Text(
                            text = "$wcStr ${stringResource(R.string.word_count_suffix)} • $rtStr ${stringResource(R.string.read_time_suffix)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.secondaryColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesStateView(
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_notes_state")

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_y"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compact modern illustration
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Dynamic Glowing Halo Background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(
                        scaleX = scaleAnim,
                        scaleY = scaleAnim
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.22f),
                                NeonPurple.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Glassmorphic Floating Base Card
            Card(
                modifier = Modifier
                    .size(56.dp, 68.dp)
                    .graphicsLayer(
                        translationY = floatAnim * 0.5f,
                        rotationZ = floatAnim * 0.1f
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = SlateDark.copy(alpha = 1.0f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BorderColor,
                            NeonCyan.copy(alpha = 0.25f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Line 1
                    Box(
                        modifier = Modifier
                            .size(30.dp, 4.dp)
                            .background(NeonCyan, RoundedCornerShape(2.dp))
                    )
                    // Line 2
                    Box(
                        modifier = Modifier
                            .size(36.dp, 2.dp)
                            .background(TextPrimary.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
                    )
                    // Line 3
                    Box(
                        modifier = Modifier
                            .size(32.dp, 2.dp)
                            .background(TextPrimary.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
                    )
                }
            }

            // 3. Floating Accent Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .graphicsLayer(
                        translationY = -floatAnim * 0.3f,
                        rotationZ = -floatAnim * 0.15f
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonCyan, NeonBlue)
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, SlateLight.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = null,
                    tint = ObsidianBlack,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title text
        Text(
            text = stringResource(R.string.notes_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description text
        Text(
            text = stringResource(R.string.notes_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .padding(horizontal = 16.dp)
        )

        if (onAddClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.new_note),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
