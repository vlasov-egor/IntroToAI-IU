import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

class GameOverException extends Exception {
};

class ItemConsumedException extends Exception {
    Cell itemType;

    ItemConsumedException(Cell type) {
        this.itemType = type;
    }
};

class Vector {
    public int x;
    public int y;

    Vector(int x, int y) {
        this.x = x;
        this.y = y;
    };

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Vector vec = (Vector) obj;
        return vec.x == this.x && vec.y == this.y;
    }

    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }
}

enum Cell {
    DNG(0b00000001), // Dangerous
    EXT(0b00000010), // Exit
    INV(0b00000100), // Invisiblity Cloak
    BOK(0b00001000), // Book
    INS(0b00010000), // Inspector
    HRY(0b00100000), // Harry
    VT2(0b10000000), // Visited and do not have unexplored cells around
    VT1(0b01000000); // Visited but still have unexplored cells around

    private final int mask;

    Cell(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    public boolean is(int cell) {
        return (cell & this.mask) == this.mask;
    }

    public String emoji() {
        switch (this) {
            case DNG:
                return "🟧";
            case EXT:
                return "🟩";
            case INV:
                return "🟪";
            case BOK:
                return "🟫";
            case INS:
                return "🟥";
            case HRY:
                return "🟦";
            case VT1:
                return "🔲";
            case VT2:
                return "⬛️";
            default:
                return "?";
        }
    }
}

class PerceptionField {
    static List<Vector> squareWithRadius(int radius, boolean skipZero) {
        List<Vector> field = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x == 0 && y == 0 && skipZero)
                    continue;
                field.add(new Vector(x, y));
            }
        }

        return field;
    }

    static List<Vector> harryVariant1() {
        return squareWithRadius(1, false);
    }

    static List<Vector> harryVariant2() {
        return List.of(
                new Vector(-2, -1),
                new Vector(-2, 0),
                new Vector(-2, 1),
                new Vector(2, -1),
                new Vector(2, 0),
                new Vector(2, 1),
                new Vector(-1, 2),
                new Vector(0, 2),
                new Vector(1, 2),
                new Vector(-1, -2),
                new Vector(0, -2),
                new Vector(1, -2));
    }
}

class MapUtils {
    static void draw(Map map) {
        for (int y = 8; y >= 0; y--) {
            for (int x = 0; x < 9; x++) {
                Vector pos = new Vector(x, y);

                if (map.containsKey(pos)) {
                    int cell = map.get(pos);

                    for (Cell cellType : Cell.values()) {
                        if (cellType.is(cell)) {
                            System.out.print(cellType.emoji());
                            break;
                        }
                    }
                } else {
                    System.out.print("⬜️");
                }
            }
            System.out.println("");
        }
    }
}

class Map {
    public HashMap<Vector, Integer> map;

    Map() {
        this.map = new HashMap<Vector, Integer>();
    }

    public void clear() {
        this.map.clear();
    }

    public void put(Vector v, Integer mask) {
        if (this.map.containsKey(v)) {
            this.map.put(v, map.get(v) | mask);
        } else {
            this.map.put(v, mask);
        }
    }

    public Integer get(Vector v) {
        return this.map.get(v);
    }

    public boolean containsKey(Vector v) {
        return this.map.containsKey(v);
    }

    public Map clone() {
        Map cl = new Map();
        cl.map = (HashMap<Vector, Integer>) this.map.clone();

        return cl;
    }
}

class Game {
    public Map map;
    public List<Actor> actors;

    private HarryActor harry;

    Game(List<Actor> actors) {
        this.map = new Map();
        this.actors = actors;

        this.harry = (HarryActor) this.actors.stream().filter(actor -> (actor instanceof HarryActor)).findFirst().get();
    }

    public void gameOver() {
        System.out.println("Мунир пососи мои яйца\nТы проебал сучара");
    }

