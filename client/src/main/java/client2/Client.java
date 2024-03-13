package client2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import util.ConcurrentLiftRidePoster;

public class Client {
  private static final int NUM_THREADS_INITIAL = 32;
  private static final int NUM_POSTS_PER_THREAD = 1000;
  private static final int TOTAL_POSTS = 200000;
  private static final String CSV_FILE_PATH = "records.csv";
  private static final String BASE_PATH = "http://localhost:8080/server_war_exploded";
  private static final String REMOTE_BASE_PATH = "http://ec2-54-212-6-176.us-west-2.compute.amazonaws.com:8080/server_war/";
  private static final List<String> records = Collections.synchronizedList(new ArrayList<>());

  public static void main(String[] args) {
    AtomicInteger successfulRequests = new AtomicInteger(0);
    AtomicInteger failedRequests = new AtomicInteger(0);
    List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    ConcurrentLiftRidePoster concurrentLiftRidePoster = new ConcurrentLiftRidePoster();

    long startTime = System.currentTimeMillis();
    concurrentLiftRidePoster.launchLiftRidePostingProcess(
        NUM_THREADS_INITIAL,
        NUM_POSTS_PER_THREAD,
        TOTAL_POSTS, BASE_PATH,
        latencies,
        successfulRequests,
        failedRequests,
        records
    );

    writeRecordsToFile();
    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;

    calculateAndPrintMetrics(latencies, totalTime);
  }

  private static void writeRecordsToFile() {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(CSV_FILE_PATH, true))) {
      for (String record : records) {
        bufferedWriter.write(record);
        bufferedWriter.newLine(); // Ensure each record is on a new line.
      }
    } catch (IOException e) {
      System.err.println("Failed to write records to file: " + e.getMessage());
    }
  }

  private static void calculateAndPrintMetrics(List<Long> latencies, long totalTime) {
    // Ensure thread safety on latencies list
    synchronized (latencies) {
      Collections.sort(latencies);
      double meanResponseTime = latencies.stream().mapToLong(Long::valueOf).average().orElse(Double.NaN);
      long medianResponseTime = latencies.get(latencies.size() / 2);
      long p99ResponseTime = latencies.get((int) (latencies.size() * 0.99));
      long minResponseTime = latencies.get(0);
      long maxResponseTime = latencies.get(latencies.size() - 1);
      double throughput = (double) TOTAL_POSTS / totalTime * 1000;

      System.out.println("Total Time: " + totalTime / 1000.0 + "s");
      System.out.println("Mean Response Time: " + meanResponseTime + " ms");
      System.out.println("Median Response Time: " + medianResponseTime + " ms");
      System.out.println("P99 Response Time: " + p99ResponseTime + " ms");
      System.out.println("Min Response Time: " + minResponseTime + " ms");
      System.out.println("Max Response Time: " + maxResponseTime + " ms");
      System.out.println("Throughput: " + throughput + " requests per second");
    }
  }
}
