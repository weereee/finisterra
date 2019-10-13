package game.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import game.utils.Skins;

import java.util.ArrayList;
import java.util.List;

public class SwitchButtons extends Table {

    private ImageButton inventory;
    private ImageButton spells;
    private List<ActionSwitchListener> listeners = new ArrayList<>();
    private State state = State.INVENTORY;

    SwitchButtons() {
        setBackground(Skins.COMODORE_SKIN.getDrawable("inventory-spells-window"));
        inventory = new ImageButton(Skins.COMODORE_SKIN, "inventory");
        spells = new ImageButton(Skins.COMODORE_SKIN, "spells");
        inventory.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggle(State.INVENTORY);
            }
        });
        spells.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggle(State.SPELLS);
            }
        });
        add(inventory);
        add(spells).padLeft(1);
        toggle(State.INVENTORY);
    }

    void addListener(ActionSwitchListener listener) {
        listeners.add(listener);
    }

    public void toggle(State state) {
        if (this.state == state) {
            return;
        }
        this.state = state;
        switch (state) {
            case SPELLS:
                inventory.setChecked(false);
                spells.setChecked(true);
                break;
            case INVENTORY:
                spells.setChecked(false);
                inventory.setChecked(true);
                break;
        }
        listeners.forEach(listener -> listener.notify(this.state));
    }

    public void toggle() {
        switch (state) {
            case INVENTORY:
                toggle(State.SPELLS);
                break;
            case SPELLS:
                toggle(State.INVENTORY);
                break;
        }

    }

    public State getState() {
        return state;
    }

    public enum State {
        INVENTORY,
        SPELLS
    }

    public interface ActionSwitchListener {
        void notify(State state);
    }
}