    public void run() {
        runLoop: for (int i = 0; i < 10; i++) {
            this.map.clear();
            this.actors
                    .stream()
                    .forEach(actor -> actor.buildMap(this.map));

            for (Actor actor : this.actors) {
                try {
                    actor.proceedPerception(this.map);
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof GameOverException) {
                        System.out.println("МЫ ПОСОСАЛИИ ЮХУУУУ");
                    } else if (e.getCause() instanceof ItemConsumedException) {
                        ItemConsumedException consumedItem = (ItemConsumedException) e.getCause();

                        harry.consumeItem(consumedItem.itemType);
                    }
                    break runLoop;
                } catch (Exception e) {
                    e.printStackTrace();
                    break runLoop;
                }
            }

            for (Actor actor : this.actors) {
                try {
                    actor.proceedMovement();
                } catch (GameOverException e) {
                    System.out.println("МЫ ПОСОСАЛИИ ЮХУУУУ");
                    break runLoop;
                } catch (Exception e) {
                    e.printStackTrace();
                    break runLoop;
                }
            }

            drawMap();

            System.out.println("Harry");
            ((HarryActor) this.actors.get(0)).drawMap();
        }
    }

    public void drawMap() {
        MapUtils.draw(this.map);
    }
}

abstract class Actor {
    List<Vector> perceptionField;
    Vector position;

    abstract void buildMap(Map map);

    abstract void proceedPerceptionCell(Vector position, int cell) throws Exception;

    abstract void proceedMovement() throws Exception;

