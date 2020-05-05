/*
 *
 *  * Pixel Dungeon
 *  * Copyright (C) 2012-2015 Oleg Dolya
 *  *
 *  * Shattered Pixel Dungeon
 *  * Copyright (C) 2014-2019 Evan Debenham
 *  *
 *  * Yet Another Shattered Dungeon
 *  * Copyright (C) 2014-2020 Samuel Braithwaite
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 *
 */

package com.shatteredpixel.yasd.general.actors.buffs;

import com.shatteredpixel.yasd.general.Assets;
import com.shatteredpixel.yasd.general.Dungeon;
import com.shatteredpixel.yasd.general.Element;
import com.shatteredpixel.yasd.general.actors.Actor;
import com.shatteredpixel.yasd.general.actors.Char;
import com.shatteredpixel.yasd.general.actors.hero.HeroAction;
import com.shatteredpixel.yasd.general.actors.mobs.Mob;
import com.shatteredpixel.yasd.general.actors.mobs.Rat;
import com.shatteredpixel.yasd.general.actors.mobs.npcs.NPC;
import com.shatteredpixel.yasd.general.effects.CellEmitter;
import com.shatteredpixel.yasd.general.effects.Effects;
import com.shatteredpixel.yasd.general.effects.Speck;
import com.shatteredpixel.yasd.general.messages.Messages;
import com.shatteredpixel.yasd.general.scenes.CellSelector;
import com.shatteredpixel.yasd.general.scenes.GameScene;
import com.shatteredpixel.yasd.general.ui.ActionIndicator;
import com.shatteredpixel.yasd.general.ui.BuffIndicator;
import com.shatteredpixel.yasd.general.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Preparation extends Buff implements ActionIndicator.Action {
	
	{
		//always acts after other buffs, so invisibility effects can process first
		actPriority = BUFF_PRIO - 1;
	}
	
	public enum AttackLevel{
		LVL_1( 1,  0.2f, 0.0f, 1, 0),
		LVL_2( 3,  0.4f, 0.0f, 1, 1),
		LVL_3( 6,  0.6f  , 0.0f, 2, 3),
		LVL_4( 11, 0.8f, 0.6f, 2, 5),
		LVL_5( 16, 2f, 1.0f, 3, 7);
		
		final int turnsReq;
		final float baseDmgBonus, missingHPBonus;
		final int damageRolls, blinkDistance;
		
		AttackLevel( int turns, float base, float missing, int rolls, int dist){
			turnsReq = turns;
			baseDmgBonus = base; missingHPBonus = missing;
			damageRolls =rolls; blinkDistance = dist;
		}
		
		public boolean canInstakill(Char defender){
			return this == LVL_5
					&& !defender.properties().contains(Char.Property.MINIBOSS)
					&& !defender.properties().contains(Char.Property.BOSS);
		}
		
		public int damageRoll( Char attacker, Char defender){
			int dmg = attacker.damageRoll();
			for( int i = 1; i < damageRolls; i++){
				int newDmg = attacker.damageRoll();
				if (newDmg > dmg) dmg = newDmg;
			}
			float defenderHPPercent = 1f - (defender.HP / (float)defender.HT);
			return Math.round(dmg * (1f + baseDmgBonus + (missingHPBonus * defenderHPPercent)));
		}
		
		public static AttackLevel getLvl(float turnsInvis){
			List<AttackLevel> values = Arrays.asList(values());
			Collections.reverse(values);
			for ( AttackLevel lvl : values ){
				if (turnsInvis >= lvl.turnsReq){
					return lvl;
				}
			}
			return LVL_1;
		}
	}
	
	private float turnsPrep = 0;

	public static boolean canAttatch(Char ch) {
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.getEnemy() == ch && mob.enemySeen) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean act() {
		/*if (target.invisible > 0){
			turnsPrep++;
			if (AttackLevel.getLvl(turnsPrep).blinkDistance > 0 && target == Dungeon.hero){
				ActionIndicator.setAction(this);
			}
			BuffIndicator.refreshHero();
			spend(TICK);
		} else {
			detach();
		}*/
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.getEnemy() == target && mob.enemySeen) {
				detach();
			} else if (target.fieldOfView[mob.pos] || target.notice(mob, false)) {
				turnsPrep++;
				spend(TICK);
				return true;
			}
		}
		if (turnsPrep > 0) {
			turnsPrep -= 0.5;
		}
		spend(TICK);
		return true;
	}
	
	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}
	
	public int damageRoll(Char attacker, Char defender ){
		AttackLevel lvl = AttackLevel.getLvl(turnsPrep);
		if (lvl.canInstakill(defender)){
			int dmg = lvl.damageRoll(attacker, defender);
			defender.damage( Math.max(defender.HT, dmg), new Char.DamageSrc(Element.PHYSICAL, this).ignoreDefense());
			//even though the defender is dead, other effects should still proc (enchants, etc.)
			return Math.max( defender.HT, dmg);
		} else {
			return lvl.damageRoll(attacker, defender);
		}
	}
	
	@Override
	public int icon() {
		return BuffIndicator.PREPARATION;
	}
	
	@Override
	public void tintIcon(Image icon) {
		switch (AttackLevel.getLvl(turnsPrep)){
			case LVL_1:
				icon.hardlight(1f, 1f, 1f);
				break;
			case LVL_2:
				icon.hardlight(0f, 1f, 0f);
				break;
			case LVL_3:
				icon.hardlight(1f, 1f, 0f);
				break;
			case LVL_4:
				icon.hardlight(1f, 0.6f, 0f);
				break;
			case LVL_5:
				icon.hardlight(1f, 0f, 0f);
				break;
		}
	}
	
	@Override
	public String toString() {
		return Messages.get(this, "name");
	}
	
	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		
		AttackLevel lvl = AttackLevel.getLvl(turnsPrep);
		
		if (lvl.canInstakill(new Rat())){
			desc += "\n\n" + Messages.get(this, "desc_dmg_instakill",
					(int)(lvl.baseDmgBonus*100),
					(int)(lvl.baseDmgBonus*100 + lvl.missingHPBonus*100));
		} else if (lvl.missingHPBonus > 0){
			desc += "\n\n" + Messages.get(this, "desc_dmg_scale",
					(int)(lvl.baseDmgBonus*100),
					(int)(lvl.baseDmgBonus*100 + lvl.missingHPBonus*100));
		} else {
			desc += "\n\n" + Messages.get(this, "desc_dmg", (int)(lvl.baseDmgBonus*100));
		}
		
		if (lvl.damageRolls > 1){
			desc += " " + Messages.get(this, "desc_dmg_likely");
		}
		
		if (lvl.blinkDistance > 0){
			desc += "\n\n" + Messages.get(this, "desc_blink", lvl.blinkDistance);
		}
		
		desc += "\n\n" + Messages.get(this, "desc_invis_time", turnsPrep);
		
		if (lvl.ordinal() != AttackLevel.values().length-1){
			AttackLevel next = AttackLevel.values()[lvl.ordinal()+1];
			desc += "\n" + Messages.get(this, "desc_invis_next", next.turnsReq);
		}
		
		return desc;
	}
	
	private static final String TURNS = "turnsInvis";
	
	@Override
	public void restoreFromBundle( Bundle bundle) {
		super.restoreFromBundle(bundle);
		turnsPrep = bundle.getInt(TURNS);
		if (AttackLevel.getLvl(turnsPrep).blinkDistance > 0){
			ActionIndicator.setAction(this);
		}
	}
	
	@Override
	public void storeInBundle( Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TURNS, turnsPrep);
	}
	
	@Override
	public Image getIcon() {
		Image actionIco = Effects.get(Effects.Type.WOUND);
		tintIcon(actionIco);
		return actionIco;
	}
	
	@Override
	public void doAction() {
		GameScene.selectCell(attack);
	}
	
	private CellSelector.Listener attack = new CellSelector.Listener() {
		
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Char enemy = Actor.findChar( cell );
			if (enemy == null || Dungeon.hero.isCharmedBy(enemy) || enemy instanceof NPC){
				GLog.w(Messages.get(Preparation.class, "no_target"));
			} else {
				
				//just attack them then!
				if (Dungeon.hero.canAttack(enemy)){
					Dungeon.hero.curAction = new HeroAction.Attack( enemy );
					Dungeon.hero.next();
					return;
				}
				
				AttackLevel lvl = AttackLevel.getLvl(turnsPrep);
				
				boolean[] passable = Dungeon.level.passable();
				//need to consider enemy cell as passable in case they are on a trap or chasm
				passable[cell] = true;
				PathFinder.buildDistanceMap(Dungeon.hero.pos, passable, lvl.blinkDistance+1);
				if (PathFinder.distance[cell] == Integer.MAX_VALUE){
					GLog.w(Messages.get(Preparation.class, "out_of_reach"));
					return;
				}
				
				//we can move through enemies when determining blink distance,
				// but not when actually jumping to a location
				for (Char ch : Actor.chars()){
					if (ch != Dungeon.hero)  passable[ch.pos] = false;
				}
				
				PathFinder.Path path = PathFinder.find(Dungeon.hero.pos, cell, passable);
				int attackPos = path == null ? -1 : path.get(path.size()-2);
				
				if (attackPos == -1 ||
						Dungeon.level.distance(attackPos, Dungeon.hero.pos) > lvl.blinkDistance){
					GLog.w(Messages.get(Preparation.class, "out_of_reach"));
					return;
				}
				
				Dungeon.hero.pos = attackPos;
				Dungeon.level.occupyCell(Dungeon.hero);
				//prevents the hero from being interrupted by seeing new enemies
				Dungeon.observe();
				Dungeon.hero.checkVisibleMobs();
				
				Dungeon.hero.sprite.place( Dungeon.hero.pos );
				Dungeon.hero.sprite.turnTo( Dungeon.hero.pos, cell);
				CellEmitter.get( Dungeon.hero.pos ).burst( Speck.factory( Speck.WOOL ), 6 );
				Sample.INSTANCE.play( Assets.SND_PUFF );

				Dungeon.hero.curAction = new HeroAction.Attack( enemy );
				Dungeon.hero.next();
			}
		}
		
		@Override
		public String prompt() {
			return Messages.get(Preparation.class, "prompt", AttackLevel.getLvl(turnsPrep).blinkDistance);
		}
	};
}
