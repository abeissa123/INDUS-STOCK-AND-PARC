package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Article::class,
        MouvementStock::class,
        Fournisseur::class,
        Equipement::class,
        Licence::class,
        Intervention::class,
        ActionLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun articleDao(): ArticleDao
    abstract fun mouvementStockDao(): MouvementStockDao
    abstract fun fournisseurDao(): FournisseurDao
    abstract fun equipementDao(): EquipementDao
    abstract fun licenceDao(): LicenceDao
    abstract fun interventionDao(): InterventionDao
    abstract fun actionLogDao(): ActionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestion_entreprise_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // Delete all (just in case)
            // Prep-populate default roles/users
            val userDao = db.userDao()
            val articleDao = db.articleDao()
            val fournisseurDao = db.fournisseurDao()
            val equipementDao = db.equipementDao()
            val licenceDao = db.licenceDao()
            val interventionDao = db.interventionDao()
            val logDao = db.actionLogDao()
            val movementDao = db.mouvementStockDao()

            // 1. Users
            userDao.insertUser(User(nom = "Admin", email = "admin@entreprise.com", motDePasse = "admin123", role = "Administrateur"))
            userDao.insertUser(User(nom = "Jean Stock", email = "stock@entreprise.com", motDePasse = "stock123", role = "Gestionnaire de stock"))
            userDao.insertUser(User(nom = "Luc Tech", email = "tech@entreprise.com", motDePasse = "tech123", role = "Technicien informatique"))
            userDao.insertUser(User(nom = "Sophie Dev", email = "sophie@entreprise.com", motDePasse = "sophie123", role = "Consultation seule"))

            // 2. Suppliers (Fournisseurs)
            val f1Id = fournisseurDao.insertFournisseur(Fournisseur(nom = "Tech Distri", contact = "Pierre Martin", telephone = "01 42 33 55 66", email = "contact@techdistri.fr", adresse = "12 Rue de l'Invention, Paris"))
            val f2Id = fournisseurDao.insertFournisseur(Fournisseur(nom = "Metal & Co", contact = "Sarah Bernard", telephone = "03 88 44 22 11", email = "sales@metalco.fr", adresse = "45 Avenue de l'Industrie, Lyon"))
            val f3Id = fournisseurDao.insertFournisseur(Fournisseur(nom = "Buro Office", contact = "Paul Dupont", telephone = "02 40 11 22 33", email = "paul@burooffice.fr", adresse = "88 Boulevard Circulaire, Nantes"))

            // 3. Articles
            val a1Id = articleDao.insertArticle(Article(reference = "MP-AC-001", designation = "Tôles d'acier galvanisé", categorie = "Matière première", unite = "tonnes", seuilAlerte = 5, quantiteStock = 8, prixUnitaire = 450.0))
            val a2Id = articleDao.insertArticle(Article(reference = "PF-MO-500", designation = "Moteur Électrique 500W", categorie = "Produit fini", unite = "pièces", seuilAlerte = 10, quantiteStock = 25, prixUnitaire = 120.0))
            val a3Id = articleDao.insertArticle(Article(reference = "PR-CO-012", designation = "Courroie de transmission", categorie = "Pièce de rechange", unite = "pièces", seuilAlerte = 20, quantiteStock = 12, prixUnitaire = 15.0)) // Critically low!
            val a4Id = articleDao.insertArticle(Article(reference = "CO-GA-005", designation = "Graisse industrielle haute temp", categorie = "Consommable", unite = "cartouches", seuilAlerte = 15, quantiteStock = 45, prixUnitaire = 8.5))

            // 4. Initial Movements
            movementDao.insertMouvement(MouvementStock(articleId = a1Id, articleDesignation = "Tôles d'acier galvanisé", typeMouvement = "Entrée", quantite = 8, utilisateurNom = "Jean Stock", motif = "Réception fournisseur Tech Distri"))
            movementDao.insertMouvement(MouvementStock(articleId = a2Id, articleDesignation = "Moteur Électrique 500W", typeMouvement = "Entrée", quantite = 30, utilisateurNom = "Jean Stock", motif = "Production interne"))
            movementDao.insertMouvement(MouvementStock(articleId = a2Id, articleDesignation = "Moteur Électrique 500W", typeMouvement = "Sortie", quantite = 5, utilisateurNom = "Jean Stock", motif = "Expédition commande #4092"))
            movementDao.insertMouvement(MouvementStock(articleId = a3Id, articleDesignation = "Courroie de transmission", typeMouvement = "Entrée", quantite = 12, utilisateurNom = "Jean Stock", motif = "Stock initial"))

            // 5. IT Assets (Equipements)
            val eq1Id = equipementDao.insertEquipement(Equipement(type = "Ordinateur portable", marque = "Dell", modele = "Latitude 5430", numeroSerie = "DELL-5430-XP8", dateAchat = "2024-02-15", valeurAchat = 1100.0, statut = "En service", localisation = "Bureau R&D", utilisateurAffecte = "Thomas Durand"))
            val eq2Id = equipementDao.insertEquipement(Equipement(type = "Ordinateur portable", marque = "Apple", modele = "MacBook Pro M3", numeroSerie = "APPL-M3-9092", dateAchat = "2024-05-10", valeurAchat = 2200.0, statut = "En service", localisation = "Direction", utilisateurAffecte = "Sophie Martin"))
            val eq3Id = equipementDao.insertEquipement(Equipement(type = "Serveur", marque = "HP", modele = "ProLiant DL380 Gen11", numeroSerie = "HP-PL-8899A", dateAchat = "2023-11-20", valeurAchat = 5400.0, statut = "En réparation", localisation = "Salle Serveur", utilisateurAffecte = "Service IT"))
            val eq4Id = equipementDao.insertEquipement(Equipement(type = "Imprimante", marque = "Canon", modele = "i-SENSYS X", numeroSerie = "CAN-IS-77", dateAchat = "2024-01-08", valeurAchat = 450.0, statut = "En panne", localisation = "Open Space Commercial", utilisateurAffecte = "Ventes"))

            // 6. Licences
            licenceDao.insertLicence(Licence(nomLogiciel = "Office 365 Business", equipementId = eq1Id, equipementNom = "Dell Latitude 5430 (Thomas Durand)", dateAchat = "2025-01-15", dateExpiration = "2026-01-15", nombreUtilisateurs = 1, typeLicence = "Annuelle"))
            licenceDao.insertLicence(Licence(nomLogiciel = "Windows 11 Enterprise", equipementId = null, equipementNom = "Volume Licence", dateAchat = "2024-01-01", dateExpiration = "2029-01-01", nombreUtilisateurs = 50, typeLicence = "Volume"))
            licenceDao.insertLicence(Licence(nomLogiciel = "SolidWorks 2024 CAD", equipementId = eq1Id, equipementNom = "Dell Latitude 5430 (Thomas Durand)", dateAchat = "2025-04-01", dateExpiration = "2026-04-01", nombreUtilisateurs = 5, typeLicence = "Annuelle"))

            // 7. Interventions
            interventionDao.insertIntervention(Intervention(equipementId = eq4Id, equipementNom = "Canon i-SENSYS X (Ventes)", date = "2026-07-01", description = "Remplacement du tambour d'impression suite à des lignes noires horizontales", technicienNom = "Luc Tech", cout = 120.0))
            interventionDao.insertIntervention(Intervention(equipementId = eq3Id, equipementNom = "HP ProLiant DL380 Gen11 (Service IT)", date = "2026-07-10", description = "Changement de l'alimentation redondante défectueuse", technicienNom = "Luc Tech", cout = 350.0))

            // 8. Action logs
            logDao.insertLog(ActionLog(utilisateurNom = "Admin", action = "Initialisation de la base de données avec le jeu de données d'usine"))
            logDao.insertLog(ActionLog(utilisateurNom = "Jean Stock", action = "Enregistrement de stock initial pour Courroie de transmission"))
        }
    }
}
