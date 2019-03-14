package github.relvl.threadpoolmanager

import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/** Потоковый пул по-умолчанию.
 * @author karpov-em on 014, 14.03.2019*/
object ThreadPoolManager : IThreadPoolManager {
    private val LOGGER = LoggerFactory.getLogger(ThreadPoolManager::class.java)
    private val THREAD_POOL_MAX_SCHEDULE_DELAY = TimeUnit.NANOSECONDS.toMillis(java.lang.Long.MAX_VALUE - System.nanoTime()) / 2
    private val NCPUS = Runtime.getRuntime().availableProcessors()

    private var THREAD_POOL_AWAIT_TERMINATION: Long = 10L
    private var THREAD_POOL_AWAIT_TERMINATION_SCHEDULER: Long = 1L

    private var isTerminate = false

    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(
            NCPUS * 2, // Количество потоков в пуле по-умолчанию.
            NCPUS * 2 + 10, // Максимальное количество потоков в пуле.
            5, TimeUnit.SECONDS, // Время жизни дополнительного потока.
            object : LinkedBlockingQueue<Runnable>(10) { // Очередь исполнения. Оффер переопределен, пул всегда бомбанёт исключение, после чего добавит в очередь.
                override fun offer(e: Runnable) = false
            },
            PriorityThreadFactory("${ThreadPoolManager::class.java.simpleName}-Default-Queued", Thread.NORM_PRIORITY),
            RejectedExecutionHandler { r, executor -> if (executor.isShutdown) return@RejectedExecutionHandler else executor.queue.put(r) }
    )
    private val scheduledExecutor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(
            NCPUS * 4, //Количество потоков в пуле
            PriorityThreadFactory("${ThreadPoolManager::class.java.simpleName}-Default-Scheduled", Thread.NORM_PRIORITY),
            RejectedExecutionHandler { r, executor ->
                if (executor.isShutdown) return@RejectedExecutionHandler
                else LOGGER.error("scheduledExecutor rejects task: $r. Executor: $executor", RejectedExecutionException())
            } // Логгировщик отказов выполнения
    )

    fun configureExecutor(corePoolSize: Int? = null, maxPoolSize: Int? = null, keepAliveTime: Long? = null) {
        if (corePoolSize != null) executor.corePoolSize = corePoolSize
        if (maxPoolSize != null) executor.maximumPoolSize = maxPoolSize
        if (keepAliveTime != null) executor.setKeepAliveTime(keepAliveTime, TimeUnit.SECONDS)
    }

    fun configureScheduler(corePoolSize: Int? = null, rejectedExecutionHandler: RejectedExecutionHandler? = null) {
        if (corePoolSize != null) scheduledExecutor.corePoolSize = corePoolSize
        if (rejectedExecutionHandler != null) scheduledExecutor.rejectedExecutionHandler = rejectedExecutionHandler
    }

    /** Помещает задачу в очередь исполнения пула. */
    override fun execute(runnable: Runnable) = executor.execute(RunnableWrapper(runnable))

    /** Планирует исполнение задачи с указанной задержкой (мс). */
    override fun schedule(delay: Long, r: Runnable): ScheduledFuture<*> = scheduledExecutor.schedule(RunnableWrapper(r), clamp(delay), TimeUnit.MILLISECONDS)

    /**
     * Планирует исполнение периодической задачи с фиксированным интервалом между стартами исполнения задачи.
     * initial - > (run and start scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется, в противном случае планировка назначается до принудительной отмены.
     * Если время исполнения задачи превышает интерфал ожидания, то следующее исполнение начнется сразу по завершении текущего, но не параллельно!
     */
    override fun scheduleAtFixedRate(initial: Long, delay: Long, r: Runnable): ScheduledFuture<*> = scheduledExecutor.scheduleAtFixedRate(RunnableWrapper(r), clamp(initial), clamp(delay), TimeUnit.MILLISECONDS)

    /**
     * Планирует исполнение периодической задачи с фиксированным интервалом между завершением текущей задачи и началом новой.
     * initial -> (run, stop) -> (strat scheduling)
     * Если при выполнении задачи происходит исключение - последующее исполнение подавляется, в противном случае планировка назначается до принудительной отмены.
     */
    override fun scheduleAtFixedDelay(initial: Long, delay: Long, r: Runnable): ScheduledFuture<*> = scheduledExecutor.scheduleWithFixedDelay(RunnableWrapper(r), clamp(initial), clamp(delay), TimeUnit.MILLISECONDS)

    /** Ограничивает максимальное время ожидания планировщика.  */
    private fun clamp(delay: Long): Long = Math.max(0, Math.min(THREAD_POOL_MAX_SCHEDULE_DELAY, delay))

    /**
     * Завершает планировщик и пул.
     * Дожидается завершения задачи планировщика, но не дольше THREAD_POOL_AWAIT_TERMINATION_SCHEDULER сек.
     * Дожидается завершения задачи пула, но не дольше THREAD_POOL_AWAIT_TERMINATION сек.
     */
    fun destroy() {
        isTerminate = true
        try {
            scheduledExecutor.shutdown()
            scheduledExecutor.awaitTermination(THREAD_POOL_AWAIT_TERMINATION_SCHEDULER, TimeUnit.SECONDS)
            executor.shutdown()
            executor.awaitTermination(THREAD_POOL_AWAIT_TERMINATION, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            LOGGER.error("", e)
        }
    }

    private class RunnableWrapper(val originalRunnable: Runnable) : Runnable {
        override fun run() = try {
            originalRunnable.run()
        } catch (e: Exception) {
            LOGGER.error("RunnableWrapper:", e)
        }
    }

    private class PriorityThreadFactory(private val threadName: String, private val prio: Int) : ThreadFactory {
        private val threadNumber = AtomicInteger(1)
        private val group: ThreadGroup = ThreadGroup(threadName)
        override fun newThread(r: Runnable): Thread = Thread(group, r).apply {
            name = "$threadName-${threadNumber.getAndIncrement()}"
            priority = prio
        }
    }
}