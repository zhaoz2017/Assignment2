public class LiftRide {
  private int time;
  private int liftId;
  private String resortId;
  private String seasonId;
  private String dayId;
  private int skierId;

  public LiftRide(int time, int liftId, String resortId, String seasonId, String dayId, int skierId) {
    this.time = time;
    this.liftId = liftId;
    this.resortId = resortId;
    this.seasonId = seasonId;
    this.dayId = dayId;
    this.skierId = skierId;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  public int getLiftId() {
    return liftId;
  }

  public void setLiftId(int liftId) {
    this.liftId = liftId;
  }

  public String getResortId() {
    return resortId;
  }

  public void setResortId(String resortId) {
    this.resortId = resortId;
  }

  public String getSeasonId() {
    return seasonId;
  }

  public void setSeasonId(String seasonId) {
    this.seasonId = seasonId;
  }

  public String getDayId() {
    return dayId;
  }

  public void setDayId(String dayId) {
    this.dayId = dayId;
  }

  public int getSkierId() {
    return skierId;
  }

  public void setSkierId(int skierId) {
    this.skierId = skierId;
  }

  @Override
  public String toString() {
    return "LiftRide{" +
        "time=" + time +
        ", liftId=" + liftId +
        ", resortId=" + resortId +
        ", seasonId='" + seasonId + '\'' +
        ", dayId='" + dayId + '\'' +
        ", skierId=" + skierId +
        '}';
  }
}
