package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Article
import com.example.data.MouvementStock
import com.example.ui.MainViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ScannableItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Catalogue Articles", "Historique Mouvements")

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userRole = currentUser?.role ?: "Consultation seule"
    val canModify = userRole == "Administrateur" || userRole == "Gestionnaire de stock"

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var showMovementDialog by remember { mutableStateOf(false) }
    var selectedArticleForMovement by remember { mutableStateOf<Article?>(null) }
    var selectedArticleForEdit by remember { mutableStateOf<Article?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Module Title Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gestion des Stocks",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Suivi, alertes critiques et mouvements",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (canModify) {
                Button(
                    onClick = {
                        selectedArticleForEdit = null
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouvel Article", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> CatalogueTab(
                viewModel = viewModel,
                canModify = canModify,
                onRecordMovement = { article ->
                    selectedArticleForMovement = article
                    showMovementDialog = true
                },
                onEditArticle = { article ->
                    selectedArticleForEdit = article
                    showAddDialog = true
                }
            )
            1 -> MouvementsTab(viewModel = viewModel)
        }
    }

    // --- Dialog: Add / Edit Article ---
    if (showAddDialog) {
        AddEditArticleDialog(
            article = selectedArticleForEdit,
            onDismiss = { showAddDialog = false },
            onSave = { ref, des, cat, unit, threshold, initialStock, price ->
                if (selectedArticleForEdit == null || selectedArticleForEdit!!.id == 0L) {
                    viewModel.addArticle(ref, des, cat, unit, threshold, initialStock, price)
                } else {
                    viewModel.updateArticle(
                        selectedArticleForEdit!!.copy(
                            reference = ref,
                            designation = des,
                            categorie = cat,
                            unite = unit,
                            seuilAlerte = threshold,
                            prixUnitaire = price
                        )
                    )
                }
                showAddDialog = false
            },
            onDelete = { article ->
                viewModel.deleteArticle(article)
                showAddDialog = false
            }
        )
    }

    // --- Dialog: Record Stock Movement ---
    if (showMovementDialog && selectedArticleForMovement != null) {
        RecordMovementDialog(
            article = selectedArticleForMovement!!,
            onDismiss = { showMovementDialog = false },
            onSave = { type, qty, motif ->
                viewModel.recordStockMovement(
                    articleId = selectedArticleForMovement!!.id,
                    designation = selectedArticleForMovement!!.designation,
                    typeMouvement = type,
                    quantite = qty,
                    motif = motif,
                    currentQty = selectedArticleForMovement!!.quantiteStock
                )
                showMovementDialog = false
            }
        )
    }
}

