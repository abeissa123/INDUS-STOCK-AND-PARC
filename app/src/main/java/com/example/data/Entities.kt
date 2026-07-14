package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utilisateurs")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val email: String,
    val motDePasse: String,
    val role: String // "Administrateur", "Gestionnaire de stock", "Technicien informatique", "Consultation seule"
)

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val designation: String,
    val categorie: String, // "Matière première", "Produit fini", "Pièce de rechange", "Consommable"
    val unite: String, // "kg", "pièce", "litre", etc.
    val seuilAlerte: Int,
    val quantiteStock: Int = 0,
    val prixUnitaire: Double
)

@Entity(tableName = "mouvements_stock")
data class MouvementStock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long,
    val articleDesignation: String,
    val typeMouvement: String, // "Entrée" ou "Sortie"
    val quantite: Int,
    val date: Long = System.currentTimeMillis(),
    val utilisateurNom: String,
    val motif: String
)

@Entity(tableName = "fournisseurs")
data class Fournisseur(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val contact: String,
    val telephone: String,
    val email: String,
    val adresse: String
)

@Entity(tableName = "equipements")
data class Equipement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "Ordinateur portable", "Ordinateur de bureau", "Serveur", "Imprimante", "Routeur", etc.
    val marque: String,
    val modele: String,
    val numeroSerie: String,
    val dateAchat: String, // format YYYY-MM-DD
    val valeurAchat: Double = 0.0,
    val statut: String, // "En service", "En panne", "En réparation", "Hors service"
    val localisation: String,
    val utilisateurAffecte: String // Employé ou service
)

@Entity(tableName = "licences")
data class Licence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomLogiciel: String,
    val equipementId: Long?,
    val equipementNom: String?,
    val dateAchat: String, // YYYY-MM-DD
    val dateExpiration: String, // YYYY-MM-DD
    val nombreUtilisateurs: Int,
    val typeLicence: String // "Annuelle", "Permanente", etc.
)

@Entity(tableName = "interventions")
data class Intervention(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipementId: Long,
    val equipementNom: String,
    val date: String, // YYYY-MM-DD
    val description: String,
    val technicienNom: String,
    val cout: Double = 0.0
)

@Entity(tableName = "journal_actions")
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val utilisateurNom: String,
    val action: String,
    val date: Long = System.currentTimeMillis()
)
