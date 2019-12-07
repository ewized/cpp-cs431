/**
  Joshua Rodriguez
  May 17th 2018

 -- How Custom Works --
 I call it the zig zag, since we will have 5 in the queue it will sort that, then the incoming batch with then be sorted in reverse.
 This sort is only done when new requests are coming in.
 Based off of the average if the two lists it will reverse one of them to have the zig zag flow better.
 While it processes the values the disk arm will zig zag along the disk.

 */
import java.util.*;

public class DiskArm {
  enum Direction { UP, DOWN } // used for the elevator
  private static final Random RANDOM = new Random();
  private static Report ssfReport = new Report("SSF");
  private static Report evReport = new Report("Elevator");
  private static Report cstReport = new Report("Custom");

  public static int[] generateNumbers(int length) {
    return generateNumbers(length, 1 , 100);
  }

  private static int[] generateNumbers(int length, int min, int max) {
    int[] newArray = new int[length];
    while (length > 0) {
      newArray[--length] = RANDOM.nextInt(max) + min;
    }
    return newArray;
  }

  private static void debugMasterList(int[] list) {
    System.out.print("Master List (" + list.length + "):");
    for (int x : list) {
      System.out.print(" " + x);
    }
    System.out.println();
  }

  private static int avg(List<SeqRequest> avg) {
    if (avg.size() == 0) {
      return 0;
    }
    int total = 0;
    for (SeqRequest seq : avg) {
      total += seq.cylinder;
    }
    return total / avg.size();
  }

  /** since cylinder needs to be final to sort by distance we pass it in using a lambda consumer functional interface */
  private static void sort(List<SeqRequest> requests, final int cylinder) {
    requests.sort(Comparator.comparingInt(left -> Math.abs(left.cylinder - cylinder)));
  }

  /** This implements and run the first come first served */
  private static int firstComeFirstServed(int[] list) {
    int total = 0;
    int lastVal = 50;
    for (int x : list) {
      total += Math.abs(lastVal - x);
      lastVal = x;
    }
    return total;
  }

  /** This implements and runs the shortestSeekFirst */
  private static int shortestSeekFirst(int[] list) {
    int lastOfList = 0; // used to keep track of the master list
    int time = 0;
    int cylinder = 50;
    // the simulation
    final LinkedList<SeqRequest> requests = new LinkedList<>();
    do {
      // insert the first 10 in the simulation, then when ever its less than 10
      if (requests.size() < 5) {
        for (int i = lastOfList ; i < list.length && i < lastOfList + 10 ; i++) {
          requests.add(new SeqRequest(time, list[i]));
        }
        lastOfList += 10;
      }
      // process each request
      sort(requests, cylinder);
      SeqRequest seq = requests.pop();
      seq.timeDone = time;
      time += Math.abs(cylinder - seq.cylinder);
      cylinder = seq.cylinder;
      ssfReport.add(seq);
    } while (lastOfList <= list.length || requests.size() != 0);
    return time;
  }

  /** This implements and runs the elevator */
  private static int elevator(int[] list) {
    //if (true) return 0;
    Direction direction = Direction.UP;
    int lastOfList = 0; // used to keep track of the master list
    int time = 0;
    int cylinder = 50;
    // the simulation
    final LinkedList<SeqRequest> requests = new LinkedList<>();
    do {
      // insert the first 10 in the simulation, then when ever its less than 10
      if (requests.size() < 5) {
        for (int i = lastOfList ; i < list.length && i < lastOfList + 10 ; i++) {
          requests.add(new SeqRequest(time, list[i]));
        }
        lastOfList += 10;
      }
      // process each request
      LinkedList<SeqRequest> otherWay = new LinkedList<>();
      for (SeqRequest request : requests) { // split into two directions
        if (direction == Direction.UP && cylinder < request.cylinder) {
          // going in the same direction
          otherWay.add(request);
        } else if (direction == Direction.DOWN && cylinder > request.cylinder) {
          otherWay.add(request);
        }
      }
      for (SeqRequest request : otherWay) {
        requests.remove(request); // remove from first list
      }
      Collections.sort(requests);
      Collections.sort(otherWay);
      Collections.reverse(direction == Direction.DOWN ? otherWay : requests);
      //requests.addAll(otherWay);
      // Process the requests in the same direction first
      for (SeqRequest request : otherWay) {
        request.timeDone = time;
        time += Math.abs(cylinder - request.cylinder);
        cylinder = request.cylinder;
        evReport.add(request);
      }
      direction = direction == Direction.DOWN ? Direction.UP : Direction.DOWN; // flip the direction and process the other list
      if (requests.size() != 0) { // safety check for pop
        SeqRequest seq = requests.pop();
        seq.timeDone = time;
        time += Math.abs(cylinder - seq.cylinder);
        cylinder = seq.cylinder;
        evReport.add(seq);
      }
    } while (lastOfList <= list.length || requests.size() != 0);
    return time;
  }

