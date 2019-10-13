package game.managers;

import com.artemis.E;
import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import game.AOGame;
import game.handlers.AOAssetManager;
import game.screens.GameScreen;
import game.systems.camera.CameraSystem;
import game.systems.network.TimeSync;
import game.systems.render.world.TargetRenderingSystem;
import game.ui.GUI;
import game.ui.SwitchButtons;
import game.utils.AOKeys;
import game.utils.AlternativeKeys;
import game.utils.Cursors;
import game.utils.WorldUtils;
import position.WorldPos;
import shared.model.AttackType;
import shared.model.Spell;
import shared.network.combat.AttackRequest;
import shared.network.combat.SpellCastRequest;
import shared.network.interaction.DropItem;
import shared.network.interaction.MeditateRequest;
import shared.network.interaction.TakeItemRequest;
import shared.network.interaction.TalkRequest;
import shared.network.inventory.ItemActionRequest;
import shared.util.Messages;

import java.util.Optional;

import static com.artemis.E.E;

public class AOInputProcessor extends Stage implements ControllerListener {

    public static boolean alternativeKeys = false;

    private GUI gui;
    private AOAssetManager assetManager;

    public AOInputProcessor(GUI gui) {
        this.gui = gui;
        this.assetManager = AOGame.getGlobalAssetManager();
        Controllers.addListener(this);
    }

    @Override
    public boolean scrolled(int amount) {
        if (gui.getActionBar().isOver()) {
            gui.getActionBar().scrolled(amount);
        } else {
            WorldUtils.getWorld().ifPresent(world -> {
                CameraSystem system = world.getSystem(CameraSystem.class);
                system.zoom(amount, CameraSystem.ZOOM_TIME);
            });
        }
        return super.scrolled(amount);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        boolean result = super.touchUp(screenX, screenY, pointer, button);
        if (gui.getActionBar().isOver()) {
            return result;
        }

        WorldUtils.getWorld().ifPresent(world -> WorldUtils.mouseToWorldPos().ifPresent(worldPos -> {
            shoot(world, worldPos);
        }));
        return result;
    }

    public void shoot(World world, WorldPos worldPos) {
        final Optional<Spell> toCast = gui.getSpellView().toCast;
        if (toCast.isPresent()) {
            Spell spell = toCast.get();
            E player = E.E(GameScreen.getPlayer());
            if (!player.hasAttack() || player.getAttack().interval - world.getDelta() < 0) {
                TimeSync timeSyncSystem = world.getSystem(TimeSync.class);
                long rtt = timeSyncSystem.getRtt();
                long timeOffset = timeSyncSystem.getTimeOffset();
                GameScreen.getClient().sendToAll(new SpellCastRequest(spell, worldPos, rtt + timeOffset));
                player.attack();
            } else {
                gui.getConsole().addWarning(assetManager.getMessages(Messages.CANT_ATTACK));
            }
            Cursors.setCursor("hand");
            gui.getSpellView().cleanCast();
        } else {
            WorldManager worldManager = world.getSystem(WorldManager.class);
            Optional<String> name = worldManager.getEntities()
                    .stream()
                    .filter(entity -> E(entity).hasWorldPos() && E(entity).getWorldPos().equals(worldPos))
                    .filter(entity -> E(entity).hasName())
                    .map(entity -> E(entity).getName().text)
                    .findFirst();
            if (name.isPresent()) {
                gui.getConsole().addInfo(assetManager.getMessages(Messages.SEE_SOMEONE, name.get()));
            } else {
                gui.getConsole().addInfo(assetManager.getMessages(Messages.SEE_NOTHING));
            }

        }
    }

    @Override
    public boolean keyUp(int keycode) {
        if (!gui.getDialog().isVisible()) {
            if (alternativeKeys) {
                doAlternativeActions(keycode);
            } else {
                doActions(keycode);
            }
        }
        switch (keycode) {
            case AlternativeKeys.TALK:
                toggleDialogText();
                break;
            case Input.Keys.F1:
                alternativeKeys = !alternativeKeys;
                break;
        }

        return super.keyUp(keycode);
    }


    private void doActions(int keycode) {
        switch (keycode) {
            case AOKeys.INVENTORY:
                toggleInventory();
                break;
            case AOKeys.SPELLS:
                toggleSpells();
                break;
            case AOKeys.MEDITATE:
                toggleMeditate();
                break;
            case AOKeys.DROP:
                dropItem();
                break;
            case AOKeys.TAKE:
                takeItem();
                break;
            case AOKeys.EQUIP:
                equip();
                break;
            case AOKeys.USE:
                use();
                break;
            case AOKeys.ATTACK_1:
                attack();
                break;
            case AOKeys.ATTACK_2:
                attack();
                break;
            case Input.Keys.L:
                gui.getActionBar().toggle();
                break;
            case Input.Keys.ESCAPE:
                // Disconnect & go back to LoginScreen
                AOGame game = (AOGame) Gdx.app.getApplicationListener();
                game.toLogin();
                break;
            case Input.Keys.F2:
                // Take a screenshot of the render.
                gui.takeScreenshot();
                break;
            case Input.Keys.F11:
                // Toggle between Windowed Mode and Fullscreen.
                gui.toggleFullscreen();
                break;
        }
    }

