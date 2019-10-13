package server.systems.ai;

import com.artemis.Aspect;
import com.artemis.E;
import com.artemis.EBag;
import com.artemis.annotations.Wire;
import com.esotericsoftware.minlog.Log;
import entity.character.Character;
import entity.character.states.Immobile;
import entity.npc.AIMovement;
import entity.npc.NPC;
import entity.world.Footprint;
import movement.Destination;
import physics.AOPhysics;
import position.WorldPos;
import server.systems.IntervalFluidIteratingSystem;
import server.systems.manager.MapManager;
import server.systems.manager.WorldManager;
import server.utils.WorldUtils;
import shared.model.map.Map;
import shared.model.map.Tile;
import shared.network.movement.MovementNotification;
import shared.network.notifications.EntityUpdate;
import shared.util.MapHelper;

import java.util.*;

import static physics.AOPhysics.Movement.*;
import static server.utils.WorldUtils.WorldUtils;

@Wire
public class PathFindingSystem extends IntervalFluidIteratingSystem {

    private static final int MAX_DISTANCE_TARGET = 10;
    private MapManager mapManager;
    private HashMap<Integer, AStarMap> maps = new HashMap<>();

    public PathFindingSystem(float interval) {
        super(Aspect.all(NPC.class, WorldPos.class, AIMovement.class).exclude(Character.class, Footprint.class, Immobile.class), interval);
    }

    private AStarMap updateMap(Integer map) {
        Set<Integer> entitiesInMap = mapManager.getEntitiesInMap(map);
        if (entitiesInMap.stream().noneMatch(e -> E.E(e).isCharacter())) {
            return maps.get(map);
        }
        // TODO can we update on each move instead of create all again?
        AStarMap starMap = createStarMap(map);
        maps.put(map, starMap);
        return starMap;
    }

    @Override
    protected void begin() {
        mapManager.getMaps().forEach(this::updateMap);
    }

    @Override
    protected void process(E e) {
        WorldPos origin = e.getWorldPos();
        if (!maps.containsKey(origin.map)) {
            return;
        }
        AStarMap aStarMap = maps.get(origin.map);
        Optional<E> target1 = findTarget(origin);
        WorldPos targetPos = target1.map(E::getWorldPos).orElse(e.getOriginPos().toWorldPos());
        if (targetPos.equals(e.getWorldPos())) {
            return;
        } else if (!target1.isPresent() && WorldUtils.WorldUtils(world).distance(origin, e.getOriginPos().toWorldPos()) < 10) {
            return;
        }
        makeYourMove(e, origin, targetPos, aStarMap);
    }

    private void makeYourMove(E e, WorldPos origin, WorldPos targetPos, AStarMap map) {
        boolean originWasWall = map.getNodeAt(origin.x, origin.y).isWall;
        boolean targetWasWall = map.getNodeAt(targetPos.x, targetPos.y).isWall;
        map.getNodeAt(origin.x, origin.y).isWall = false;
        map.getNodeAt(targetPos.x, targetPos.y).isWall = false;
        AStartPathFinding aStartPathFinding = new AStartPathFinding(map);
        Node from = aStartPathFinding.map.getNodeAt(origin.x, origin.y);
        Node nextNode = aStartPathFinding.findNextNode(origin, targetPos);
        move(e, from, nextNode);
        map.getNodeAt(origin.x, origin.y).isWall = originWasWall;
        map.getNodeAt(targetPos.x, targetPos.y).isWall = targetWasWall;
    }

    private void move(E e, Node from, Node nextNode) {
        if (nextNode == null) {
            Log.info("Cant find next node");
            return;
        }
        int entityId = e.id();
        String text = entityId + " " + (e.hasName() ? e.getName().text : "NO NAME: " + entityId);
        if (nextNode.x - from.x > 0) {
            // move right
            moveEntity(entityId, RIGHT);
            Log.info(text + " AI MOVE RIGHT");
            Log.info(e.getWorldPos() + " ");
        } else if (nextNode.x - from.x < 0) {
            // move left
            moveEntity(entityId, LEFT);
            Log.info(text + " AI MOVE LEFT");
            Log.info(e.getWorldPos() + " ");
        } else if (nextNode.y - from.y > 0) {
            // move south
            moveEntity(entityId, DOWN);
            Log.info(text + " AI MOVE DOWN");
            Log.info(e.getWorldPos() + " ");
        } else {
            // move north
            moveEntity(entityId, UP);
            Log.info(text + "AI MOVE UP");
            Log.info(e.getWorldPos() + " ");
        }
    }

    private void moveEntity(int entityId, AOPhysics.Movement mov) {
        E player = E.E(entityId);

        WorldUtils worldUtils = WorldUtils(world);
        player.headingCurrent(worldUtils.getHeading(mov));

        WorldPos worldPos = player.getWorldPos();
        WorldPos oldPos = new WorldPos(worldPos);
        WorldPos nextPos = worldUtils.getNextPos(worldPos, mov);

        Map map = mapManager.getMap(nextPos.map);
        boolean blocked = mapManager.getHelper().isBlocked(map, nextPos);
        boolean occupied = mapManager.getHelper().hasEntity(mapManager.getNearEntities(entityId), nextPos);
        Tile tile = mapManager.getHelper().getTile(map, nextPos);
        if (player.hasImmobile() || blocked || occupied || (tile != null && tile.getTileExit() != null)) {
            nextPos = oldPos;
        }

        player.worldPosMap(nextPos.map);
        player.worldPosX(nextPos.x);
        player.worldPosY(nextPos.y);

        mapManager.movePlayer(entityId, Optional.of(oldPos));

        WorldManager worldManager = world.getSystem(WorldManager.class);
        // notify near users
        if (nextPos != oldPos) {
            worldManager.notifyUpdate(entityId, new MovementNotification(entityId, new Destination(nextPos, mov)));
        } else {
            worldManager.notifyUpdate(entityId, EntityUpdate.EntityUpdateBuilder.of(entityId).withComponents(player.getHeading()).build());
        }
    }

    private Optional<E> findTarget(WorldPos worldPos) {
        Set<E> all = new HashSet<>();
        EBag es = E.withAspect(Aspect.all(Character.class));
        es.forEach(all::add);
        return all.stream()
                .filter(E::hasWorldPos)
                .filter(e -> e.hasHealth() && e.getHealth().min > 0)
                .filter(e -> {
                    int distance = WorldUtils(world).distance(e.getWorldPos(), worldPos);
                    return distance < MAX_DISTANCE_TARGET && distance >= 0;
                })
                .min(Comparator.comparingInt(e -> WorldUtils(world).distance(e.getWorldPos(), worldPos)));
    }

    private AStarMap createStarMap(int map) {
        Map realMap = mapManager.getMap(map);
        int height = realMap.getHeight();
        int width = realMap.getWidth();

        AStarMap aMap = new AStarMap(width, height);
        MapHelper helper = mapManager.getHelper();
        Set<Integer> entitiesInMap = mapManager.getEntitiesInMap(map);
        for (int x = 1; x < width; x++) {
            for (int y = 1; y < height; y++) {
                Node nodeAt = aMap.getNodeAt(x, y);
                Tile tile = realMap.getTile(x, y);
                nodeAt.isWall = tile == null || helper.isBlocked(realMap, x, y) || helper.hasEntity(entitiesInMap, new WorldPos(x, y, map));
            }
        }

        return aMap;
    }

    public AStarMap getMap(int map) {
        return maps.get(map);
    }
}