@Composable
fun CatalogueTab(
    viewModel: MainViewModel,
    canModify: Boolean,
    onRecordMovement: (Article) -> Unit,
    onEditArticle: (Article) -> Unit
) {
    val articles by viewModel.allArticles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.articleSearchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.articleCategoryFilter.collectAsStateWithLifecycle()

    var showScanner by remember { mutableStateOf(false) }

    val filteredArticles = remember(articles, searchQuery, categoryFilter) {
        articles.filter { art ->
            val matchesSearch = art.designation.contains(searchQuery, ignoreCase = true) ||
                    art.reference.contains(searchQuery, ignoreCase = true)
            val matchesCategory = categoryFilter == "Tous" || art.categorie == categoryFilter
            matchesSearch && matchesCategory
        }
    }

    val scannableItems = remember(articles) {
        articles.map { ScannableItem(it.reference, it.designation, it.categorie) }
    }

    // Check if searchQuery matches exactly a single reference to show identification/actions HUD banner
    val exactMatchedArticle = remember(articles, searchQuery) {
        if (searchQuery.isNotBlank()) {
            articles.find { it.reference.equals(searchQuery.trim(), ignoreCase = true) }
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Category Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.articleSearchQuery.value = it },
                label = { Text("Rechercher un article...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showScanner = true },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scanner",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Category Filter trigger
            var expanded by remember { mutableStateOf(false) }
            val categories = listOf("Tous", "Matière première", "Produit fini", "Pièce de rechange", "Consommable")

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(categoryFilter)
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.articleCategoryFilter.value = cat
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Camera identification banner or Unrecognized code banner
        if (exactMatchedArticle != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Article Identifié par Scan !",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${exactMatchedArticle.designation} (${exactMatchedArticle.reference})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Stock actuel : ${exactMatchedArticle.quantiteStock} ${exactMatchedArticle.unite}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onRecordMovement(exactMatchedArticle) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mouvement", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(
                            onClick = { viewModel.articleSearchQuery.value = "" },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        } else if (searchQuery.isNotBlank() && filteredArticles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Référence inconnue : $searchQuery",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "Aucun article dans votre stock ne correspond à ce code-barres. Souhaitez-vous créer un nouvel article avec cette référence ?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (canModify) {
                            Button(
                                onClick = {
                                    // Open Add Article Dialog with prefilled scanned reference
                                    onEditArticle(Article(
                                        id = 0,
                                        reference = searchQuery.trim(),
                                        designation = "",
                                        categorie = "Matière première",
                                        unite = "pièces",
                                        seuilAlerte = 5,
                                        quantiteStock = 0,
                                        prixUnitaire = 0.0
                                    ))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Créer l'article", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.articleSearchQuery.value = "" },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Text("Annuler", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (filteredArticles.isEmpty()) {
            EmptyStateView(
                title = "Aucun article trouvé",
                description = "Essayez de modifier votre recherche ou ajoutez un nouvel article.",
                icon = Icons.Default.Inventory
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredArticles) { art ->
                    ArticleItemCard(
                        article = art,
                        canModify = canModify,
                        onRecordMovement = { onRecordMovement(art) },
                        onEditArticle = { onEditArticle(art) }
                    )
                }
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            title = "Scanner d'Articles & Outils de Stock",
            subtitle = "Pointez le code-barres ou sélectionnez un article ci-dessous",
            scannableItems = scannableItems,
            onDismiss = { showScanner = false },
            onScanResult = { result ->
                viewModel.articleSearchQuery.value = result
                showScanner = false
            }
        )
    }
}

@Composable
fun ArticleItemCard(
    article: Article,
    canModify: Boolean,
    onRecordMovement: () -> Unit,
    onEditArticle: () -> Unit
) {
    val isAlert = article.quantiteStock <= article.seuilAlerte
    val accentColor = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.designation,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Réf: ${article.reference} • ${article.categorie}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Edit/Detail Button
                if (canModify) {
                    IconButton(onClick = onEditArticle) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(accentColor, CircleShape)
                    )
                    Text(
                        text = "${article.quantiteStock} ${article.unite}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )

                    if (isAlert) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "CRITIQUE (Seuil: ${article.seuilAlerte})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Unit Price
                Text(
                    text = String.format("%,.0f F CFA / u", article.prixUnitaire),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Quick Operations (Entrée / Sortie Mouvements)
            if (canModify) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRecordMovement,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Mouvement stock",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MouvementsTab(viewModel: MainViewModel) {
    val movements by viewModel.allMouvements.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historique complet des Entrées & Sorties",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Gray
        )

        if (movements.isEmpty()) {
            EmptyStateView(
                title = "Aucun mouvement enregistré",
                description = "Les mouvements de stock apparaîtront ici dès qu'une entrée ou sortie sera enregistrée.",
                icon = Icons.Default.History
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(movements) { mvt ->
                    MovementItemCard(mvt = mvt)
                }
            }
        }
    }
}

@Composable
fun MovementItemCard(mvt: MouvementStock) {
    val isEntry = mvt.typeMouvement == "Entrée"
    val badgeColor = if (isEntry) Color(0xFF2E7D32) else Color(0xFFC62828)
    val containerBg = if (isEntry) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flow indicator badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(containerBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEntry) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Description Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mvt.articleDesignation,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Motif: ${mvt.motif}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Par: ${mvt.utilisateurNom} • Le ${formatMvtDate(mvt.date)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.Gray
                )
            }

            // Quantity badge
            Text(
                text = "${if (isEntry) "+" else "-"}${mvt.quantite}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
        }
    }
}

@Composable
fun AddEditArticleDialog(
    article: Article?,
    onDismiss: () -> Unit,
    onSave: (ref: String, des: String, cat: String, unit: String, threshold: Int, initialStock: Int, price: Double) -> Unit,
    onDelete: (Article) -> Unit
) {
    var ref by remember { mutableStateOf(article?.reference ?: "") }
    var des by remember { mutableStateOf(article?.designation ?: "") }
    var selectedCat by remember { mutableStateOf(article?.categorie ?: "Matière première") }
    var unit by remember { mutableStateOf(article?.unite ?: "pièces") }
    var thresholdStr by remember { mutableStateOf(article?.seuilAlerte?.toString() ?: "5") }
    var initialStockStr by remember { mutableStateOf(article?.quantiteStock?.toString() ?: "0") }
    var priceStr by remember { mutableStateOf(article?.prixUnitaire?.toString() ?: "") }

    var catExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Matière première", "Produit fini", "Pièce de rechange", "Consommable")

    var showScanDialogForRef by remember { mutableStateOf(false) }

    if (showScanDialogForRef) {
        BarcodeScannerDialog(
            title = "Scanner le code-barres de l'article",
            subtitle = "Pointez la caméra vers le code-barres ou QR code pour remplir la référence",
            scannableItems = emptyList(),
            onDismiss = { showScanDialogForRef = false },
            onScanResult = { result ->
                ref = result
                showScanDialogForRef = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (article == null || article.id == 0L) "Ajouter un article" else "Modifier l'article", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("Référence (ex: MP-AC-100)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showScanDialogForRef = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner le code-barres",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = des,
                    onValueChange = { des = it },
                    label = { Text("Désignation / Nom de l'article") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        label = { Text("Catégorie") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { catExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { catExpanded = true }
                    )

                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = {
                                selectedCat = cat
                                catExpanded = false
                            })
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité (ex: kg, pcs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("Seuil d'alerte") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (article == null) {
                        OutlinedTextField(
                            value = initialStockStr,
                            onValueChange = { initialStockStr = it },
                            label = { Text("Stock Initial") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Prix Unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdStr.toIntOrNull() ?: 5
                    val initialStock = initialStockStr.toIntOrNull() ?: 0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    if (ref.isNotBlank() && des.isNotBlank()) {
                        onSave(ref, des, selectedCat, unit, threshold, initialStock, price)
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            Row {
                if (article != null) {
                    TextButton(
                        onClick = { onDelete(article) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

@Composable
fun RecordMovementDialog(
    article: Article,
    onDismiss: () -> Unit,
    onSave: (type: String, qty: Int, motif: String) -> Unit
) {
    var typeMouvement by remember { mutableStateOf("Entrée") } // "Entrée" or "Sortie"
    var quantityStr by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("Réception fournisseur") }

    var motifExpanded by remember { mutableStateOf(false) }
    val entryMotifs = listOf("Réception fournisseur", "Retour client", "Ajustement d'inventaire (+)", "Retour de prêt")
    val exitMotifs = listOf("Utilisation interne", "Vente client", "Transfert de stock", "Ajustement d'inventaire (-)", "Avarie / rebut")

    val activeMotifs = if (typeMouvement == "Entrée") entryMotifs else exitMotifs

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mouvement de Stock", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Article: ${article.designation}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Stock actuel: ${article.quantiteStock} ${article.unite}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Segmented button for In / Out
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val entries = listOf("Entrée", "Sortie")
                    entries.forEach { entry ->
                        val isSelected = typeMouvement == entry
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    typeMouvement = entry
                                    motif = if (entry == "Entrée") entryMotifs[0] else exitMotifs[0]
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quantity Text Field
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantité à mouvementer (${article.unite})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Motif Selection dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = motif,
                        onValueChange = { motif = it },
                        label = { Text("Motif / Raison") },
                        trailingIcon = {
                            IconButton(onClick = { motifExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(expanded = motifExpanded, onDismissRequest = { motifExpanded = false }) {
                        activeMotifs.forEach { mot ->
                            DropdownMenuItem(text = { Text(mot) }, onClick = {
                                motif = mot
                                motifExpanded = false
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toIntOrNull() ?: 0
                    if (qty > 0 && motif.isNotBlank()) {
                        onSave(typeMouvement, qty, motif)
                    }
                }
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

fun formatMvtDate(timeMs: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMs))
}
