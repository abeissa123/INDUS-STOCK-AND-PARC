package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM utilisateurs ORDER BY nom ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM utilisateurs WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY designation ASC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: Long): Article?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article): Long

    @Query("UPDATE articles SET quantiteStock = :newQty WHERE id = :id")
    suspend fun updateStockQuantity(id: Long, newQty: Int)

    @Delete
    suspend fun deleteArticle(article: Article)
}

@Dao
interface MouvementStockDao {
    @Query("SELECT * FROM mouvements_stock ORDER BY date DESC")
    fun getAllMouvements(): Flow<List<MouvementStock>>

    @Query("SELECT * FROM mouvements_stock WHERE articleId = :articleId ORDER BY date DESC")
    fun getMouvementsByArticle(articleId: Long): Flow<List<MouvementStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouvement(mouvement: MouvementStock): Long
}

@Dao
interface FournisseurDao {
    @Query("SELECT * FROM fournisseurs ORDER BY nom ASC")
    fun getAllFournisseurs(): Flow<List<Fournisseur>>

    @Query("SELECT * FROM fournisseurs WHERE id = :id LIMIT 1")
    suspend fun getFournisseurById(id: Long): Fournisseur?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFournisseur(fournisseur: Fournisseur): Long

    @Delete
    suspend fun deleteFournisseur(fournisseur: Fournisseur)
}

@Dao
interface EquipementDao {
    @Query("SELECT * FROM equipements ORDER BY type ASC, marque ASC")
    fun getAllEquipements(): Flow<List<Equipement>>

    @Query("SELECT * FROM equipements WHERE id = :id LIMIT 1")
    suspend fun getEquipementById(id: Long): Equipement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipement(equipement: Equipement): Long

    @Delete
    suspend fun deleteEquipement(equipement: Equipement)
}

@Dao
interface LicenceDao {
    @Query("SELECT * FROM licences ORDER BY nomLogiciel ASC")
    fun getAllLicences(): Flow<List<Licence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicence(licence: Licence): Long

    @Delete
    suspend fun deleteLicence(licence: Licence)
}

@Dao
interface InterventionDao {
    @Query("SELECT * FROM interventions ORDER BY date DESC")
    fun getAllInterventions(): Flow<List<Intervention>>

    @Query("SELECT * FROM interventions WHERE equipementId = :equipementId ORDER BY date DESC")
    fun getInterventionsByEquipement(equipementId: Long): Flow<List<Intervention>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: Intervention): Long

    @Delete
    suspend fun deleteIntervention(intervention: Intervention)
}

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM journal_actions ORDER BY date DESC")
    fun getAllLogs(): Flow<List<ActionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLog): Long
}
