package util;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import model.LiftRideEvent;

public class RidesDataProducer implements Runnable {

  private final BlockingQueue<LiftRideEvent> queue;
  private final int numPosts;
  private final Random random;
  private final CountDownLatch countDownLatch;

  public RidesDataProducer(BlockingQueue<LiftRideEvent> queue, int numPosts,
      CountDownLatch countDownLatch) {
    this.queue = queue;
    this.numPosts = numPosts;
    this.random = new Random();
    this.countDownLatch = countDownLatch;
  }

  /**
   * When an object implementing interface {@code Runnable} is used to create a thread, starting the
   * thread causes the object's {@code run} method to be called in that separately executing
   * thread.
   * <p>
   * The general contract of the method {@code run} is that it may take any action whatsoever.
   *
   * @see Thread#run()
   */
  @Override
  public void run() {
    for (int i = 0; i < numPosts; i++) {
      LiftRideEvent ride = dataGenerator();
      try {
        queue.put(ride);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    countDownLatch.countDown();
  }

  private LiftRideEvent dataGenerator() {
    return new LiftRideEvent(
        random.nextInt(100000) + 1,
        random.nextInt(10) + 1,
        random.nextInt(40) + 1,
        "2024",
        "1",
        random.nextInt(360) + 1
    );
  }
}