    void proceedPerception(Map map) throws Exception {
        perceptionField
                .stream()
                .map(relative -> new Vector(relative.x + this.position.x, relative.y + this.position.y))
                .filter(absolute -> map.containsKey(absolute))
                .forEach(absolute -> {
                    try {
                        this.proceedPerceptionCell(absolute,
                                map.get(absolute) & (~Cell.BOK.mask()) & (~Cell.INV.mask()));
                    } catch (GameOverException e) {
                        throw new RuntimeException(e);
                    } catch (ItemConsumedException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}

class DangerActor extends Actor {
    DangerActor(Vector position, int perceptionRadius) {
        this.perceptionField = PerceptionField.squareWithRadius(perceptionRadius, true);
        this.position = position;
    }

    @Override
    void buildMap(Map map) {
        this.perceptionField
                .stream()
                .map(relative -> new Vector(relative.x + this.position.x, relative.y + this.position.y))
                .filter(absolute -> absolute.x >= 0 && absolute.x < 9 && absolute.y >= 0 && absolute.y < 9)
                .forEach(absolute -> map.put(absolute, Cell.DNG.mask()));
        map.put(this.position, Cell.INS.mask());
    }

    @Override
    void proceedMovement() {
        return;
    }

    @Override
    void proceedPerceptionCell(Vector position, int cell) throws GameOverException {
        if (Cell.HRY.is(cell)) {
            throw new GameOverException();
        }
    }
}

class ItemActor extends Actor {
    Cell type;

    ItemActor(Vector position, Cell type) {
        this.type = type;
        this.position = position;
        this.perceptionField = PerceptionField.squareWithRadius(0, false);
    }

    @Override
    void proceedMovement() {
        return;
    }

    @Override
    void proceedPerceptionCell(Vector position, int cell) throws ItemConsumedException {
        if (Cell.HRY.is(cell)) {
            throw new ItemConsumedException(this.type);
        }
    }

    @Override
    void buildMap(Map map) {
        map.put(this.position, this.type.mask());
    }
}

class HarryActor extends Actor {
    ArrayList<Vector> totalPath;
    Stack<Vector> explorePath;
    Vector exitPosition;
    List<Vector> inspectorPositions;

    Map perceptionMap;
    Map movementsMap;

    HarryActor(Vector position, List<Vector> perceptionField, Vector exitPosition) {
        this.perceptionMap = new Map();
        this.movementsMap = new Map();
        this.position = position;
        this.perceptionField = perceptionField;

        this.totalPath = new ArrayList<Vector>();
        this.totalPath.add(position);
        this.explorePath = new Stack<Vector>();
        this.explorePath.push(this.position);

        this.exitPosition = exitPosition;

        this.inspectorPositions = new ArrayList<Vector>();
    }

    public void consumeItem(Cell type) {
        System.out.println("Схавал " + type.emoji());
    }

    @Override
    void proceedPerceptionCell(Vector position, int rcell) throws ItemConsumedException {
        int cell = rcell & (~Cell.HRY.mask());

        if (cell == 0) {
            return;
        }

        this.perceptionMap.put(position, cell);

        if (Cell.INS.is(cell) && !this.inspectorPositions.contains(position)) {
            this.inspectorPositions.add(position);
        }
    }

    @Override
    void proceedMovement() throws GameOverException {
        this.movementsMap.put(this.position, Cell.VT1.mask());

        List<Vector> variants1 = PerceptionField.squareWithRadius(1, true)
                .stream()
                .map(relative -> new Vector(relative.x + this.position.x, relative.y + this.position.y))
                .filter(absolute -> absolute.x >= 0 && absolute.x < 9 && absolute.y >= 0
                        && absolute.y < 9)
                .filter(absolute -> {
                    boolean flag = true;

                    if (this.perceptionMap.containsKey(absolute)) {
                        int cell = this.perceptionMap.get(absolute);

                        flag = flag && (!Cell.DNG.is(cell));
                    }

                    if (this.movementsMap.containsKey(absolute)) {
                        int cell = this.movementsMap.get(absolute);

                        flag = flag && (!Cell.VT2.is(cell));
                    }

                    return flag;
                })
                .collect(Collectors.toList());

        List<Vector> variants2 = variants1
                .stream()
                .filter(absolute -> {
                    if (this.movementsMap.containsKey(absolute)) {
                        int cell = this.movementsMap.get(absolute);

                        return !Cell.VT1.is(cell);
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toList());

        if (variants2.size() == 0) {
            this.movementsMap.put(this.position, Cell.VT2.mask());
        }

        if (variants1.size() == 0 && variants2.size() == 0) {
            if (this.explorePath.empty()) {
                throw new GameOverException();
            }

            Vector stepBack = this.explorePath.pop();

            this.position = stepBack;
            this.totalPath.add(this.position);
        } else if (variants2.size() == 0) {
            Vector acceptedVariant = variants1.get(0);

            this.position = acceptedVariant;
            this.totalPath.add(this.position);
            this.explorePath.push(this.position);
        } else {
            Vector acceptedVariant = variants2.get(0);

            this.position = acceptedVariant;
            this.totalPath.add(this.position);
            this.explorePath.push(this.position);
        }
    }

    @Override
    void buildMap(Map map) {
        map.put(this.position, Cell.HRY.mask());
    }

    void drawMap() {
        Map clone = (Map) this.movementsMap.clone();
        clone.put(this.position, Cell.HRY.mask());

        MapUtils.draw(clone);
        System.out.println("");
        MapUtils.draw(this.perceptionMap);
    }
}

// class MapRandomGenerator {
// // Todo: это говно надо переделать
// public List<Actor> generateActors(int harryPerceptionVariant) {
// Random random = new Random();

// int x_filch = random.nextInt(9);
// int y_filch = random.nextInt(9);
// DangerActor filch = new DangerActor(new Vector(x_filch, y_filch), 2);

// int x_norris = x_filch;
// int y_norris = y_filch;
// // while ()
// x_norris = random.nextInt(9);
// y_norris = random.nextInt(9);
// DangerActor norris = new DangerActor(new Vector(2, 7), 1);

// HarryActor harry = new HarryActor(new Vector(0, 0),
// PerceptionField.harryVariant2());

// ItemActor cloak = new ItemActor(new Vector(0, 8), Cell.INV);
// ItemActor exit = new ItemActor(new Vector(1, 4), Cell.EXT);
// ItemActor book = new ItemActor(new Vector(7, 4), Cell.BOK);

// return List.of(
// harry,
// norris,
// filch,
// cloak,
// exit,
// book);
// }
// }

public class EgorVlasov {
    public static void main(String[] args) throws Exception {
        DangerActor norris = new DangerActor(new Vector(2, 7), 1);
        DangerActor filch = new DangerActor(new Vector(4, 2), 2);
        ItemActor cloak = new ItemActor(new Vector(0, 8), Cell.INV);
        ItemActor book = new ItemActor(new Vector(7, 4), Cell.BOK);
        ItemActor exit = new ItemActor(new Vector(1, 4), Cell.EXT);
        HarryActor harry = new HarryActor(new Vector(0, 0), PerceptionField.harryVariant2(), exit.position);

        Game game = new Game(List.of(
                harry,
                norris,
                filch,
                cloak,
                exit,
                book));

        game.run();
    }
}
