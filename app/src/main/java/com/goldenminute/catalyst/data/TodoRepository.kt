package com.goldenminute.catalyst.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val noteDao: NoteDao
) {
    val allTodos: Flow<List<TodoItem>> = todoDao.getAllTodos()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val taskCategories: Flow<List<Category>> = categoryDao.getCategoriesByType("task")
    val noteCategories: Flow<List<Category>> = categoryDao.getCategoriesByType("note")

    fun getTodosByCategory(categoryId: Int): Flow<List<TodoItem>> {
        return todoDao.getTodosByCategory(categoryId)
    }

    suspend fun getTodoById(id: Int): TodoItem? {
        return todoDao.getTodoById(id)
    }

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertTodo(todo: TodoItem): Long {
        return todoDao.insertTodo(todo)
    }

    suspend fun updateTodo(todo: TodoItem) {
        todoDao.updateTodo(todo)
    }

    suspend fun deleteTodo(todo: TodoItem) {
        todoDao.deleteTodo(todo)
    }

    suspend fun deleteTodoById(id: Int) {
        todoDao.deleteTodoById(id)
    }

    suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean, completedDate: Long?) {
        todoDao.updateCompletionStatus(id, isCompleted, completedDate)
    }

    suspend fun updateTodoPosition(id: Int, position: Int) {
        todoDao.updateTodoPosition(id, position)
    }

    suspend fun resetDailyTasks(startOfToday: Long) {
        todoDao.resetDailySubTasks()
        todoDao.resetDailyTasks(startOfToday)
    }

    suspend fun clearExpiredTaskSessions(startOfToday: Long) {
        todoDao.clearExpiredTaskSessions(startOfToday)
    }

    suspend fun addElapsedTime(id: Int, timeToAdd: Long) {
        todoDao.addElapsedTime(id, timeToAdd)
    }

    suspend fun addPomodoroCycles(id: Int, cyclesToAdd: Int) {
        todoDao.addPomodoroCycles(id, cyclesToAdd)
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // --- Sub-task features ---
    val allSubTasks: Flow<List<SubTask>> = todoDao.getAllSubTasks()

    fun getSubTasksForTodo(todoId: Int): Flow<List<SubTask>> {
        return todoDao.getSubTasksForTodo(todoId)
    }

    suspend fun insertSubTask(subTask: SubTask): Long {
        return todoDao.insertSubTask(subTask)
    }

    suspend fun deleteSubTaskById(id: Int) {
        todoDao.deleteSubTaskById(id)
    }

    suspend fun updateSubTaskCompletion(id: Int, isCompleted: Boolean) {
        todoDao.updateSubTaskCompletion(id, isCompleted)
    }

    // --- Notes features ---
    val allNotes: Flow<List<NoteItem>> = noteDao.getAllNotes()

    suspend fun insertNote(note: NoteItem): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteItem) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        noteDao.deleteNoteById(id)
    }
}
