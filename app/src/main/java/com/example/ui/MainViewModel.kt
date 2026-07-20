package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val id: String, val title: String) {
    object Login : Screen("login", "Connexion")
    object Dashboard : Screen("dashboard", "Tableau de Bord")
    object Stocks : Screen("stocks", "Gestion des Stocks")
    object ITAssets : Screen("it_assets", "Parc Informatique")
    object Licences : Screen("licences", "Licences & Logiciels")
    object Interventions : Screen("interventions", "Interventions")
    object Fournisseurs : Screen("fournisseurs", "Fournisseurs")
    object AuditLogs : Screen("audit_logs", "Journal d'Actions")
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // Authentication State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Core Data Flows from Repository
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allArticles = repository.allArticles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMouvements = repository.allMouvements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFournisseurs = repository.allFournisseurs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEquipements = repository.allEquipements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLicences = repository.allLicences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allInterventions = repository.allInterventions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLogs = repository.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter states for lists
    val articleSearchQuery = MutableStateFlow("")
    val articleCategoryFilter = MutableStateFlow("Tous")

    val equipmentSearchQuery = MutableStateFlow("")
    val equipmentStatusFilter = MutableStateFlow("Tous")

    val supplierSearchQuery = MutableStateFlow("")
    val logSearchQuery = MutableStateFlow("")

    // Login Method
    fun login(email: String, pssw: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim())
            if (user != null && user.motDePasse == pssw) {
                _currentUser.value = user
                _currentScreen.value = Screen.Dashboard
                repository.insertLog(ActionLog(
                    utilisateurNom = user.nom,
                    action = "Connexion réussie (Rôle: ${user.role})"
                ))
                onResult(true, null)
            } else {
                onResult(false, "Identifiants incorrects.")
            }
        }
    }

    // Direct Login for testing/roles
    fun forceLogin(user: User) {
        viewModelScope.launch {
            _currentUser.value = user
            _currentScreen.value = Screen.Dashboard
            repository.insertLog(ActionLog(
                utilisateurNom = user.nom,
                action = "Connexion forcée (Simulation Rôle: ${user.role})"
            ))
        }
    }

    fun logout() {
        viewModelScope.launch {
            val current = _currentUser.value
            if (current != null) {
                repository.insertLog(ActionLog(
                    utilisateurNom = current.nom,
                    action = "Déconnexion de la session"
                ))
            }
            _currentUser.value = null
            _currentScreen.value = Screen.Login
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Module 1 — Operations: Stocks ---
    fun addArticle(reference: String, designation: String, category: String, unit: String, alertThreshold: Int, initialStock: Int, unitPrice: Double) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            val newArticle = Article(
                reference = reference.trim(),
                designation = designation.trim(),
                categorie = category,
                unite = unit.trim(),
                seuilAlerte = alertThreshold,
                quantiteStock = initialStock,
                prixUnitaire = unitPrice
            )
            val articleId = repository.insertArticle(newArticle)
            
            // Log initial stock movement if it's > 0
            if (initialStock > 0) {
                repository.insertMouvement(MouvementStock(
                    articleId = articleId,
                    articleDesignation = designation.trim(),
                    typeMouvement = "Entrée",
                    quantite = initialStock,
                    utilisateurNom = opName,
                    motif = "Stock initial de création"
                ), initialStock)
            } else {
                repository.insertLog(ActionLog(
                    utilisateurNom = opName,
                    action = "Création de l'article ${designation.trim()} (Réf: ${reference.trim()})"
                ))
            }
        }
    }

    fun updateArticle(article: Article) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.insertArticle(article)
            repository.insertLog(ActionLog(
                utilisateurNom = opName,
                action = "Mise à jour de l'article: ${article.designation} (Réf: ${article.reference})"
            ))
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.deleteArticle(article)
            repository.insertLog(ActionLog(
                utilisateurNom = opName,
                action = "Suppression de l'article: ${article.designation}"
            ))
        }
    }

    fun recordStockMovement(articleId: Long, designation: String, typeMouvement: String, quantite: Int, motif: String, currentQty: Int) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            val finalQty = if (typeMouvement == "Entrée") {
                currentQty + quantite
            } else {
                (currentQty - quantite).coerceAtLeast(0)
            }
            
            val mvt = MouvementStock(
                articleId = articleId,
                articleDesignation = designation,
                typeMouvement = typeMouvement,
                quantite = quantite,
                utilisateurNom = opName,
                motif = motif.trim()
            )
            repository.insertMouvement(mvt, finalQty)
        }
    }

    // --- Module 1 — Operations: Fournisseurs ---
    fun addFournisseur(nom: String, contact: String, telephone: String, email: String, adresse: String) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            val f = Fournisseur(
                nom = nom.trim(),
                contact = contact.trim(),
                telephone = telephone.trim(),
                email = email.trim(),
                adresse = adresse.trim()
            )
            repository.insertFournisseur(f)
            repository.insertLog(ActionLog(
                utilisateurNom = opName,
                action = "Création du fournisseur: ${nom.trim()}"
            ))
        }
    }

    fun deleteFournisseur(fournisseur: Fournisseur) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.deleteFournisseur(fournisseur)
            repository.insertLog(ActionLog(
                utilisateurNom = opName,
                action = "Suppression du fournisseur: ${fournisseur.nom}"
            ))
        }
    }


    // --- Module 2 — Operations: Equipements ---
    fun addEquipement(type: String, marque: String, modele: String, numeroSerie: String, dateAchat: String, valeurAchat: Double, statut: String, localisation: String, utilisateurAffecte: String) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            val eq = Equipement(
                type = type,
                marque = marque.trim(),
                modele = modele.trim(),
                numeroSerie = numeroSerie.trim(),
                dateAchat = dateAchat,
                valeurAchat = valeurAchat,
                statut = statut,
                localisation = localisation.trim(),
                utilisateurAffecte = utilisateurAffecte.trim()
            )
            repository.insertEquipement(eq, opName)
        }
    }

    fun updateEquipement(equipement: Equipement) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.insertEquipement(equipement, opName)
        }
    }

    fun deleteEquipement(equipement: Equipement) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.deleteEquipement(equipement, opName)
        }
    }


    // --- Module 2 — Operations: Licences ---
    fun addLicence(nomLogiciel: String, equipementId: Long?, equipementNom: String?, dateAchat: String, dateExpiration: String, nombreUtilisateurs: Int, typeLicence: String) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            val lic = Licence(
                nomLogiciel = nomLogiciel.trim(),
                equipementId = equipementId,
                equipementNom = equipementNom,
                dateAchat = dateAchat,
                dateExpiration = dateExpiration,
                nombreUtilisateurs = nombreUtilisateurs,
                typeLicence = typeLicence
            )
            repository.insertLicence(lic, opName)
        }
    }

    fun deleteLicence(licence: Licence) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.deleteLicence(licence, opName)
        }
    }


    // --- Module 2 — Operations: Interventions ---
    fun addIntervention(equipementId: Long, equipementNom: String, date: String, description: String, technicienNom: String, cout: Double) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            val inter = Intervention(
                equipementId = equipementId,
                equipementNom = equipementNom,
                date = date,
                description = description.trim(),
                technicienNom = technicienNom.trim(),
                cout = cout
            )
            repository.insertIntervention(inter, opName)
        }
    }

    fun deleteIntervention(intervention: Intervention) {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.deleteIntervention(intervention, opName)
        }
    }


    // --- Export Reports Utilities ---
    fun exportStocksReport(context: Context) {
        val articles = allArticles.value
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val stringBuilder = java.lang.StringBuilder()
        stringBuilder.append("RAPPORT D'ÉTAT DES STOCKS\n")
        stringBuilder.append("Généré le: $dateStr par ${currentUser.value?.nom ?: "Utilisateur"}\n")
        stringBuilder.append("=========================================\n\n")

        var totalValue = 0.0
        var lowStockCount = 0

        stringBuilder.append(String.format("%-12s | %-30s | %-15s | %-10s | %-12s | %-12s\n", "Référence", "Désignation", "Catégorie", "Stock", "Prix Unit.", "Val. Totale"))
        stringBuilder.append("-------------------------------------------------------------------------------------------------------------\n")

        for (art in articles) {
            val value = art.quantiteStock * art.prixUnitaire
            totalValue += value
            val isLow = art.quantiteStock <= art.seuilAlerte
            val alertMarker = if (isLow) " [ALERTE]" else ""
            if (isLow) lowStockCount++

            stringBuilder.append(String.format("%-12s | %-30s | %-15s | %-10s | %-12.0f F | %-12.0f F%s\n",
                art.reference,
                if (art.designation.length > 28) art.designation.substring(0, 25) + "..." else art.designation,
                art.categorie,
                "${art.quantiteStock} ${art.unite}",
                art.prixUnitaire,
                value,
                alertMarker
            ))
        }

        stringBuilder.append("\n=========================================\n")
        stringBuilder.append("RÉSUMÉ DU STOCK:\n")
        stringBuilder.append("Nombre d'articles total: ${articles.size}\n")
        stringBuilder.append("Nombre d'articles en alerte critique: $lowStockCount\n")
        stringBuilder.append(String.format("Valeur financière globale: %,.0f FCFA\n", totalValue))

        shareReport(context, "Rapport_Stocks_${System.currentTimeMillis()}.pdf", stringBuilder.toString())
    }

    fun exportITReport(context: Context) {
        val assets = allEquipements.value
        val licenses = allLicences.value
        val interventions = allInterventions.value
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val stringBuilder = java.lang.StringBuilder()
        stringBuilder.append("RAPPORT DE GESTION DU PARC INFORMATIQUE\n")
        stringBuilder.append("Généré le: $dateStr par ${currentUser.value?.nom ?: "Utilisateur"}\n")
        stringBuilder.append("=========================================\n\n")

        stringBuilder.append("1. INVENTAIRE MATÉRIEL\n")
        stringBuilder.append("-----------------------------------------\n")
        stringBuilder.append(String.format("%-20s | %-15s | %-15s | %-15s | %-12s\n", "Type", "Marque/Modèle", "Numéro de Série", "Localisation", "Statut"))
        stringBuilder.append("-------------------------------------------------------------------------------------------------\n")

        var totalPurchaseCost = 0.0
        val statusCount = mutableMapOf<String, Int>()

        for (eq in assets) {
            totalPurchaseCost += eq.valeurAchat
            statusCount[eq.statut] = (statusCount[eq.statut] ?: 0) + 1
            stringBuilder.append(String.format("%-20s | %-15s | %-15s | %-15s | %-12s\n",
                eq.type,
                "${eq.marque} ${eq.modele}",
                eq.numeroSerie,
                eq.localisation,
                eq.statut
            ))
        }

        stringBuilder.append("\n2. SYNTHÈSE DES LOGICIELS & LICENCES\n")
        stringBuilder.append("-----------------------------------------\n")
        stringBuilder.append(String.format("%-25s | %-30s | %-12s\n", "Logiciel", "Poste Affecté", "Expiration"))
        stringBuilder.append("-----------------------------------------------------------------------------------\n")
        for (lic in licenses) {
            stringBuilder.append(String.format("%-25s | %-30s | %-12s\n",
                lic.nomLogiciel,
                lic.equipementNom ?: "Licence Volume",
                lic.dateExpiration
            ))
        }

        var totalMaintenanceCost = 0.0
        stringBuilder.append("\n3. DERNIÈRES INTERVENTIONS DE MAINTENANCE\n")
        stringBuilder.append("-----------------------------------------\n")
        stringBuilder.append(String.format("%-12s | %-25s | %-15s | %-10s | %-15s\n", "Date", "Équipement", "Technicien", "Coût", "Description"))
        stringBuilder.append("-------------------------------------------------------------------------------------------------\n")
        for (itv in interventions) {
            totalMaintenanceCost += itv.cout
            stringBuilder.append(String.format("%-12s | %-25s | %-15s | %-10.0f F | %-15s\n",
                itv.date,
                if (itv.equipementNom.length > 23) itv.equipementNom.substring(0, 20) + "..." else itv.equipementNom,
                itv.technicienNom,
                itv.cout,
                if (itv.description.length > 20) itv.description.substring(0, 17) + "..." else itv.description
            ))
        }

        stringBuilder.append("\n=========================================\n")
        stringBuilder.append("INDIFICATEURS CLÉS DU PARC:\n")
        stringBuilder.append("Nombre d'équipements total: ${assets.size}\n")
        statusCount.forEach { (statut, count) ->
            stringBuilder.append("- Statut \"$statut\": $count\n")
        }
        stringBuilder.append(String.format("Valeur d'acquisition totale: %,.0f FCFA\n", totalPurchaseCost))
        stringBuilder.append(String.format("Coût global de maintenance (interventions): %,.0f FCFA\n", totalMaintenanceCost))

        shareReport(context, "Rapport_Parc_Informatique_${System.currentTimeMillis()}.pdf", stringBuilder.toString())
    }

    private fun shareReport(context: Context, filename: String, textContent: String) {
        try {
            val reportTitle = if (filename.contains("Stocks")) "Rapport d'État des Stocks" else "Rapport du Parc Informatique"
            val pdfFile = PdfExporter.writeTextToPdf(context, filename, reportTitle, textContent)
            
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context, 
                "${context.packageName}.fileprovider", 
                pdfFile
            )
            
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Exporter le rapport PDF via :")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text sharing in case of an issue
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, filename.replace(".pdf", ""))
                putExtra(Intent.EXTRA_SUBJECT, filename.replace(".pdf", "").replace("_", " "))
                putExtra(Intent.EXTRA_TEXT, textContent)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Exporter le rapport via :")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            val opName = _currentUser.value?.nom ?: "Système"
            repository.deleteAllLogs()
            repository.insertLog(ActionLog(
                utilisateurNom = opName,
                action = "Le journal d'actions a été vidé par l'utilisateur."
            ))
        }
    }
}

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
