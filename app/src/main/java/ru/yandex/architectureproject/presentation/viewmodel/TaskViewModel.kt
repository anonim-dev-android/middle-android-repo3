package ru.yandex.architectureproject.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.yandex.architectureproject.domain.AddTaskUseCase
import ru.yandex.architectureproject.domain.CompleteTaskUseCase
import ru.yandex.architectureproject.domain.DeleteTaskUseCase
import ru.yandex.architectureproject.domain.GetAllTasksUseCase
import ru.yandex.architectureproject.domain.IncompleteTaskUseCase
import ru.yandex.architectureproject.presentation.state.TaskAction
import ru.yandex.architectureproject.presentation.state.TaskState

class TaskViewModel(
    private val addTaskUseCase: AddTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val incompleteTaskUseCase: IncompleteTaskUseCase,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val tag = "TaskViewModel"

    private val _state = MutableStateFlow<TaskState>(TaskState.Loading)
    val state: StateFlow<TaskState> = _state.asStateFlow()

    // [Задание 4] мапа для хранения очереди задач на удаление
    private val tasksToDeleteMap = mutableMapOf<Int, Job>()

    init {
        reduce(TaskAction.LoadTasks)
    }

    /**
     * // [Задание 2] обработчик действий.
     * * Загрузка заданий: loadTasks()
     * * Добавление задания: подключите UseCase, который добавляет новую задачу.
     * * Обновление статуса задания: обеспечьте вызов UseCase, меняющего статус задачи (сделано/не сделано).
     * * Удаление задания: свяжите действие с UseCase, отвечающим за удаление задачи.
     * * Обновление данных: добавьте обновление данных, следующие за любым их изменением.
     *
     * @param action текущее действие [TaskAction]
     * @return Unit, вызыывает соответствующее действие-usecase
     */
    fun reduce(action: TaskAction) {
        viewModelScope.launch {
            when(action) {
                is TaskAction.LoadTasks -> loadTasks()
                is TaskAction.AddTask -> withContext(ioDispatcher) {
                    addTaskUseCase(action.text)
                }
                    is TaskAction.UpdateTaskStatus -> {
                        if (action.isDone) {
                            tasksToDeleteMap[action.id] = this.coroutineContext.job.apply {
                                this.invokeOnCompletion { th ->
                                    if (th is CancellationException) {
                                        Log.e(tag, "Автоудаление задания ${action.id} было отменено. Причина: ${th.message}")
                                    }
                                }
                            }
                            completeTaskUseCase(action.id)
                        } else {
                            tasksToDeleteMap
                                .remove(action.id)
                                ?.cancel(CancellationException("Задание больше не отмечено выполненным, таймер на автоудаление остановлен"))
                            incompleteTaskUseCase(action.id)
                        }
                    }
                is TaskAction.DeleteTask -> withContext(ioDispatcher) {
                    deleteTaskUseCase(action.id)
                }
            }
        }
    }

    private suspend fun loadTasks() {
        withContext(ioDispatcher) {
            getAllTasksUseCase()
                .distinctUntilChanged()
                .onStart { _state.value = TaskState.Loading }
                .catch { e -> _state.value = TaskState.Error(e.message ?: "Ошибка загрузки") }
                .collect { tasks ->
                    _state.value = TaskState.Loaded(tasks)
                    // [Задание 4] ставим таймер на удаление выполненных при загрузке данных "с нуля"
                    tasks.filter { it.isDone }.forEach { reduce(TaskAction.UpdateTaskStatus(it.id, true)) }
                }
        }
    }
}
