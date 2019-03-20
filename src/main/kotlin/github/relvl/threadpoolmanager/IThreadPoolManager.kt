package github.relvl.threadpoolmanager

import java.util.concurrent.ScheduledFuture

/** @author Relvl on 014, 14.03.2019*/
internal interface IThreadPoolManager {
    /** Запустить задачу в отдельном потоке из пула. */
    fun execute(runnable: Runnable)

    /** Запустить задачу в отдельном потоке из пула. */
    fun execute(runnable: () -> Unit) = this.execute(Runnable { runnable() })

    /** Запланировать задачу с указанной задержкой (мс). */
    fun schedule(delay: Long, r: Runnable): ScheduledFuture<*>

    /** Запланировать задачу с указанной задержкой (мс). */
    fun schedule(delay: Long, r: () -> Unit): ScheduledFuture<*> = this.schedule(delay, Runnable { r() })

    /**
     * Запланировать периодическую задачу с фиксированным интервалом между стартами исполнения задачи.
     * initial - > (run and start scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется,
     * в противном случае планировка назначается до принудительной отмены.
     * Если время исполнения задачи превышает интерфал ожидания, то следующее исполнение начнется сразу по завершении текущего, но не параллельно!
     */
    fun scheduleAtFixedRate(initial: Long, delay: Long, r: Runnable): ScheduledFuture<*>

    /**
     * Запланировать периодическую задачу с фиксированным интервалом между стартами исполнения задачи.
     * initial - > (run and start scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется,
     * в противном случае планировка назначается до принудительной отмены.
     * Если время исполнения задачи превышает интерфал ожидания, то следующее исполнение начнется сразу по завершении текущего, но не параллельно!
     */
    fun scheduleAtFixedRate(initial: Long, delay: Long, r: () -> Unit): ScheduledFuture<*> = this.scheduleAtFixedRate(initial, delay, Runnable { r() })

    /**
     * Запланировать периодическую задачу с фиксированным интервалом между завершением текущей задачи и началом новой.
     * initial -> (run) -> (stop) -> (strat scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется,
     * в противном случае планировка назначается до принудительной отмены.
     */
    fun scheduleAtFixedDelay(initial: Long, delay: Long, r: Runnable): ScheduledFuture<*>

    /**
     * Запланировать периодическую задачу с фиксированным интервалом между завершением текущей задачи и началом новой.
     * initial -> (run) -> (stop) -> (strat scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется,
     * в противном случае планировка назначается до принудительной отмены.
     */
    fun scheduleAtFixedDelay(initial: Long, delay: Long, r: () -> Unit): ScheduledFuture<*> = this.scheduleAtFixedDelay(initial, delay, Runnable { r() })
}