package server.core;

import com.artemis.FluidEntityPlugin;
import com.artemis.World;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import server.network.ServerNotificationProcessor;
import server.network.ServerRequestProcessor;
import server.systems.*;
import server.systems.ai.NPCAttackSystem;
import server.systems.ai.PathFindingSystem;
import server.systems.ai.NPCRespawnSystem;
import server.systems.battle.PlayerRespawnSystem;
import server.systems.battle.SpotRegenerationSystem;
import server.systems.combat.MagicCombatSystem;
import server.systems.combat.PhysicalCombatSystem;
import server.systems.manager.*;
import shared.model.map.Map;

import java.util.HashMap;

import static server.systems.Intervals.*;

public class Server {

    private final int tcpPort;
    private final int udpPort;
    private int roomId;
    private ObjectManager objectManager;
    private SpellManager spellManager;
    private World world;
    private ServerStrategy strategy;
    private float tickTime;

    private final static float TICK_RATE = 0.0166f; // 60 ticks per second

    public Server(int roomId, int tcpPort, int udpPort, ObjectManager objectManager, SpellManager spellManager, HashMap<Integer, Map> maps) {
        this.roomId = roomId;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.objectManager = objectManager;
        this.spellManager = spellManager;
        create();
    }

    int getTcpPort() {
        return tcpPort;
    }

    int getUdpPort() {
        return udpPort;
    }

    int getRoomId() {
        return roomId;
    }

    private void create() {
        long start = System.currentTimeMillis();
        initWorld();
        Gdx.app.log("Server initialization", "Elapsed time: " + (start - System.currentTimeMillis()));
    }

    public World getWorld() {
        return world;
    }

    private void initWorld() {
        System.out.println("Initializing systems...");
        final WorldConfigurationBuilder builder = new WorldConfigurationBuilder();
        strategy = new ServerStrategy(tcpPort, udpPort);
        builder
                .with(new FluidEntityPlugin())
                .with(new ServerSystem(roomId, strategy))
                .with(new ServerNotificationProcessor())
                .with(new ServerRequestProcessor())
                .with(new EntityFactorySystem())
                .with(new ItemManager())
                .with(new ItemConsumers())
                .with(new NPCManager())
                .with(new MapManager())
                .with(spellManager)
                .with(objectManager)
                .with(new WorldManager())
                .with(new PhysicalCombatSystem())
                .with(new CharacterTrainingSystem())
                .with(new MagicCombatSystem())
                .with(new PathFindingSystem(PATH_FINDING_INTERVAL))
                .with(new NPCAttackSystem(NPC_ATTACK_INTERVAL))
                .with(new EnergyRegenerationSystem(ENERGY_REGENERATION_INTERVAL))
                .with(new MeditateSystem(MEDITATE_INTERVAL))
                .with(new FootprintSystem(FOOTPRINT_LIVE_TIME))
                .with(new RandomMovementSystem())
                .with(new NPCRespawnSystem())
                .with(new PlayerRespawnSystem())
                .with(new SpotRegenerationSystem())
                .with(new BuffSystem());
        world = new World(builder.build());
        System.out.println("World created!");
    }

    public void update() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        tickTime += deltaTime;
        if (tickTime > TICK_RATE) {
            world.setDelta(deltaTime);
            world.process();
            tickTime = tickTime - TICK_RATE;
        }
    }
}
