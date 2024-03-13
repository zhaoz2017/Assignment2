package model;

public class LiftRideEvent {
  private final int skierId;
  private final int resortId;
  private final int liftId;
  private final String seasonId;
  private final String dayId;
  private final int time;

  public LiftRideEvent(int skierId, int resortId, int liftId, String seasonId, String dayId, int time) {
    this.skierId = skierId;
    this.resortId = resortId;
    this.liftId = liftId;
    this.seasonId = seasonId;
    this.dayId = dayId;
    this.time = time;
  }

  public int getSkierId() {
    return skierId;
  }

  public int getResortId() {
    return resortId;
  }

  public int getLiftId() {
    return liftId;
  }

  public String getSeasonId() {
    return seasonId;
  }

  public String getDayId() {
    return dayId;
  }

  public int getTime() {
    return time;
  }

}
