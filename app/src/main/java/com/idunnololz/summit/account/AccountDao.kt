package com.idunnololz.summit.account

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class AccountDao {
    @Query("SELECT * FROM account")
    abstract suspend fun getAll(): List<Account>

    @Query("SELECT * FROM account")
    abstract fun getAllSync(): List<Account>

    @Query("SELECT * FROM account WHERE current = 1")
    abstract suspend fun getCurrentAccount(): Account?

    @Query("SELECT * FROM account LIMIT 1")
    abstract suspend fun getFirstAccount(): Account?

    @Query("SELECT * FROM account WHERE id = :accountId")
    abstract suspend fun getAccountById(accountId: Int): Account?

    @Insert(onConflict = OnConflictStrategy.IGNORE, entity = Account::class)
    abstract suspend fun insert(account: Account)

    @Update(entity = Account::class)
    abstract suspend fun update(account: Account)

    @Query("UPDATE account set current = 0 where current = 1")
    abstract suspend fun removeCurrent()

    @Query("UPDATE account set current = 1 where id = :accountId")
    abstract suspend fun setCurrent(accountId: Int)

    @Delete(entity = Account::class)
    abstract suspend fun delete(account: Account)

    @Query("SELECT COUNT(*) FROM account")
    abstract suspend fun count(): Int

    @Transaction
    open suspend fun clearAndSetCurrent(accountId: Int) {
        removeCurrent()
        setCurrent(accountId)
    }
}
