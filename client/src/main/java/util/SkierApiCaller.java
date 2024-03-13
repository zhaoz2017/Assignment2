package util;

import model.LiftRideEvent;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes API calls to the Skiers API, posting lift ride events in a concurrent manner.
 */
public class SkierApiCaller implements Runnable {
  // Queue holding events to be processed.
  private final BlockingQueue<LiftRideEvent> queue;
  // Number of posts to be made by this thread.
  private final int numPosts;
  // Latch to signal completion of all threads.
  private final CountDownLatch countDownLatch;
  // API client configured with the server's base path.
  private final ApiClient apiClient;
  // Skiers API for making HTTP requests.
  private final SkiersApi skiersApi;
  // Counters for successful and failed requests.
  private final AtomicInteger successReq;
  private final AtomicInteger failedReq;
  // Lists for recording latencies and response details.
  private final List<Long> latencies;
  private final List<String> records;

  /**
   * Constructs a new SkierApiCaller.
   *
   * @param queue The queue of lift ride events to process.
   * @param numPosts The number of posts to be processed by this caller.
   * @param countDownLatch A countdown latch to signal task completion.
   * @param basePath The base path URL of the API.
   * @param successReq Counter for successful requests.
   * @param failedReq Counter for failed requests.
   * @param latencies List to record the latencies of each request.
   * @param records List to record the details of each request.
   */
  public SkierApiCaller(BlockingQueue<LiftRideEvent> queue, int numPosts, CountDownLatch countDownLatch,
      String basePath, AtomicInteger successReq, AtomicInteger failedReq,
      List<Long> latencies, List<String> records) {
    this.queue = queue;
    this.numPosts = numPosts;
    this.countDownLatch = countDownLatch;
    this.apiClient = new ApiClient();
    this.apiClient.setBasePath(basePath);
    this.apiClient.addDefaultHeader("Accept", "application/json");
    this.skiersApi = new SkiersApi(apiClient);
    this.successReq = successReq;
    this.failedReq = failedReq;
    this.latencies = latencies;
    this.records = records;
  }

  /**
   * Runs the thread, processing lift ride events by making API calls.
   */
  @Override
  public void run() {
    for (int i = 0; i < numPosts; i++) {
      try {
        LiftRideEvent event = queue.take();
        boolean success = processLiftRideEvent(event);
        if (success) {
          successReq.incrementAndGet();
          System.out.println(Thread.currentThread() + " numPosts: " + numPosts + " i: " + i + " : Post sent successfully " + successReq.get());
        } else {
          failedReq.incrementAndGet();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // Properly handle thread interruption.
        return; // Exit the method early if the thread is interrupted.
      }
    }
    System.out.println(Thread.currentThread().getId() + ": Completed");
    countDownLatch.countDown(); // Notify countDownLatch of task completion.
  }

  /**
   * Processes a single lift ride event, attempting to post it via the API.
   *
   * @param event The lift ride event to be processed.
   * @return true if the post was successful, false otherwise.
   */
  private boolean processLiftRideEvent(LiftRideEvent event) {
    LiftRide body = new LiftRide();
    body.setTime(event.getTime());
    body.setLiftID(event.getLiftId());

    int retryCount = 5;
    while (retryCount > 0) {
      try {
        long startTime = System.currentTimeMillis();
        ApiResponse<Void> response = skiersApi.writeNewLiftRideWithHttpInfo(body,
            event.getResortId(), event.getSeasonId(), event.getDayId(), event.getSkierId());
        long endTime = System.currentTimeMillis();
        long latency = endTime - startTime;
        int status = response.getStatusCode();
        recordLatencyAndStatus(latency, startTime, status);

        if (status == 201) {
          return true; // Successful request.
        } else {
          retryCount--;
          Thread.sleep(100); // Wait before retrying to reduce load on the server.
        }
      } catch (ApiException | InterruptedException e) {
        System.err.println("Exception occurred when calling SkiersApi: " + e.getMessage());
        e.printStackTrace();
        retryCount--;
        try {
          Thread.sleep(100); // Wait before retrying after an exception.
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt(); // Properly handle interruption.
          return false; // Exit if the thread is interrupted during sleep.
        }
      }
    }
    return false; // Return false if retries are exhausted without success.
  }

  /**
   * Records the latency and status code of an API call.
   *
   * @param latency The latency of the request in milliseconds.
   * @param startTime The start time of the request.
   * @param status The HTTP status code of the response.
   */
  private void recordLatencyAndStatus(long latency, long startTime, int status) {
    synchronized (latencies) {
      latencies.add(latency);
    }
    synchronized (records) {
      records.add(String.format("%d,POST,%d,%d", startTime, latency, status));
    }
  }
}
