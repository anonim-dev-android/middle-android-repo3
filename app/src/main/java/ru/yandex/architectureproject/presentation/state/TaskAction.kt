package ru.yandex.architectureproject.presentation.state

/**
 * // [Задание 1]
 * события, которые происходят в UI или приходят от внешних источников
 */
sealed class TaskAction {
    /** Загрузка заданий — для получения списка задач */
    data object LoadTasks : TaskAction()

    /**
     * Добавление задания — для создания новой задачи
     * @param text текст задания, которое нужно добавить
     * */
    data class AddTask(val text: String) : TaskAction()

    /**
     * Обновление статуса задания — чтобы помечать задачу как выполненную или невыполненную
     * @param id id задания
     * @param isDone сделано/не сделано
     * */
    data class UpdateTaskStatus(val id: Int, val isDone: Boolean) : TaskAction()

    /**
     * Удаление задания — для удаления задач
     * @param id id задания, которое надо удалить
     * */
    data class DeleteTask(val id: Int) : TaskAction()
}
