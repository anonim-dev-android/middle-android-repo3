package ru.yandex.architectureproject.domain

import kotlinx.coroutines.delay
import ru.yandex.architectureproject.data.repository.TaskRepository

class CompleteTaskUseCase(
    private val repository: TaskRepository,

) {
    private val autodeleteDelayMs = 10_000L

    /**
     * // [Задание 3] автоудаление задачи через [autodeleteDelayMs] секунд после выполнения
     */
    suspend operator fun invoke(taskId: Int) {
        repository.completeTask(taskId)
        delay(autodeleteDelayMs)
        repository.deleteTask(taskId)
    }

}
