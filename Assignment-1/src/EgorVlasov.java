import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

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
}

enum Cell {
    DNG(0b000001), // Dangerous
    EXT(0b000010), // Exit
    INV(0b000100), // Invisiblity Cloak
    BOK(0b001000), // Book
    INS(0b010000), // Inspector
    HRY(0b100000); // Harry

    private final int mask;

    Cell(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
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
            default:
                return "⬛️";
        }
    }
}

class PerceptionField {
    static List<Vector> sqaureWithRadius(int radius) {
        List<Vector> field = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                field.add(new Vector(x, y));
            }
        }

        return field;
    }

    static List<Vector> harryVariant1() {
        return sqaureWithRadius(1);
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
    static void draw(HashMap<Vector, Integer> map) {
        System.out.println(map.values());

        for (int y = 8; y >= 0; y--) {
            for (int x = 0; x < 9; x++) {
                Vector pos = new Vector(x, y);

                if (map.containsKey(pos)) {
                    int cell = map.get(pos);

                    for (Cell cellType : Cell.values()) {
                        if ((cell & cellType.mask()) == cellType.mask()) {
                            System.out.print(cellType.emoji());
                            // Consider that cloak can't be in the same cell as book or exit
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

class Game {
    public HashMap<Vector, Integer> map;
    public List<Actor> actors;

    Game(List<Actor> actors) {
        this.map = new HashMap<Vector, Integer>();
        this.actors = actors;
    }

    public void gameOver() {
        System.out.println("Мунир пососи мои яйца\nТы проебал сучара");
    }

    public void buildMap() {
        this.actors
                .stream()
                .forEach(actor -> actor.buildMap(this.map));
    }

    public void proceedActors() {
        for (Actor actor : this.actors) {
            try {
                actor.proceedPerception(this.map);
            } catch (GameOverException e) {
                // пососус
            } catch (Exception e) {
                // полный пососус
            }
        }
    }

    public void drawMap() {
        MapUtils.draw(this.map);
    }
}

abstract class Actor {
    List<Vector> perceptionField;
    Vector position;

    abstract void buildMap(HashMap<Vector, Integer> map);

    abstract void proceedPerceptionCell(Vector position, int cell) throws Exception;

    void proceedPerception(HashMap<Vector, Integer> map) throws Exception {
        perceptionField
                .stream()
                .map(relative -> new Vector(relative.x + this.position.x, relative.y + this.position.y))
                .filter(absolute -> map.containsKey(absolute))
                .forEach(absolute -> {
                    try {
                        this.proceedPerceptionCell(absolute, map.get(absolute));
                    } catch (GameOverException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        System.out.println("Unhandled exception " + e.getMessage());
                    }
                });
    }
}

class DangerActor extends Actor {
    DangerActor(Vector position, int perceptionRadius) {
        this.perceptionField = PerceptionField.sqaureWithRadius(perceptionRadius);
        this.position = position;
    }

    @Override
    void buildMap(HashMap<Vector, Integer> map) {
        this.perceptionField
                .stream()
                .map(relative -> new Vector(relative.x + this.position.x, relative.y + this.position.y))
                .filter(absolute -> absolute.x >= 0 && absolute.x < 9 && absolute.y >= 0 && absolute.y < 9)
                .forEach(absolute -> map.put(absolute, Cell.DNG.mask()));
        map.put(this.position, Cell.INS.mask());
    }

    @Override
    void proceedPerceptionCell(Vector position, int cell) throws GameOverException {
        if ((cell & Cell.HRY.mask()) == Cell.HRY.mask()) {
            throw new GameOverException();
        }
    }
}

class ItemActor extends Actor {
    Cell type;

    ItemActor(Vector position, Cell type) {
        this.type = type;
        this.position = position;
        this.perceptionField = PerceptionField.sqaureWithRadius(0);
    }

    @Override
    void proceedPerceptionCell(Vector position, int cell) throws ItemConsumedException {
        if ((cell & Cell.HRY.mask()) == Cell.HRY.mask()) {
            throw new ItemConsumedException(this.type);
        }
    }

    @Override
    void buildMap(HashMap<Vector, Integer> map) {
        map.put(this.position, this.type.mask());
    }
}

class HarryActor extends Actor {
    HashMap<Vector, Integer> map = new HashMap<Vector, Integer>();

    HarryActor(Vector position, List<Vector> perceptionField) {
        this.map = new HashMap<Vector, Integer>();
        this.position = position;
        this.perceptionField = perceptionField;
    }

    @Override
    void proceedPerceptionCell(Vector position, int cell) throws ItemConsumedException {
        this.map.put(position, cell);
    }

    @Override
    void buildMap(HashMap<Vector, Integer> map) {
        map.put(this.position, Cell.HRY.mask());
    }

    void drawMap() {
        MapUtils.draw(this.map);
    }
}

public class EgorVlasov {
    public static void main(String[] args) throws Exception {
        HarryActor harry = new HarryActor(new Vector(0, 0), PerceptionField.harryVariant2());
        DangerActor norris = new DangerActor(new Vector(2, 7), 1);
        DangerActor filch = new DangerActor(new Vector(4, 2), 2);
        ItemActor cloak = new ItemActor(new Vector(0, 8), Cell.INV);
        ItemActor exit = new ItemActor(new Vector(1, 4), Cell.EXT);
        ItemActor book = new ItemActor(new Vector(7, 4), Cell.BOK);

        Game game = new Game(List.of(
                harry,
                norris,
                filch,
                cloak,
                exit,
                book));

        game.buildMap();

        System.out.println("Global map");
        game.drawMap();

        System.out.println("Harry map at step 1");
        game.proceedActors();
        harry.drawMap();
    }
}
