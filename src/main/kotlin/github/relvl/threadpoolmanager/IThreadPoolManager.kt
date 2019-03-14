package github.relvl.threadpoolmanager

import java.util.concurrent.ScheduledFuture

/** @author karpov-em on 014, 14.03.2019*/
internal interface IThreadPoolManager {
    fun execute(runnable: Runnable)

    /** Запланировать задачу с указанной задержкой (мс). */
    fun schedule(delay: Long, r: Runnable): ScheduledFuture<*>

    /**
     * Запланировать периодическую задачу с фиксированным интервалом между стартами исполнения задачи.
     * initial - > (run and start scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется,
     * в противном случае планировка назначается до принудительной отмены.
     * Если время исполнения задачи превышает интерфал ожидания, то следующее исполнение начнется сразу по завершении текущего, но не параллельно!
     */
    fun scheduleAtFixedRate(initial: Long, delay: Long, r: Runnable): ScheduledFuture<*>

    /**
     * Запланировать периодическую задачу с фиксированным интервалом между завершением текущей задачи и началом новой.
     * initial -> (run) -> (stop) -> (strat scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется,
     * в противном случае планировка назначается до принудительной отмены.
     */
    fun scheduleAtFixedDelay(initial: Long, delay: Long, r: Runnable): ScheduledFuture<*>
}