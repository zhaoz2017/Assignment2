package util;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import model.LiftRideEvent;

public class ConcurrentLiftRidePoster {

  public void launchLiftRidePostingProcess(
      int numThreadsInitial,
      int numPostsPerThread,
      int totalPosts,
      String basePath,
      List<Long> latencies,
      AtomicInteger successfulReq,
      AtomicInteger failedReq,
      List<String> recordsToWrite
  ) {
    ExecutorService executorService = Executors.newFixedThreadPool(numThreadsInitial);
    BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>();
    CountDownLatch liftRideLatch = new CountDownLatch(1);
    CountDownLatch clientLatch = new CountDownLatch(numThreadsInitial);

    startDataProducerThread(eventQueue, totalPosts, liftRideLatch);

    startClientThreads(executorService, eventQueue, numThreadsInitial, numPostsPerThread, clientLatch,
        basePath, successfulReq, failedReq, latencies, recordsToWrite);

    waitForLatch(clientLatch, "Initial client threads");

    distributeRemainingPosts(executorService, eventQueue, numThreadsInitial, numPostsPerThread,
        totalPosts, basePath, successfulReq, failedReq, latencies, recordsToWrite);

    waitForLatch(liftRideLatch, "Lift ride generator thread");

    shutdownAndAwaitTermination(executorService);
  }

  private void startDataProducerThread(BlockingQueue<LiftRideEvent> eventQueue, int totalPosts, CountDownLatch latch) {
    Thread thread = new Thread(new RidesDataProducer(eventQueue, totalPosts, latch));
    thread.start();
  }

  private void startClientThreads(ExecutorService executor, BlockingQueue<LiftRideEvent> eventQueue, int numThreads,
      int postsPerThread, CountDownLatch latch, String basePath,
      AtomicInteger successfulReq, AtomicInteger failedReq,
      List<Long> latencies, List<String> records) {
    for (int i = 0; i < numThreads; i++) {
      executor.submit(new SkierApiCaller(eventQueue, postsPerThread, latch, basePath,
          successfulReq, failedReq, latencies, records));
    }
  }

  private void distributeRemainingPosts(ExecutorService executor, BlockingQueue<LiftRideEvent> eventQueue,
      int numThreads, int postsPerThread, int totalPosts,
      String basePath, AtomicInteger successfulReq,
      AtomicInteger failedReq, List<Long> latencies,
      List<String> records) {
    int totalPostedSoFar = numThreads * postsPerThread;
    int remainingPosts = totalPosts - totalPostedSoFar;
    int remainingPostsPerThread = remainingPosts / numThreads;

    if (remainingPostsPerThread > 0) {
      for (int i = 0; i < numThreads; i++) {
        executor.submit(new SkierApiCaller(eventQueue, remainingPostsPerThread, null, basePath,
            successfulReq, failedReq, latencies, records));
      }
    }
  }

  private void waitForLatch(CountDownLatch latch, String taskDescription) {
    try {
      latch.await();
      System.out.println(taskDescription + " complete");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Interrupted during " + taskDescription);
    }
  }

  private void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown();
    try {
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
