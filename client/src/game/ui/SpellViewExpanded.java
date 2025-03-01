package game.ui;

import com.artemis.E;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import game.AOGame;
import game.handlers.AOAssetManager;
import game.handlers.SpellHandler;
import game.screens.GameScreen;
import game.utils.Colors;
import game.utils.Skins;
import game.utils.WorldUtils;
import shared.model.Spell;
import shared.util.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.artemis.E.E;

public class SpellViewExpanded extends Table {

    private static final int MAX_SPELLS = 25;
    public Optional<Spell> selected = Optional.empty();
    private Window spellTable;
    private List<SpellSlotEC> slotsEC = new ArrayList<>(MAX_SPELLS);
    private int base;

    private AOAssetManager assetManager;

    public SpellViewExpanded() {
        super(Skins.COMODORE_SKIN);
        assetManager = AOGame.getGlobalAssetManager();
        spellTable = new Window("", Skins.COMODORE_SKIN, "inventory");
        int columnsCounter = 1;
        for (int i = 0; i < MAX_SPELLS; i++) {
            SpellSlotEC slot = new SpellSlotEC(this, null);
            slotsEC.add(slot);

            if (columnsCounter < 5) {
                spellTable.add(slot).width(SpellSlotEC.SIZE).height(SpellSlotEC.SIZE);
            } else {
                spellTable.add(slot).width(SpellSlotEC.SIZE).height(SpellSlotEC.SIZE).row();
                columnsCounter = 0;
            }
            columnsCounter++;
        }
        add(spellTable);
        spellTable.toFront();
    }


    public void updateSpells() {
        WorldUtils.getWorld().ifPresent(world -> {
            SpellHandler spellHandler = world.getSystem(SpellHandler.class);
            Spell[] spells = spellHandler.getSpells();
            Spell[] spellsToShow = new Spell[MAX_SPELLS];
            System.arraycopy(spells, 0, spellsToShow, 0, Math.min(MAX_SPELLS, spells.length));
            for (int i = 0; i < MAX_SPELLS; i++) {
                slotsEC.get(i).setSpell(spellsToShow[i]);
            }
        });
    }

    public void newSpellAdd(int spellNum){
        AtomicBoolean present = new AtomicBoolean ( false );
        WorldUtils.getWorld().ifPresent(world -> {
            SpellHandler spellHandler = world.getSystem ( SpellHandler.class );
            Spell[] spells = spellHandler.getSpells();
            Spell[] spellsToShow = new Spell[MAX_SPELLS];
            Optional< Spell > newSpell = spellHandler.getSpell ( spellNum );
            newSpell.ifPresent ( spell1 -> {
                if(spells.length <= MAX_SPELLS ) {
                    for (Spell spell : spells) {
                        if (spell.equals(spell1)) {
                            present.set(true);
                        }
                    }
                    if (!present.get ( )) {
                        System.arraycopy(spells, 0, spellsToShow, 0, spells.length);
                        spellsToShow[spells.length] = spell1;
                        for (int i = 0; i < MAX_SPELLS; i++) {
                            slotsEC.get ( i ).setSpell ( spellsToShow[i] );
                        }
                        world.getSystem ( GUI.class ).getConsole ( ).addInfo ( assetManager.getMessages( Messages.SPELLS_ADD, spell1.getName(), Integer.toString(spells.length + 1) ) );
                    } else {
                        world.getSystem ( GUI.class ).getConsole ( ).addInfo ( assetManager.getMessages( Messages.SPELLS_ALREDY_KNOWN, spell1.getName() ) );
                    }
                }
                else {
                    world.getSystem ( GUI.class ).getConsole ( ).addInfo ( assetManager.getMessages( Messages.SPELLS_FULL ) );
                }
            });
        });
        E (GameScreen.getPlayer ()).getSpellBook ().addSpell ( spellNum );
        updateSpells ();
    }

    void selected(Spell spell) {
        selected = Optional.ofNullable(spell);
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

    public boolean isOver() {
        return Stream.of(spellTable.getChildren().items)
                .filter(SpellSlotEC.class::isInstance)
                .map(SpellSlotEC.class::cast)
                .anyMatch(SpellSlotEC::isOver);
    }

    public Spell getSelected() {
        return selected.orElse(null);
    }
}
