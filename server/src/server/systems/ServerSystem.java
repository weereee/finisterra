package server.systems;

import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.minlog.Log;
import net.mostlyoriginal.api.network.marshal.common.MarshalStrategy;
import net.mostlyoriginal.api.network.system.MarshalSystem;
import server.core.ServerStrategy;
import server.network.NetworkJob;
import server.network.ServerNotificationProcessor;
import server.network.ServerRequestProcessor;
import server.systems.manager.MapManager;
import server.systems.manager.WorldManager;
import shared.model.lobby.Player;
import shared.network.init.NetworkDictionary;
import shared.network.interfaces.INotification;
import shared.network.interfaces.IRequest;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Wire
public class ServerSystem extends MarshalSystem {

    // Injected Systems
    private MapManager mapManager;
    private ServerNotificationProcessor notificationProcessor;
    private ServerRequestProcessor requestProcessor;
    private WorldManager worldManager;

    private Deque<NetworkJob> netQueue = new ConcurrentLinkedDeque<>();
    private Map<Integer, Integer> playerByConnection = new ConcurrentHashMap<>();
    private Map<Integer, Integer> connectionByPlayer = new ConcurrentHashMap<>();

    private Map<Integer, Player> lobbyPlayers = new ConcurrentHashMap<>();
    private int roomId;

    public ServerSystem(int roomId, MarshalStrategy strategy) {
        super(new NetworkDictionary(), strategy);
        this.roomId = roomId;
        start();
    }

    @Override
    public void received(int connectionId, Object object) {
        netQueue.add(new NetworkJob(connectionId, object));
    }

    private void processJob(NetworkJob job) {
        int connectionId = job.getConnectionId();
        Object object = job.getReceivedObject();
        try {
            if (object instanceof IRequest) {
                ((IRequest) object).accept(requestProcessor, connectionId);
            } else if (object instanceof INotification) {
                ((INotification) object).accept(notificationProcessor);
            }
        } catch (Exception e) {
            Log.error("Failed to process Job", e);
        }
    }

    @Override
    protected void processSystem() {
        super.processSystem();
        while (netQueue.peek() != null) {
            processJob(netQueue.poll());
        }
    }

    @Override
    public void disconnected(int connectionId) {
        super.disconnected(connectionId);
        if (connectionHasNoPlayer(connectionId)) {
            return;
        }
        Gdx.app.postRunnable(() -> {
            worldManager.unregisterEntity(getPlayerByConnection(connectionId));
        });
    }

    /**
     * Object will be serialized and sent using kryo
     *
     * @param id     connection ID
     * @param packet Object to send
     */
    public void sendTo(int id, Object packet) {
        ServerStrategy marshal = (ServerStrategy) getMarshal();
        marshal.sendTo(id, packet);
    }

    public void registerUserConnection(int playerId, int connectionId) {
        playerByConnection.put(connectionId, playerId);
        connectionByPlayer.put(playerId, connectionId);
    }

    public void unregisterUserConnection(int playerId) {
        if (playerHasConnection(playerId)) {
            int connectionId = getConnectionByPlayer(playerId);
            playerByConnection.remove(connectionId);
            lobbyPlayers.remove(connectionId);
            connectionByPlayer.remove(playerId);
        }
    }

    public boolean connectionHasNoPlayer(int connectionId) {
        return !playerByConnection.containsKey(connectionId);
    }

    public boolean playerHasConnection(int player) {
        return connectionByPlayer.containsKey(player);
    }

    public int getPlayerByConnection(int connectionId) {
        return playerByConnection.get(connectionId);
    }

    public int getConnectionByPlayer(int playerId) {
        return connectionByPlayer.get(playerId);
    }

    public int getRoomId() {
        return roomId;
    }

    public void registerPlayer(int connectionId, Player player) {
        lobbyPlayers.put(connectionId, player);
    }

    public Player getLobbyPlayer(int connectionId) {
        return lobbyPlayers.get(connectionId);
    }
}

