package game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import game.utils.Resources;
import game.utils.Skins;
import shared.model.Spell;

public class SpellSlot extends ImageButton {

    static final int SIZE = 64;
    private static final float ICON_ALPHA = 0.9f;
    private static Drawable selection = Skins.COMODORE_SKIN.getDrawable("slot-selected2");
    private final SpellView spellView;
    private final ClickListener clickListener;
    private Spell spell;
    private Texture icon;
    private Tooltip tooltip;

    SpellSlot(SpellView spellView, Spell spell) {
        super(Skins.COMODORE_SKIN, "icon-container");
        this.spellView = spellView;
        clickListener = new ClickListener() {

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                onClick();
            }
        };
        addListener(clickListener);
    }

    public void setSpell(Spell spell) {
        this.spell = spell;
        if (spell == null) {
            return;
        }
        if (tooltip != null) {
            removeListener(tooltip);
        }
        tooltip = getTooltip(spell);
        addListener(tooltip);
    }

    public Spell getSpell() {
        return spell;
    }

    private Tooltip getTooltip(Spell spell) {
        Actor content = createTooltipContent(spell);
        Tooltip tooltip = new Tooltip(content);

        return tooltip;
    }

    private Actor createTooltipContent(Spell spell) {
        String name = spell.getName();
        String desc = spell.getDesc();
        int minhp = spell.getMinHP();
        int maxhp = spell.getMaxHP();
        int requiredMana = spell.getRequiredMana();
        int requiredSkills = spell.getMinSkill();

        Table table = new Window("", Skins.COMODORE_SKIN);
        table.defaults().growX();
        table.pad(10);
        Skins.COMODORE_SKIN.getFont("info").getData().markupEnabled = true;
        Label title = new Label(name, Skins.COMODORE_SKIN, "title-no-background");
        Label magic = new Label("Requiere [GRAY]" + requiredSkills + "[] puntos de Magia.", Skins.COMODORE_SKIN, "desc-no-background");
        Label mana = new Label("Requiere [CYAN]" + requiredMana + "[] puntos de Maná.", Skins.COMODORE_SKIN, "desc-no-background");
        Label damage = new Label("Inflinge entre [RED]" + minhp + " [](+DañoBase)" + "/[RED]" + maxhp + "[] (+DañoBase)", Skins.COMODORE_SKIN, "desc-no-background");
        Label description = new Label(desc, Skins.COMODORE_SKIN, "desc-no-background");
        description.setWrap(true);

        table.add(title).row();
        table.add(magic).row();
        table.add(mana).left().row();
        table.add(damage).left().row();
        description.setWidth(table.getPrefWidth());
        table.add(description).row();

        return table;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (spell == null) {
            return;
        }
        drawSpell(batch);
        spellView.selected.filter(sp -> sp.equals(spell)).ifPresent(sp -> drawSelection(batch));
    }

    private void drawSelection(Batch batch) {
        selection.draw(batch, getX(), getY(), SIZE, SIZE);
    }

    private void drawSpell(Batch batch) {
        Texture graphic = getSpellIcon();
        Color current = new Color(batch.getColor());
        batch.setColor(current.r, current.g, current.b, ICON_ALPHA);
        batch.draw(graphic, getX() + 1, getY() + 1);
        batch.setColor(current);
    }

    private void onClick() {
        spellView.selected(spell);
    }

    public boolean isOver() {
        return clickListener != null && clickListener.isOver();
    }

    private Texture getSpellIcon() {
        if (icon == null) {
            icon = new Texture(Gdx.files.local(Resources.GAME_SPELL_ICONS_PATH + spell.getId() + ".png"));
        }
        return icon;
    }

}
