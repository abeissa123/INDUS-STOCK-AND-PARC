package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val articleDao = database.articleDao()
    private val mouvementDao = database.mouvementStockDao()
    private val fournisseurDao = database.fournisseurDao()
    private val equipementDao = database.equipementDao()
    private val licenceDao = database.licenceDao()
    private val interventionDao = database.interventionDao()
    private val logDao = database.actionLogDao()

    // Flows for Reactive UI Updates
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()
    val allMouvements: Flow<List<MouvementStock>> = mouvementDao.getAllMouvements()
    val allFournisseurs: Flow<List<Fournisseur>> = fournisseurDao.getAllFournisseurs()
    val allEquipements: Flow<List<Equipement>> = equipementDao.getAllEquipements()
    val allLicences: Flow<List<Licence>> = licenceDao.getAllLicences()
    val allInterventions: Flow<List<Intervention>> = interventionDao.getAllInterventions()
    val allLogs: Flow<List<ActionLog>> = logDao.getAllLogs()

    // Suspend operations for asynchronous database modifications

    // Users
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    // Articles & Stock Movements
    suspend fun getArticleById(id: Long): Article? = articleDao.getArticleById(id)
    suspend fun insertArticle(article: Article): Long = articleDao.insertArticle(article)
    suspend fun deleteArticle(article: Article) = articleDao.deleteArticle(article)
    suspend fun updateStockQuantity(id: Long, newQty: Int) = articleDao.updateStockQuantity(id, newQty)
    
    suspend fun insertMouvement(mouvement: MouvementStock, updatedStockQty: Int) {
        mouvementDao.insertMouvement(mouvement)
        articleDao.updateStockQuantity(mouvement.articleId, updatedStockQty)
        logDao.insertLog(ActionLog(
            utilisateurNom = mouvement.utilisateurNom,
            action = "${mouvement.typeMouvement} de ${mouvement.quantite}x ${mouvement.articleDesignation} (${mouvement.motif})"
        ))
    }

    fun getMouvementsByArticle(articleId: Long): Flow<List<MouvementStock>> =
        mouvementDao.getMouvementsByArticle(articleId)

    // Fournisseurs
    suspend fun getFournisseurById(id: Long): Fournisseur? = fournisseurDao.getFournisseurById(id)
    suspend fun insertFournisseur(fournisseur: Fournisseur): Long {
        val id = fournisseurDao.insertFournisseur(fournisseur)
        return id
    }
    suspend fun deleteFournisseur(fournisseur: Fournisseur) = fournisseurDao.deleteFournisseur(fournisseur)

    // Equipements
    suspend fun getEquipementById(id: Long): Equipement? = equipementDao.getEquipementById(id)
    suspend fun insertEquipement(equipement: Equipement, operator: String): Long {
        val id = equipementDao.insertEquipement(equipement)
        logDao.insertLog(ActionLog(
            utilisateurNom = operator,
            action = "Ajout/Modif Équipement [${equipement.type} ${equipement.marque} ${equipement.modele}]"
        ))
        return id
    }
    suspend fun deleteEquipement(equipement: Equipement, operator: String) {
        equipementDao.deleteEquipement(equipement)
        logDao.insertLog(ActionLog(
            utilisateurNom = operator,
            action = "Suppression Équipement [${equipement.type} ${equipement.marque} ${equipement.modele}]"
        ))
    }

    // Licences
    suspend fun insertLicence(licence: Licence, operator: String): Long {
        val id = licenceDao.insertLicence(licence)
        logDao.insertLog(ActionLog(
            utilisateurNom = operator,
            action = "Ajout/Modif Licence [${licence.nomLogiciel}]"
        ))
        return id
    }
    suspend fun deleteLicence(licence: Licence, operator: String) {
        licenceDao.deleteLicence(licence)
        logDao.insertLog(ActionLog(
            utilisateurNom = operator,
            action = "Suppression Licence [${licence.nomLogiciel}]"
        ))
    }

    // Interventions
    suspend fun insertIntervention(intervention: Intervention, operator: String): Long {
        val id = interventionDao.insertIntervention(intervention)
        logDao.insertLog(ActionLog(
            utilisateurNom = operator,
            action = "Nouvelle intervention enregistrée sur ${intervention.equipementNom} par ${intervention.technicienNom}"
        ))
        return id
    }
    suspend fun deleteIntervention(intervention: Intervention, operator: String) {
        interventionDao.deleteIntervention(intervention)
        logDao.insertLog(ActionLog(
            utilisateurNom = operator,
            action = "Suppression intervention sur ${intervention.equipementNom}"
        ))
    }

    // Logs
    suspend fun insertLog(log: ActionLog): Long = logDao.insertLog(log)
}
