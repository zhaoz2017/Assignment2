package client1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import util.ConcurrentLiftRidePoster;

public class Client {
  private static final int NUM_THREADS_INITIAL = 32;
  private static final int NUM_POSTS_PER_THREAD = 1000;
  private static final int TOTAL_POSTS = 200000;
  private static final String LOCAL_BASE_PATH = "http://localhost:8080/server_war_exploded/";
  private static final String REMOTE_BASE_PATH = "http://ec2-54-212-6-176.us-west-2.compute.amazonaws.com:8080/server_war/";

  public static void main(String[] args) {
    // Initialize counters for successful and unsuccessful requests
    AtomicInteger successfulRequests = new AtomicInteger(0);
    AtomicInteger failedRequests = new AtomicInteger(0);

    // Initialize latency list (though not used, can be helpful for future performance measurement)
    List<Long> latencies = new ArrayList<>();

    // Determine the base path based on command-line arguments if provided
    String basePath = args.length > 0 ? args[0] : LOCAL_BASE_PATH;

    ConcurrentLiftRidePoster concurrentLiftRidePoster = new ConcurrentLiftRidePoster();

    long startTime = System.currentTimeMillis();

    // Launch the process to post lift ride data concurrently
    concurrentLiftRidePoster.launchLiftRidePostingProcess(
        NUM_THREADS_INITIAL,
        NUM_POSTS_PER_THREAD,
        TOTAL_POSTS,
        basePath,
        latencies, // Passing latencies list for future enhancements
        successfulRequests,
        failedRequests,
        new ArrayList<>() // Placeholder for records, if needed in the future
    );

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    double throughput = (double) TOTAL_POSTS / (double) totalTime * 1000;

    // Print the summary of the operation
    System.out.println("Operation Summary:");
    System.out.printf("Total time taken: %.2f seconds%n", totalTime / 1000.0);
    System.out.printf("Throughput: %.2f requests per second%n", throughput);
    System.out.println("Number of successful requests: " + successfulRequests.get());
    System.out.println("Number of unsuccessful requests: " + failedRequests.get());
  }
}