  /** This implements and runs the custom algo */
  private static int custom(int[] list) {
    int lastOfList = 0; // used to keep track of the master list
    int time = 0;
    int cylinder = 50;
    // the simulation
    final LinkedList<SeqRequest> requests = new LinkedList<>();
    do {
      // insert the first 10 in the simulation, then when ever its less than 10
      final LinkedList<SeqRequest> buffer = new LinkedList<>();
      if (requests.size() < 5) {
        for (int i = lastOfList ; i < list.length && i < lastOfList + 10 ; i++) {
          buffer.add(new SeqRequest(time, list[i]));
        }
        lastOfList += 10;

        // my custom
        sort(requests, cylinder);
        sort(buffer, cylinder);
        if (avg(requests) < avg(buffer)) {
          Collections.reverse(buffer);
          requests.addAll(buffer);
        } else {
          Collections.reverse(requests);
          buffer.addAll(requests);
          requests.clear();
          requests.addAll(buffer);
        }
      }
      //System.out.println(requests);
      // do it
      SeqRequest seq = requests.pop();
      seq.timeDone = time;
      time += Math.abs(cylinder - seq.cylinder);
      cylinder = seq.cylinder;
      cstReport.add(seq);
    } while (lastOfList <= list.length || requests.size() != 0);
    return time;
  }

  /** The class for this request */
  static class SeqRequest implements Comparable<SeqRequest> {
    private UUID uuid = UUID.randomUUID();
    private int timeEntered;
    private int timeDone;
    private int cylinder;

    public SeqRequest(int timeEntered, int cylinder) {
      this.timeEntered = timeEntered;
      this.cylinder = cylinder;
    }

    /** The delay as defined in the prompt */
    public int delay() {
      return this.timeDone - this.timeEntered;
    }

    /** The score as defined in the prompt */
    public double score() {
      return this.delay() * Math.sqrt(this.delay());
    }

    @Override
    public int compareTo(SeqRequest o) {
      return this.cylinder - o.cylinder;
    }

    @Override
    public String toString() {
      return String.valueOf(this.cylinder);
    }

    @Override
    public int hashCode() {
      return uuid.hashCode();
    }
  }

  /** Runs a report on the tasks */
  static class Report {
    private String name;
    private List<SeqRequest> requests = new ArrayList<>();

    public Report(String name) {
      this.name = name;
    }

    public void add(SeqRequest request) {
      requests.add(request);
    }

    @Override
    public String toString() {
      double maxScore = 0;
      double totalScore = 0;
      int maxDelay = 0;
      int totalDelay = 0;
      for (SeqRequest request : requests) {
        totalScore += request.score();
        totalDelay += request.delay();
        if (maxDelay < request.delay()) {
          maxDelay = request.delay();
        }
        if (maxScore < request.score()) {
          maxScore = request.score();
        }
      }
      return new StringBuilder()
        .append(name)
        .append("(")
        .append(requests.size())
        .append(")")
        .append(": ")
        .append("max score: ")
        .append(String.format("%.2f", maxScore))
        .append(", ")
        .append("avg score: ")
        .append(String.format("%.2f", requests.size() > 0 ? totalScore / requests.size() : 0))
        .append(", ")
        .append("max delay: ")
        .append(String.format("%d", maxDelay))
        .append(", ")
        .append("avg delay: ")
        .append(String.format("%d", requests.size() > 0 ? totalDelay / requests.size() : 0))
        .toString();
    }
  }

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter max value for the list: ");
    int masterLength = scanner.nextInt();

    int[] master = generateNumbers(masterLength);
    debugMasterList(master);

    System.out.println(" -- Disk Movements -- ");
    System.out.println("First Come First Served: " + firstComeFirstServed(master));
    System.out.println("Shortest Seek First: " + shortestSeekFirst(master));
    System.out.println("Elevator: " + elevator(master));
    System.out.println("Custom: " + custom(master));

    System.out.println(" -- Reports -- ");
    System.out.println(ssfReport);
    System.out.println(evReport);
    System.out.println(cstReport);
  }
}