    private void doAlternativeActions(int keycode) {
        switch (keycode) {
            case AlternativeKeys.INVENTORY:
                toggleInventory();
                break;
            case AlternativeKeys.SPELLS:
                toggleSpells();
                break;
            case AlternativeKeys.MEDITATE:
                toggleMeditate();
                break;
            case AlternativeKeys.DROP:
                dropItem();
                break;
            case AlternativeKeys.TAKE:
                takeItem();
                break;
            case AlternativeKeys.EQUIP:
                equip();
                break;
            case AlternativeKeys.USE:
                use();
                break;
            case AlternativeKeys.ATTACK_1:
                attack();
                break;
            case AlternativeKeys.ATTACK_2:
                attack();
                break;
            case Input.Keys.ESCAPE:
                // Disconnect & go back to LoginScreen
                AOGame game = (AOGame) Gdx.app.getApplicationListener();
                game.toLogin();
                break;
            case Input.Keys.F2:
                // Take a screenshot of the render.
                gui.takeScreenshot();
                break;
            case Input.Keys.F11:
                // Toggle between Windowed Mode and Fullscreen.
                gui.toggleFullscreen();
                break;
        }
    }

    private void use() {
        gui.getInventory()
                .getSelected()
                .ifPresent(slot -> GameScreen.getClient().sendToAll(new ItemActionRequest(gui.getInventory().selectedIndex())));
    }

    private void attack() {
        E player = E(GameScreen.getPlayer());
        WorldUtils.getWorld().ifPresent(world -> {
            if (!player.hasAttack() || player.getAttack().interval - world.getDelta() <= 0) {
                GameScreen.getClient().sendToAll(new AttackRequest(AttackType.PHYSICAL));
                player.attack();
            }
        });
    }

    private void equip() {
        gui.getInventory()
                .getSelected()
                .ifPresent(slot -> {
                    int inventoryIndex = gui.getInventory().selectedIndex();
                    GameScreen.getClient().sendToAll(new ItemActionRequest(inventoryIndex));
                });
    }

    private void takeItem() {
        GameScreen.getClient().sendToAll(new TakeItemRequest());
    }

    // drop selected item (count 1 for the time being)
    private void dropItem() {
        gui.getInventory().getSelected().ifPresent(selected -> {
            int player = GameScreen.getPlayer();
            GameScreen
                    .getClient()
                    .sendToAll(new DropItem(E(player).getNetwork().id, gui.getInventory().selectedIndex(), E(player).getWorldPos()));
        });
    }

    private void toggleDialogText() {
        if (gui.getDialog().isVisible()) {
            String message = gui.getDialog().getMessage();
            GameScreen.getClient().sendToAll(new TalkRequest(message));
        }
        gui.getDialog().toggle();
        E(GameScreen.getPlayer()).writing(gui.getDialog().isVisible());
    }

    private void toggleMeditate() {
        GameScreen.getClient().sendToAll(new MeditateRequest());
    }

    private void toggleInventory() {
        gui.getInventory().setVisible(!gui.getInventory().isVisible());
    }

    private void toggleSpells() {
        gui.getSpellView().setVisible(!gui.getSpellView().isVisible());
    }

    @Override
    public void connected(Controller controller) {

    }

    @Override
    public void disconnected(Controller controller) {

    }


    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        switch (buttonCode) {
            case 4:
                // L1 - SELECT SPELL UP
                showSpellsIfNecessary();
                gui.getSpellView().selectUp();
                break;
            case 5:
                // R1 - SELECT SPELL DOWN
                showSpellsIfNecessary();
                gui.getSpellView().selectDown();
                break;
            case 6:
                // L2 - CAST
                gui.getSpellView().cast();
                break;
            case 7:
                // R2 - SHOOT
                WorldUtils.getWorld().ifPresent(world -> WorldUtils.controllerToWorldPos().ifPresent(pos -> shoot(world, pos)));

        }
        return false;
    }

    private void showSpellsIfNecessary() {
        SwitchButtons buttons = gui.getActionBar().getButtons();
        if (!buttons.getState().equals(SwitchButtons.State.SPELLS)) {
            gui.getActionBar().getButtons().toggle(SwitchButtons.State.SPELLS);
        }
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }

    /**
     * For Future Reference. PS4 Buttons to Code
     * Right Stick Up = Axis 0, Negative Value
     * Right Stick Down = Axis 0, Positive Value
     * Right Stick Left = Axis 1, Negative Value
     * Right Stick Right = Axis 1, Positive Value
     * Left Stick Up = Axis 2, Negative Value
     * Left Stick Down = Axis 2, Positive Value
     * Left Stick Left = Axis 3, Negative Value
     * Left Stick Right = Axis 3, Positive Value
     */
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (Math.abs(value) >= 0.5f) {
//            Log.info("Controller: " + controller);
//            Log.info("Axis Code: " + axisCode);
//            Log.info("Value: " + value);
            switch (axisCode) {
                case 0:
                    if (value > 0) {
                        // DOWN
                    } else {
                        // UP
                    }
                case 1:
                    if (value > 0) {
                        // RIGHT
                    } else {
                        // UP
                    }
                case 2:

                case 3:
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        return false;
    }

    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        return false;
    }
}
