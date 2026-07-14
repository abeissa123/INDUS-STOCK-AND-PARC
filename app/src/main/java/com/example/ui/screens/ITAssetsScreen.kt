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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter Matériel", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    1 -> {
                        Button(
                            onClick = { showLicenceDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nouvelle Licence", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    2 -> {
                        Button(
                            onClick = {
                                selectedEquipementForIntervention = null
                                showInterventionDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saisir Interv.", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                if (selectedEquipementForEdit == null) {
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
}

@Composable
fun EquipementsTab(
    viewModel: MainViewModel,
    canModify: Boolean,
    onEditEquipement: (Equipement) -> Unit,
    onAddIntervention: (Equipement) -> Unit
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
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusFilter)
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
                        onAddIntervention = { onAddIntervention(eq) }
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
                viewModel.equipmentSearchQuery.value = result
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
    onAddIntervention: () -> Unit
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

                if (canModify) {
                    IconButton(onClick = onEditEquipement) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, size = 16.dp, tint = Color.Gray)
                    Text(
                        text = "${eq.localisation} (${eq.utilisateurAffecte})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Créer une intervention de réparation", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (equipement == null) "Ajouter un Équipement" else "Modifier Équipement", fontWeight = FontWeight.Bold) },
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
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            Row {
                if (equipement != null) {
                    TextButton(
                        onClick = { onDelete(equipement) },
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
                }
            ) {
                Text("Ajouter")
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
                }
            ) {
                Text("Valider l'Intervention")
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
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
}
