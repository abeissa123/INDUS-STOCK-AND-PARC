package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.data.Equipement
import com.example.data.Intervention
import com.example.data.Licence
import com.example.ui.MainViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ScannableItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ITAssetsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Équipements", "Licences & Logiciels", "Maintenance")

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userRole = currentUser?.role ?: "Consultation seule"
    val canModify = userRole == "Administrateur" || userRole == "Technicien informatique"

    // Dialog flags
    var showEquipementDialog by remember { mutableStateOf(false) }
    var selectedEquipementForEdit by remember { mutableStateOf<Equipement?>(null) }
    var showLicenceDialog by remember { mutableStateOf(false) }
    var showInterventionDialog by remember { mutableStateOf(false) }
    var selectedEquipementForIntervention by remember { mutableStateOf<Equipement?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var selectedEquipementForQr by remember { mutableStateOf<Equipement?>(null) }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Module title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Parc Informatique",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Suivi des matériels, licences et réparations",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (canModify) {
                // Actions depending on current sub-tab
                when (selectedTabIndex) {
                    0 -> {
                        Button(
                            onClick = {
                                selectedEquipementForEdit = null
                                showEquipementDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter Matériel", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    }
                    1 -> {
                        Button(
                            onClick = { showLicenceDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nouvelle Licence", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    }
                    2 -> {
                        Button(
                            onClick = {
                                selectedEquipementForIntervention = null
                                showInterventionDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saisir Interv.", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    }
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
            0 -> EquipementsTab(
                viewModel = viewModel,
                canModify = canModify,
                onEditEquipement = { eq ->
                    selectedEquipementForEdit = eq
                    showEquipementDialog = true
                },
                onAddIntervention = { eq ->
                    selectedEquipementForIntervention = eq
                    showInterventionDialog = true
                },
                onShowQrCode = { eq ->
                    selectedEquipementForQr = eq
                    showQrDialog = true
                }
            )
            1 -> LicencesTab(viewModel = viewModel, canModify = canModify)
            2 -> InterventionsTab(viewModel = viewModel, canModify = canModify)
        }
    }

    // --- Dialog: Add/Edit Equipement ---
    if (showEquipementDialog) {
        AddEditEquipementDialog(
            equipement = selectedEquipementForEdit,
            onDismiss = { showEquipementDialog = false },
            onSave = { type, brand, model, serial, date, value, status, location, user ->
                val isNew = selectedEquipementForEdit == null || selectedEquipementForEdit!!.id == 0L
                if (isNew) {
                    viewModel.addEquipement(type, brand, model, serial, date, value, status, location, user)
                } else {
                    viewModel.updateEquipement(
                        selectedEquipementForEdit!!.copy(
                            type = type,
                            marque = brand,
                            modele = model,
                            numeroSerie = serial,
                            dateAchat = date,
                            valeurAchat = value,
                            statut = status,
                            localisation = location,
                            utilisateurAffecte = user
                        )
                    )
                }
                showEquipementDialog = false

                // If asset is assigned to a personnel, automatically pop the QR code dialog
                if (user.trim().isNotBlank()) {
                    selectedEquipementForQr = Equipement(
                        id = selectedEquipementForEdit?.id ?: 0L,
                        type = type,
                        marque = brand,
                        modele = model,
                        numeroSerie = serial,
                        dateAchat = date,
                        valeurAchat = value,
                        statut = status,
                        localisation = location,
                        utilisateurAffecte = user
                    )
                    showQrDialog = true
                }
            },
            onDelete = { eq ->
                viewModel.deleteEquipement(eq)
                showEquipementDialog = false
            }
        )
    }

    // --- Dialog: Add Licence ---
    if (showLicenceDialog) {
        val equipements by viewModel.allEquipements.collectAsStateWithLifecycle()
        AddLicenceDialog(
            equipements = equipements,
            onDismiss = { showLicenceDialog = false },
            onSave = { name, eqId, eqName, dateAchat, dateExp, usersCount, type ->
                viewModel.addLicence(name, eqId, eqName, dateAchat, dateExp, usersCount, type)
                showLicenceDialog = false
            }
        )
    }

    // --- Dialog: Add Intervention ---
    if (showInterventionDialog) {
        val equipements by viewModel.allEquipements.collectAsStateWithLifecycle()
        AddInterventionDialog(
            equipements = equipements,
            preSelectedEquipement = selectedEquipementForIntervention,
            onDismiss = { showInterventionDialog = false },
            onSave = { eqId, eqName, date, desc, tech, cost, updateStatus ->
                viewModel.addIntervention(eqId, eqName, date, desc, tech, cost)
                // If requested, update equipement status to "En service" or "En réparation"
                if (updateStatus != null) {
                    val targetEq = equipements.find { it.id == eqId }
                    if (targetEq != null) {
                        viewModel.updateEquipement(targetEq.copy(statut = updateStatus))
                    }
                }
                showInterventionDialog = false
            }
        )
    }

    // --- Dialog: QR Code tracking of asset ---
    if (showQrDialog && selectedEquipementForQr != null) {
        EquipementAssignmentQrDialog(
            eq = selectedEquipementForQr!!,
            onDismiss = { showQrDialog = false }
        )
    }

}

@Composable
fun EquipementsTab(
    viewModel: MainViewModel,
    canModify: Boolean,
    onEditEquipement: (Equipement) -> Unit,
    onAddIntervention: (Equipement) -> Unit,
    onShowQrCode: (Equipement) -> Unit
) {
    val equipements by viewModel.allEquipements.collectAsStateWithLifecycle()
    val searchQuery by viewModel.equipmentSearchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.equipmentStatusFilter.collectAsStateWithLifecycle()

    var showScanner by remember { mutableStateOf(false) }

    val filteredEquipements = remember(equipements, searchQuery, statusFilter) {
        equipements.filter { eq ->
            val matchesSearch = eq.marque.contains(searchQuery, ignoreCase = true) ||
                    eq.modele.contains(searchQuery, ignoreCase = true) ||
                    eq.numeroSerie.contains(searchQuery, ignoreCase = true) ||
                    eq.type.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "Tous" || eq.statut == statusFilter
            matchesSearch && matchesStatus
        }
    }

    val scannableItems = remember(equipements) {
        equipements.map { ScannableItem(it.numeroSerie, "${it.type} ${it.marque} ${it.modele}", "S/N: ${it.numeroSerie}") }
    }

    // Check if searchQuery matches exactly a single device S/N to show identification/actions HUD banner
    val exactMatchedEquipement = remember(equipements, searchQuery) {
        if (searchQuery.isNotBlank()) {
            equipements.find { it.numeroSerie.equals(searchQuery.trim(), ignoreCase = true) }
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
        // Search & Status filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.equipmentSearchQuery.value = it },
                label = { Text("Marque, modèle, S/N...") },
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

            var expanded by remember { mutableStateOf(false) }
            val statuses = listOf("Tous", "En service", "En panne", "En réparation", "Hors service")

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusFilter, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    statuses.forEach { stat ->
                        DropdownMenuItem(
                            text = { Text(stat) },
                            onClick = {
                                viewModel.equipmentStatusFilter.value = stat
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Camera identification banner or Unrecognized serial number banner
        if (exactMatchedEquipement != null) {
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
                                text = "Matériel Identifié par Scan !",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${exactMatchedEquipement.type} ${exactMatchedEquipement.marque} ${exactMatchedEquipement.modele}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "S/N: ${exactMatchedEquipement.numeroSerie} • Statut: ${exactMatchedEquipement.statut}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onEditEquipement(exactMatchedEquipement) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modifier/Voir", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(
                            onClick = { viewModel.equipmentSearchQuery.value = "" },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        } else if (searchQuery.isNotBlank() && filteredEquipements.isEmpty()) {
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
                            text = "S/N inconnu : $searchQuery",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "Aucun équipement matériel de votre parc IT ne correspond à ce code-barres / S/N. Souhaitez-vous l'ajouter au parc ?",
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
                                    // Open Add Equipment Dialog with prefilled scanned S/N
                                    onEditEquipement(Equipement(
                                        id = 0,
                                        type = "Ordinateur portable",
                                        marque = "",
                                        modele = "",
                                        numeroSerie = searchQuery.trim(),
                                        dateAchat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                        valeurAchat = 0.0,
                                        statut = "En service",
                                        localisation = "",
                                        utilisateurAffecte = ""
                                    ))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter au parc", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.equipmentSearchQuery.value = "" },
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

        if (filteredEquipements.isEmpty()) {
            EmptyStateView(
                title = "Aucun matériel trouvé",
                description = "Modifiez vos filtres ou ajoutez une nouvelle fiche équipement.",
                icon = Icons.Default.Devices
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredEquipements) { eq ->
                    EquipementItemCard(
                        eq = eq,
                        canModify = canModify,
                        onEditEquipement = { onEditEquipement(eq) },
                        onAddIntervention = { onAddIntervention(eq) },
                        onShowQrCode = { onShowQrCode(eq) }
                    )
                }
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            title = "Scanner d'Équipements & Parc IT",
            subtitle = "Pointez le code-barres / S/N ou sélectionnez un équipement ci-dessous",
            scannableItems = scannableItems,
            onDismiss = { showScanner = false },
            onScanResult = { result ->
                val cleanResult = if (result.startsWith("TRACK_IT_ASSET:")) {
                    result.removePrefix("TRACK_IT_ASSET:")
                } else {
                    result
                }
                viewModel.equipmentSearchQuery.value = cleanResult
                showScanner = false
            }
        )
    }
}

@Composable
fun EquipementItemCard(
    eq: Equipement,
    canModify: Boolean,
    onEditEquipement: () -> Unit,
    onAddIntervention: () -> Unit,
    onShowQrCode: () -> Unit
) {
    val statusColor = when (eq.statut) {
        "En service" -> Color(0xFF2E7D32)
        "En panne" -> Color(0xFFC62828)
        "En réparation" -> Color(0xFFE65100)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${eq.type} ${eq.marque} ${eq.modele}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "S/N: ${eq.numeroSerie} • Achat: ${eq.dateAchat} (${String.format("%,.0f", eq.valeurAchat)} F CFA)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onShowQrCode) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Code QR de suivi",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (canModify) {
                        IconButton(onClick = onEditEquipement) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location & Owner
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Text(
                            text = eq.localisation.ifBlank { "Sans localisation" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (eq.utilisateurAffecte.isNotBlank()) Icons.Default.Person else Icons.Default.PersonOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (eq.utilisateurAffecte.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            text = if (eq.utilisateurAffecte.isNotBlank()) {
                                "Affecté à : ${eq.utilisateurAffecte}"
                            } else {
                                "Non affecté (En stock)"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (eq.utilisateurAffecte.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (eq.utilisateurAffecte.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = eq.statut.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }


            if (canModify && (eq.statut == "En panne" || eq.statut == "En réparation")) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAddIntervention,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Créer une intervention de réparation", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                }
            }
        }
    }
}

@Composable
fun LicencesTab(viewModel: MainViewModel, canModify: Boolean) {
    val licences by viewModel.allLicences.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Licences logicielles globales et par poste",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Gray
        )

        if (licences.isEmpty()) {
            EmptyStateView(
                title = "Aucune licence enregistrée",
                description = "Ajoutez des clés de licences, abonnements SaaS ou volumes d'entreprise.",
                icon = Icons.Default.VpnKey
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(licences) { lic ->
                    LicenceItemCard(lic = lic, canModify = canModify, onDelete = { viewModel.deleteLicence(lic) })
                }
            }
        }
    }
}

@Composable
fun LicenceItemCard(lic: Licence, canModify: Boolean, onDelete: () -> Unit) {
    // Check if license is expired (simulated dates check)
    val isExpired = remember(lic.dateExpiration) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expDate = sdf.parse(lic.dateExpiration)
            expDate != null && expDate.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = lic.nomLogiciel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (canModify) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Affectation: ${lic.equipementNom ?: "Licence Volume d'Entreprise"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Expiration: ${lic.dateExpiration} (${lic.typeLicence})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isExpired) MaterialTheme.colorScheme.error else Color.DarkGray
                    )
                }

                Surface(
                    color = if (isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isExpired) "EXPIRED" else "${lic.nombreUtilisateurs} POSTES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InterventionsTab(viewModel: MainViewModel, canModify: Boolean) {
    val interventions by viewModel.allInterventions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historique des pannes et résolutions de maintenance",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Gray
        )

        if (interventions.isEmpty()) {
            EmptyStateView(
                title = "Aucune intervention de maintenance",
                description = "Les interventions de dépannage saisies apparaîtront dans cette liste.",
                icon = Icons.Default.Build
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(interventions) { itv ->
                    InterventionItemCard(itv = itv, canModify = canModify, onDelete = { viewModel.deleteIntervention(itv) })
                }
            }
        }
    }
}

@Composable
fun InterventionItemCard(itv: Intervention, canModify: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Équipement: ${itv.equipementNom}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Technicien: ${itv.technicienNom} • Le ${itv.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%,.0f F CFA", itv.cout),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                    if (canModify) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = itv.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun AddEditEquipementDialog(
    equipement: Equipement?,
    onDismiss: () -> Unit,
    onSave: (type: String, brand: String, model: String, serial: String, date: String, value: Double, status: String, location: String, user: String) -> Unit,
    onDelete: (Equipement) -> Unit
) {
    var type by remember { mutableStateOf(equipement?.type ?: "Ordinateur portable") }
    var brand by remember { mutableStateOf(equipement?.marque ?: "") }
    var model by remember { mutableStateOf(equipement?.modele ?: "") }
    var serial by remember { mutableStateOf(equipement?.numeroSerie ?: "") }
    var date by remember { mutableStateOf(equipement?.dateAchat ?: "2026-07-14") }
    var valueStr by remember { mutableStateOf(equipement?.valeurAchat?.toString() ?: "") }
    var status by remember { mutableStateOf(equipement?.statut ?: "En service") }
    var location by remember { mutableStateOf(equipement?.localisation ?: "") }
    var user by remember { mutableStateOf(equipement?.utilisateurAffecte ?: "") }

    var typeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val typesList = listOf("Ordinateur portable", "Ordinateur de bureau", "Serveur", "Imprimante", "Routeur", "Switch", "Autre")
    val statusList = listOf("En service", "En panne", "En réparation", "Hors service")

    var showScanDialogForSerial by remember { mutableStateOf(false) }

    if (showScanDialogForSerial) {
        BarcodeScannerDialog(
            title = "Scanner le S/N de l'équipement",
            subtitle = "Pointez la caméra vers le code-barres ou QR code de l'équipement",
            scannableItems = emptyList(),
            onDismiss = { showScanDialogForSerial = false },
            onScanResult = { result ->
                serial = result
                showScanDialogForSerial = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (equipement == null || equipement.id == 0L) "Ajouter un Équipement" else "Modifier Équipement", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("Type d'équipement") },
                        readOnly = true,
                        trailingIcon = { IconButton(onClick = { typeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true }
                    )
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        typesList.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                type = t
                                typeExpanded = false
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marque (ex: Dell)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modèle") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = serial,
                    onValueChange = { serial = it },
                    label = { Text("Numéro de série / Code S/N") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showScanDialogForSerial = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner le S/N",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date d'achat (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it },
                        label = { Text("Valeur (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Status Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        label = { Text("Statut opérationnel") },
                        readOnly = true,
                        trailingIcon = { IconButton(onClick = { statusExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth().clickable { statusExpanded = true }
                    )
                    DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statusList.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = {
                                status = s
                                statusExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localisation (ex: Salle Serveur)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Affecté à (ex: Thomas Durand)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = valueStr.toDoubleOrNull() ?: 0.0
                    if (brand.isNotBlank() && model.isNotBlank() && serial.isNotBlank()) {
                        onSave(type, brand, model, serial, date, value, status, location, user)
                    }
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enregistrer", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (equipement != null) {
                    TextButton(
                        onClick = { onDelete(equipement) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Supprimer", style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Annuler", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    )
}

@Composable
fun AddLicenceDialog(
    equipements: List<Equipement>,
    onDismiss: () -> Unit,
    onSave: (name: String, eqId: Long?, eqName: String?, dateAchat: String, dateExp: String, count: Int, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEquipementId by remember { mutableStateOf<Long?>(null) }
    var selectedEquipementName by remember { mutableStateOf<String?>(null) }
    var dateAchat by remember { mutableStateOf("2026-07-14") }
    var dateExp by remember { mutableStateOf("2027-07-14") }
    var usersCountStr by remember { mutableStateOf("1") }
    var typeLicence by remember { mutableStateOf("Annuelle") }

    var eqExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val typesList = listOf("Annuelle", "Volume", "Permanente", "Abonnement Mensuel")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer une Licence", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du logiciel (ex: Adobe CC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Equipment link Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedEquipementName ?: "Licence Volume d'Entreprise (Non affectée)",
                        onValueChange = {},
                        label = { Text("Affectation matériel (Optionnel)") },
                        readOnly = true,
                        trailingIcon = { IconButton(onClick = { eqExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth().clickable { eqExpanded = true }
                    )
                    DropdownMenu(expanded = eqExpanded, onDismissRequest = { eqExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Licence Volume d'Entreprise (Non affectée)") },
                            onClick = {
                                selectedEquipementId = null
                                selectedEquipementName = null
                                eqExpanded = false
                            }
                        )
                        equipements.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text("${eq.type} ${eq.marque} (${eq.utilisateurAffecte})") },
                                onClick = {
                                    selectedEquipementId = eq.id
                                    selectedEquipementName = "${eq.type} ${eq.marque} (${eq.utilisateurAffecte})"
                                    eqExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateAchat,
                        onValueChange = { dateAchat = it },
                        label = { Text("Date d'achat") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateExp,
                        onValueChange = { dateExp = it },
                        label = { Text("Date d'expiration") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = usersCountStr,
                        onValueChange = { usersCountStr = it },
                        label = { Text("Nbr postes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )

                    // Type of license dropdown
                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = typeLicence,
                            onValueChange = {},
                            label = { Text("Type licence") },
                            readOnly = true,
                            trailingIcon = { IconButton(onClick = { typeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            modifier = Modifier.fillMaxWidth().clickable { typeExpanded = true }
                        )
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            typesList.forEach { t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = {
                                    typeLicence = t
                                    typeExpanded = false
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = usersCountStr.toIntOrNull() ?: 1
                    if (name.isNotBlank()) {
                        onSave(name, selectedEquipementId, selectedEquipementName, dateAchat, dateExp, count, typeLicence)
                    }
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ajouter", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Annuler", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
fun AddInterventionDialog(
    equipements: List<Equipement>,
    preSelectedEquipement: Equipement?,
    onDismiss: () -> Unit,
    onSave: (eqId: Long, eqName: String, date: String, desc: String, tech: String, cost: Double, updateStatus: String?) -> Unit
) {
    var selectedEq by remember { mutableStateOf(preSelectedEquipement ?: equipements.firstOrNull()) }
    var date by remember { mutableStateOf("2026-07-14") }
    var description by remember { mutableStateOf("") }
    var techNom by remember { mutableStateOf("Luc Tech") }
    var costStr by remember { mutableStateOf("") }
    var updateEqStatusToActive by remember { mutableStateOf(true) } // default check to reset PC to "En service" after intervention

    var eqExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer une Intervention", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Equipment Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedEq?.let { "${it.type} ${it.marque} (${it.localisation})" } ?: "Aucun équipement disponible",
                        onValueChange = {},
                        label = { Text("Sélectionner l'équipement") },
                        readOnly = true,
                        trailingIcon = { IconButton(onClick = { eqExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth().clickable { eqExpanded = true }
                    )
                    DropdownMenu(expanded = eqExpanded, onDismissRequest = { eqExpanded = false }) {
                        equipements.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text("${eq.type} ${eq.marque} (${eq.localisation})") },
                                onClick = {
                                    selectedEq = eq
                                    eqExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date intervention") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Coût (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = techNom,
                    onValueChange = { techNom = it },
                    label = { Text("Technicien en charge") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description des travaux effectués") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = updateEqStatusToActive,
                        onCheckedChange = { updateEqStatusToActive = it }
                    )
                    Text(
                        text = "Remettre le matériel en statut \"EN SERVICE\"",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    if (selectedEq != null && description.isNotBlank()) {
                        val statusUpdate = if (updateEqStatusToActive) "En service" else null
                        onSave(
                            selectedEq!!.id,
                            "${selectedEq!!.type} ${selectedEq!!.marque} (${selectedEq!!.utilisateurAffecte})",
                            date,
                            description,
                            techNom,
                            cost,
                            statusUpdate
                        )
                    }
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Valider l'Intervention", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Annuler", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun EquipementAssignmentQrDialog(
    eq: Equipement,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val qrContent = "TRACK_IT_ASSET:${eq.numeroSerie}"
    val qrBitmap = remember(eq) { generateQrCode(qrContent) }
    
    // State to toggle between QR Code only and full asset label preview
    var showStickerPreview by remember { mutableStateOf(false) }
    
    // Remember the sticker bitmap so we only generate it once
    val stickerBitmap = remember(eq, qrBitmap) {
        if (qrBitmap != null) {
            generateStickerBitmap(eq, qrBitmap)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Suivi & Code QR d'Affectation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ce code QR permet de faire le suivi de l'équipement durant son utilisation. Vous pouvez l'imprimer, l'enregistrer ou le partager pour le coller sur le matériel.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                // Segmented toggle to switch preview modes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val activeColor = MaterialTheme.colorScheme.primaryContainer
                    val inactiveColor = Color.Transparent
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!showStickerPreview) activeColor else inactiveColor)
                            .clickable { showStickerPreview = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Code QR Seul",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (!showStickerPreview) FontWeight.Bold else FontWeight.Normal,
                                color = if (!showStickerPreview) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showStickerPreview) activeColor else inactiveColor)
                            .clickable { showStickerPreview = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Étiquette Complète",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (showStickerPreview) FontWeight.Bold else FontWeight.Normal,
                                color = if (showStickerPreview) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Render the selected preview (QR bitmap or complete sticker)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (showStickerPreview) {
                        if (stickerBitmap != null) {
                            Image(
                                bitmap = stickerBitmap.asImageBitmap(),
                                contentDescription = "Aperçu de l'étiquette d'équipement complète",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code de suivi de l'équipement",
                                modifier = Modifier.size(160.dp)
                            )
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Save to Gallery
                    Button(
                        onClick = {
                            if (showStickerPreview && stickerBitmap != null) {
                                saveQrCodeToGallery(context, eq, stickerBitmap, isSticker = true)
                            } else if (qrBitmap != null) {
                                saveQrCodeToGallery(context, eq, qrBitmap, isSticker = false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Enregistrer",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1
                        )
                    }

                    // Share
                    Button(
                        onClick = {
                            if (showStickerPreview && stickerBitmap != null) {
                                shareQrCode(context, eq, stickerBitmap, isSticker = true)
                            } else if (qrBitmap != null) {
                                shareQrCode(context, eq, qrBitmap, isSticker = false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Partager",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1
                        )
                    }

                    // Print
                    Button(
                        onClick = {
                            if (showStickerPreview && stickerBitmap != null) {
                                printQrCode(context, eq, stickerBitmap, isSticker = true)
                            } else if (qrBitmap != null) {
                                printQrCode(context, eq, qrBitmap, isSticker = false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Imprimer",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1
                        )
                    }
                }

                // Assignment details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Matériel : ${eq.type} ${eq.marque} ${eq.modele}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "S/N : ${eq.numeroSerie}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        Text(
                            text = "Personnel Affecté :",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = if (eq.utilisateurAffecte.isNotBlank()) eq.utilisateurAffecte else "Non affecté (En stock)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (eq.utilisateurAffecte.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        )
                        
                        if (eq.localisation.isNotBlank()) {
                            Text(
                                text = "Localisation : ${eq.localisation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fermer", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * Generates a beautiful sticker layout combining the QR Code with the equipment information text.
 */
fun generateStickerBitmap(eq: Equipement, qrBitmap: android.graphics.Bitmap): android.graphics.Bitmap {
    val stickerWidth = 600
    val stickerHeight = 350
    val bitmap = android.graphics.Bitmap.createBitmap(stickerWidth, stickerHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Fill white background
    val bgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, stickerWidth.toFloat(), stickerHeight.toFloat(), bgPaint)
    
    // Draw outer border
    val borderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRect(8f, 8f, (stickerWidth - 8).toFloat(), (stickerHeight - 8).toFloat(), borderPaint)
    
    // Draw a divider line
    val dividerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 3f
    }
    canvas.drawLine(320f, 20f, 320f, 330f, dividerPaint)
    
    // Draw the QR Code scaled on the left side
    val srcRect = android.graphics.Rect(0, 0, qrBitmap.width, qrBitmap.height)
    val destRect = android.graphics.Rect(20, 35, 300, 315)
    canvas.drawBitmap(qrBitmap, srcRect, destRect, null)
    
    // Draw Text on the right side
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 18f
        isAntiAlias = true
    }
    val boldPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val smallPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 14f
        isAntiAlias = true
    }
    
    var yPos = 50f
    canvas.drawText("SUIVI MATÉRIEL", 340f, yPos, titlePaint)
    
    yPos += 45f
    canvas.drawText("Type : ${eq.type}", 340f, yPos, textPaint)
    
    yPos += 30f
    canvas.drawText("Modèle : ${eq.marque} ${eq.modele}", 340f, yPos, textPaint)
    
    yPos += 30f
    canvas.drawText("S/N : ${eq.numeroSerie}", 340f, yPos, boldPaint)
    
    yPos += 45f
    canvas.drawText("AFFECTÉ À :", 340f, yPos, smallPaint)
    
    yPos += 25f
    val userText = if (eq.utilisateurAffecte.isNotBlank()) eq.utilisateurAffecte else "Non affecté (En stock)"
    canvas.drawText(userText, 340f, yPos, titlePaint.apply { textSize = 20f })
    
    if (eq.localisation.isNotBlank()) {
        yPos += 35f
        canvas.drawText("Loc : ${eq.localisation}", 340f, yPos, textPaint)
    }
    
    return bitmap
}

/**
 * Saves a Bitmap (QR Code or complete label sticker) to the device's public photo gallery.
 */
fun saveQrCodeToGallery(context: android.content.Context, eq: Equipement, bitmap: android.graphics.Bitmap, isSticker: Boolean) {
    try {
        val typeLabel = if (isSticker) "Etiquette" else "QR"
        val filename = "${typeLabel}_Suivi_${eq.numeroSerie}_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ITAssets_QR")
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                    val message = if (isSticker) "Étiquette enregistrée dans l'album 'ITAssets_QR' !" else "Code QR enregistré dans l'album 'ITAssets_QR' !"
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Impossible d'enregistrer l'image", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            android.widget.Toast.makeText(context, "Erreur lors de la création du fichier", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Erreur : ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Shares a Bitmap (QR Code or complete label sticker) using FileProvider.
 */
fun shareQrCode(context: android.content.Context, eq: Equipement, bitmap: android.graphics.Bitmap, isSticker: Boolean) {
    try {
        val cacheDir = context.cacheDir
        val imagesDir = java.io.File(cacheDir, "images").apply { mkdirs() }
        val prefix = if (isSticker) "Etiquette" else "QR"
        val file = java.io.File(imagesDir, "${prefix}_${eq.numeroSerie}.png")
        
        java.io.FileOutputStream(file).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val descriptionText = if (isSticker) {
            "Voici l'étiquette de suivi d'affectation pour ${eq.type} ${eq.marque} (${eq.modele}) affecté à ${eq.utilisateurAffecte}."
        } else {
            "Voici le code QR de suivi d'affectation pour l'équipement ${eq.type} ${eq.marque}."
        }

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Code QR de suivi - ${eq.type} ${eq.marque}")
            putExtra(android.content.Intent.EXTRA_TEXT, descriptionText)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Partager via"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Erreur de partage : ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Directly prints the Bitmap (QR Code or complete label sticker) using Android Print Framework.
 */
fun printQrCode(context: android.content.Context, eq: Equipement, bitmap: android.graphics.Bitmap, isSticker: Boolean) {
    try {
        val printHelper = androidx.print.PrintHelper(context).apply {
            scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        }
        val label = if (isSticker) "Etiquette-Asset-${eq.numeroSerie}" else "QR-Asset-${eq.numeroSerie}"
        printHelper.printBitmap(label, bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Erreur d'impression : ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Generates a QR Code Bitmap using ZXing.
 * Fully qualified names are used for Color and Bitmap to prevent import collision.
 */
fun generateQrCode(content: String, size: Int = 512): android.graphics.Bitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(
                    x, 
                    y, 
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

