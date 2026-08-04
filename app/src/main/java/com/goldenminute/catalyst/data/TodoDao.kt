package com.goldenminute.catalyst.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, position ASC, id ASC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE categoryId = :categoryId ORDER BY isCompleted ASC, position ASC, id ASC")
    fun getTodosByCategory(categoryId: Int): Flow<List<TodoItem>>

    @Query("UPDATE todos SET position = :position WHERE id = :id")
    suspend fun updateTodoPosition(id: Int, position: Int)

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Int)

    @Query("UPDATE todos SET isCompleted = :isCompleted, completedDate = :completedDate WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean, completedDate: Long?)

    @Query("UPDATE todos SET isCompleted = 0, completedDate = null WHERE isDaily = 1 AND isCompleted = 1 AND completedDate < :startOfToday")
    suspend fun resetDailyTasks(startOfToday: Long)
    
    @Query("UPDATE subtasks SET isCompleted = 0 WHERE todoId IN (SELECT id FROM todos WHERE isDaily = 1)")
    suspend fun resetDailySubTasks()
    
    @Query("UPDATE todos SET elapsedTime = 0, pomodoroCycles = 0 WHERE (isDaily = 1) OR (dueDate IS NOT NULL AND dueDate < :startOfToday)")
    suspend fun clearExpiredTaskSessions(startOfToday: Long)
    
    @Query("UPDATE todos SET elapsedTime = elapsedTime + :timeToAdd WHERE id = :id")
    suspend fun addElapsedTime(id: Int, timeToAdd: Long)

    @Query("UPDATE todos SET pomodoroCycles = pomodoroCycles + :cyclesToAdd WHERE id = :id")
    suspend fun addPomodoroCycles(id: Int, cyclesToAdd: Int)

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND hasReminder = 1 AND reminderTime > :currentTime")
    suspend fun getPendingReminders(currentTime: Long): List<TodoItem>

    // --- SubTask Queries ---
    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    fun getAllSubTasks(): Flow<List<SubTask>>

    @Query("SELECT * FROM subtasks WHERE todoId = :todoId ORDER BY id ASC")
    fun getSubTasksForTodo(todoId: Int): Flow<List<SubTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask): Long

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubTaskById(id: Int)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateSubTaskCompletion(id: Int, isCompleted: Boolean)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE todoId = :todoId")
    suspend fun updateAllSubTasksCompletion(todoId: Int, isCompleted: Boolean)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}
