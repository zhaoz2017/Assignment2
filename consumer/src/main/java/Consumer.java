import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Consumer {
  private final static String TASK_QUEUE_NAME = "task_queue";
  private final static ConcurrentHashMap<Integer, Integer> liftRidesMap = new ConcurrentHashMap<>();
  private final static Integer THREADS = 10;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setPort(5672);
    factory.setUsername("admin");
    factory.setPassword("passw0rd");

    ExecutorService executor = Executors.newFixedThreadPool(THREADS);

    Connection connection = factory.newConnection(executor);
    Channel channel = connection.createChannel();

    channel.queueDeclare(TASK_QUEUE_NAME, false, false, false, null);
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      executor.submit(() -> {
        String message = null;
        try {
          message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [x] Received '" + message + "'");
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }

        Gson gson = new Gson();
        LiftRide liftRide = gson.fromJson(message, LiftRide.class);
        int time = liftRide.getTime();
        int liftId = liftRide.getLiftID();
        liftRidesMap.put(time, liftId);
        System.out.println(" [x] Stored in map '" + liftRide + "'");

      });
      System.out.println(" [x] Done");
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    boolean autoAck = true;
    channel.basicConsume(TASK_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });
    System.out.println(" [*] Message Consumed");

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down gracefully...");
      try {
        if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
          System.out.println("Executor did not terminate in the specified time.");
          List<Runnable> droppedTasks = executor.shutdownNow();
          System.out.println("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
        }
      } catch (InterruptedException e) {
        System.out.println("Executor was interrupted during shutdown.");
        executor.shutdownNow();
      }
      try {
        channel.close();
        connection.close();
      } catch (Exception e) {
        System.out.println("Error closing connection/channel.");
      }
    }));

    Thread.currentThread().join();
  }
}
