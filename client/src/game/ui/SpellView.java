package game.ui;

import com.artemis.E;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import game.handlers.SpellHandler;
import game.screens.GameScreen;
import game.utils.Colors;
import game.utils.Cursors;
import game.utils.Skins;
import game.utils.WorldUtils;
import shared.model.Spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.artemis.E.E;

public class SpellView extends Table {

    public static final int MAX_SPELLS = 6;
    public Optional<Spell> toCast = Optional.empty();
    public Optional<Spell> selected = Optional.empty();
    private ImageButton castButton;
    private Window spellTable;
    private List<SpellSlot> slots = new ArrayList<>(MAX_SPELLS);
    private int base;

    public SpellView() {
        super(Skins.COMODORE_SKIN);
        spellTable = new Window("", Skins.COMODORE_SKIN, "inventory");
        for (int i = 0; i < MAX_SPELLS; i++) {
            SpellSlot slot = new SpellSlot(this, null);
            slots.add(slot);
            spellTable.add(slot).width(SpellSlot.SIZE).height(SpellSlot.SIZE).row();
            if (i < MAX_SPELLS - 1) {
                spellTable.add(new Image(getSkin().getDrawable("separator"))).row();
            }
        }
        castButton = createCastButton();
        add(castButton).padRight(-25f);
        add(spellTable).right();
        spellTable.toFront();
    }

    private void changeCursor() {
        WorldUtils.getWorld().ifPresent(world -> {
            world.getSystem(GUI.class).getConsole().addInfo("Haz click para lanzar el hechizo");
        });
        Cursors.setCursor("select");
    }

    public void updateSpells() {
        WorldUtils.getWorld().ifPresent(world -> {
            SpellHandler spellHandler = world.getSystem(SpellHandler.class);
            final Spell[] spells = spellHandler.getSpells();
            Spell[] spellsToShow = new Spell[MAX_SPELLS];
            System.arraycopy(spells, base, spellsToShow, 0, Math.min(MAX_SPELLS, spells.length));
            for (int i = 0; i < MAX_SPELLS; i++) {
                slots.get(i).setSpell(spellsToShow[i]);
            }
        });
    }

    private ImageButton createCastButton() {
        ImageButton staff = new ImageButton(Skins.COMODORE_SKIN, "staff");
        staff.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selected.ifPresent(spell -> preparedToCast(spell));
                super.clicked(event, x, y);
            }
        });
        return staff;
    }

    void selected(Spell spell) {
        selected = Optional.ofNullable(spell);
    }

    void preparedToCast(Spell spell) {
        toCast = Optional.ofNullable(spell);
        changeCursor();
    }

    public void cast() {
        selected.ifPresent(this::preparedToCast);
    }

    public boolean isOver() {
        return Stream.of(spellTable.getChildren().items)
                .filter(SpellSlot.class::isInstance)
                .map(SpellSlot.class::cast)
                .anyMatch(SpellSlot::isOver) || castButton.isOver();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        int player = GameScreen.getPlayer();
        Color backup = batch.getColor();
        if (player >= 0) {
            E e = E(player);
            if (e != null && e.hasAttack()) {
                batch.setColor(Colors.COMBAT);
            }
        }
        super.draw(batch, parentAlpha);
        batch.setColor(backup);
    }

    public void cleanCast() {
        toCast = Optional.empty();
        castButton.setChecked(false);
    }

    public void selectDown() {
        if (!selected.isPresent()) {
            selected = Optional.ofNullable(slots.get(0).getSpell());
            return;
        }
        selected.ifPresent(spell -> {
            for (int i = 0; i < slots.size(); i++) {
                if (spell.equals(slots.get(i).getSpell())) {
                    // found selected
                    if (i+1 < slots.size()) {
                        selected = Optional.ofNullable(slots.get(i+1).getSpell());
                    }
                }
            }
        });
    }

    public void selectUp() {
        if (!selected.isPresent()) {
            selected = Optional.ofNullable(slots.get(0).getSpell());
            return;
        }
        selected.ifPresent(spell -> {
            for (int i = 0; i < slots.size(); i++) {
                if (spell.equals(slots.get(i).getSpell())) {
                    // found selected
                    if (i-1 >= 0) {
                        selected = Optional.ofNullable(slots.get(i-1).getSpell());
                    }
                }
            }
        });
    }

}
